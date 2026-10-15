package com.netprotect.app.core.rules

import com.netprotect.app.core.status.ServiceStatusRegistry
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.BackgroundTokenRefresher
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.supervision.SupervisionServices
import com.netprotect.app.core.inventory.AppInventoryCollector
import com.netprotect.app.core.permissions.OverlayPermission
import com.netprotect.app.core.network.RealtimeClient
import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.storage.NetProtectDatabase
import com.netprotect.app.core.storage.PendingRuleEventStore
import com.netprotect.app.core.storage.RulesCacheStore
import com.netprotect.app.feature.supervised.BlockScreenActivity
import com.netprotect.app.ui.format.CategoryLabels
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that keeps sondeando (polling) which app is in the foreground and blocks
 * it locally when a rule says to — this has to keep running while the supervised device uses
 * OTHER apps, i.e. while this app itself is not on screen, which a plain Compose LaunchedEffect
 * (tied to SupervisedShell's composition) cannot do.
 *
 * Requires a persistent notification: not a choice, but a requirement of Android itself for any
 * foreground service (since API 26), independent of the Play anti-stalkerware policy — see
 * docs/android/capability-matrix.md (Sprint 8). Declared as foregroundServiceType="specialUse"
 * because no predefined type (camera, location, mediaPlayback...) fits "watch which app is in
 * the foreground"; that type is Android's own designated catch-all for such cases.
 *
 * Sprint 73: closing the app no longer stops this service (SupervisedShell only starts it, and
 * stops it when unlinked, signed out or usage access is withdrawn — SupervisionServices). If the
 * system kills the process the service is START_STICKY and rebuilds its configuration from what is
 * stored; BootReceiver starts it after a reboot or an app update, and SyncWorker starts it again
 * when EnforcementLiveness says it died. Android may still refuse a start from the background
 * (see the capability matrix, Sprint 73), and HyperOS/MIUI kills the app when it is swiped from
 * Recents unless the person allowed «Inicio automático»: then nothing restarts until it is opened.
 * Before Sprint 73 there was no restart at all (Sprints 8–19).
 *
 * Sprint 19 (offline): this service now primes its rule cache from Room (RulesCacheStore) on
 * start instead of only the ALLOW/no-school-mode fail-safe defaults, persists every successful
 * fetch back to Room, and queues (PendingRuleEventStore) any `reportRuleEvent` call that fails
 * instead of dropping it — see those classes' docstrings for the "server always wins, no local
 * merge" conflict strategy and the accepted at-least-once delivery limitation.
 */
class RuleEnforcementService : Service() {

    companion object {
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_DEVICE_ID = "device_id"

        private const val CHANNEL_ID = "rule_enforcement"
        private const val NOTIFICATION_ID = 1001

        /** Ongoing notification text; pinned by ForegroundNotificationTextTest. */
        internal const val NOTIFICATION_TEXT = "Control de apps funcionando en este dispositivo."
        private const val BLOCK_CHANNEL_ID = "block_screen_alert"
        private const val BLOCK_NOTIFICATION_ID = 1003

        // No official guidance found for a recommended polling interval (see capability
        // matrix); chosen empirically as a balance between block latency and battery/CPU use
        // for a foreground service that runs continuously.
        private const val POLL_INTERVAL_MS = 3_000L
        private const val RULES_REFRESH_INTERVAL_MS = 60_000L
        // Comfortably under access_token_ttl_minutes (15 on the backend): this service can run
        // for hours between app opens, so it has to renew its own token instead of 401ing
        // silently forever after the first 15 minutes of any session (Sprint 19).
        // Sprint 41: only a cheap "is it near expiry?" check against the shared TokenSession —
        // the network renewal happens there, once for the whole process, when actually due.
        private const val TOKEN_REFRESH_INTERVAL_MS = 15_000L

        // Sprint 73: after «Ir al inicio», how long the redraw watch gives the launcher's
        // foreground event to show up in the usage events before trusting "still in front" again.
        // Under one poll interval; going back to the blocked app is a new visit anyway.
        private const val GO_HOME_GRACE_MS = 2_000L

        private const val TAG = "RuleEnforcement"

        fun start(context: Context, baseUrl: String, accessToken: String, deviceId: String) {
            // Sprint 20: stamped here, synchronously, and not only from the poll loop below.
            // startForegroundService() is asynchronous, and SupervisedShell's heartbeat loop
            // starts in the same composition — without this, the session's first heartbeat could
            // read a marker the service hadn't stamped yet and report a SERVICE_INACTIVE that
            // never happened.
            // Sprint 73: stamped right after the start call instead of before it, still
            // synchronously: a start refused from the background (ForegroundServiceStartNotAllowed-
            // Exception, thrown by startForegroundService itself) must not mark a dead service live.
            // Idempotent: a second start for the same device keeps the running loop (onStartCommand).
            val intent = Intent(context, RuleEnforcementService::class.java)
                .putExtra(EXTRA_BASE_URL, baseUrl)
                .putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
            context.startForegroundService(intent)
            EnforcementLiveness.markActive(context)
        }

        fun stop(context: Context) {
            // A deliberate stop (signed out, unlinked, usage access withdrawn) is not
            // tampering: clearing the marker makes the next heartbeat report "no information"
            // instead of "the service was disabled". Being killed — swiped away, force-stopped,
            // reclaimed under memory pressure — never runs this, which is exactly the case the
            // SERVICE_INACTIVE signal is meant to catch.
            EnforcementLiveness.clear(context)
            context.stopService(Intent(context, RuleEnforcementService::class.java))
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var pollingJob: Job? = null
    // Sprint 73: the device the running loop enforces for (main thread only, like pollingJob).
    private var runningDeviceId: String? = null
    private var realtimeClient: RealtimeClient? = null
    // Sprint 73: set by the overlay's «Ir al inicio» (main thread), read by the poll loop.
    private val goHomeRequestedAt = AtomicLong(0L)

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sprint 73: a restart by the system (START_STICKY, intent == null) happens with the app
        // in the background, where Android 12+ may refuse to make the service foreground
        // (ForegroundServiceStartNotAllowedException, an IllegalStateException). Give up quietly
        // then: SyncWorker's watch or the next app open starts it again.
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (refused: IllegalStateException) {
            return giveUp(refused)
        } catch (refused: SecurityException) {
            return giveUp(refused)
        }

        // Sprint 73: with no extras (system restart) the configuration is rebuilt from what is
        // stored: the base URL from the build, the linked device and the session from their
        // stores (no link or no session means sign-out/unlink, nothing to restore), and the
        // token from the shared session inside the loop. Usage access is checked here because
        // nothing else would stop the service if it was withdrawn while the app was closed.
        val deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)
            ?: SupervisionServices.restorableDeviceId(this)
                ?.takeIf { UsageAccessPermission.isGranted(this) }
        if (deviceId == null) {
            if (pollingJob == null) stopSelf()
            return if (pollingJob == null) START_NOT_STICKY else START_STICKY
        }
        // Idempotent: the screen calls start() every time it is composed; the same device keeps
        // the loop that is already running. A different device (the link's identity changed)
        // replaces it — the old loop's socket closes in its own finally.
        if (pollingJob != null && deviceId == runningDeviceId) return START_STICKY
        pollingJob?.cancel()
        BlockOverlayController.hide(applicationContext)

        val baseUrl = intent?.getStringExtra(EXTRA_BASE_URL) ?: BuildConfig.API_BASE_URL
        val accessToken = intent?.getStringExtra(EXTRA_ACCESS_TOKEN).orEmpty()
        runningDeviceId = deviceId
        pollingJob = serviceScope.launch { runPollingLoop(baseUrl, accessToken, deviceId) }
        // Sprint 50: only reports its own life to "Estado de NetProtect"; changes nothing it does.
        ServiceStatusRegistry.enforcementRunning(true)
        // Sprint 73: if the system kills the process it brings the service back (intent == null).
        return START_STICKY
    }

    /** Could not become foreground: stop without crashing. Not a deliberate stop, so
     * EnforcementLiveness is left as it is — a dead service must still read as dead. */
    private fun giveUp(refused: Exception): Int {
        Log.w(TAG, "Could not start in the foreground: ${refused.javaClass.simpleName}")
        if (pollingJob == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ServiceStatusRegistry.enforcementRunning(false)
        BlockOverlayController.hide(applicationContext)
        realtimeClient?.disconnect()
        serviceJob.cancel()
        super.onDestroy()
    }

    private suspend fun runPollingLoop(baseUrl: String, initialAccessToken: String, deviceId: String) {
        val detector = ForegroundAppDetector(applicationContext)
        val client = RuleEnforcementClient(baseUrl)
        val database = NetProtectDatabase.getInstance(applicationContext)
        val rulesCacheStore = RulesCacheStore(database)
        val pendingEventStore = PendingRuleEventStore(database)
        // Sprint 41: the token handed over by the screen may already be stale (or empty); the
        // realtime connect below uses it once, so start from the shared session's valid one.
        var accessToken = BackgroundTokenRefresher.refresh(applicationContext) ?: initialAccessToken
        // Starts at ALLOW so a device whose first fetch fails keeps working normally instead of
        // blocking everything on a network error.
        var defaultPolicy = DefaultAppPolicy.ALLOW
        // Starts disabled for the same fail-safe reason: a device whose first fetch fails
        // should not suddenly start blocking everything.
        var schoolMode = SchoolMode(enabled = false, startMinute = null, endMinute = null, daysMask = null)
        var cachedRules: List<AppRule> = emptyList()
        var cachedCategoryAssignments: List<CategoryAssignment> = emptyList()
        var cachedCategoryRules: List<CategoryRule> = emptyList()

        // Sprint 19: prime the enforcement loop from the last known-good rule set (Room) before
        // the first network fetch, instead of the fail-safe ALLOW/no-school-mode defaults above.
        // Those defaults still apply on a genuinely first-ever launch (nothing cached yet) — see
        // RulesCacheStore.loadCached's docstring for why that returns null rather than an empty
        // ActiveRules in that case.
        runCatching { rulesCacheStore.loadCached(deviceId) }.getOrNull()?.let { cached ->
            cachedRules = cached.rules
            cachedCategoryAssignments = cached.categoryAssignments
            cachedCategoryRules = cached.categoryRules
            defaultPolicy = cached.defaultPolicy
            schoolMode = cached.schoolMode
        }

        var protectedPackages = ProtectedPackages.resolve(applicationContext)
        var lastRulesFetchAt = 0L
        var lastTokenRefreshAt = System.currentTimeMillis()
        // Set from the WebSocket listener's onMessage callback, which runs on OkHttp's own
        // thread, not this coroutine — an AtomicBoolean, not a plain var, so that write is
        // guaranteed visible to the polling loop below without adding a lock for something
        // this simple.
        val forceRulesRefresh = AtomicBoolean(false)
        val realtime = RealtimeClient(baseUrl).also { realtimeClient = it }
        realtime.connect(deviceId, accessToken) { event, _ ->
            if (event == "rules_changed") forceRulesRefresh.set(true)
        }
        // Sprint 73: which app the current "visit" is, decided from every foreground transition
        // of each poll window (VisitTracker), not only from the last one — leaving a blocked app
        // for the home screen and coming back inside the same window used to look like the same,
        // already-blocked visit, so it was never blocked again. An own-package visit (the block
        // screen included) resets the per-visit state and is never evaluated, so returning to a
        // blocked app afterwards is a fresh visit and blocked again.
        val visits = VisitTracker()
        // B-15: per-visit state for re-evaluating a daily/weekly limit while the user stays in
        // the same app. blockedThisVisit keeps the rule event to one per crossing (the overlay
        // covers the app until a new visit); nextLimitCheckAt comes from the last evaluation.
        var blockedThisVisit = false
        var nextLimitCheckAt: Long? = null
        // Sprint 73: what the current visit's block screen shows, so the watch below can draw it
        // again without evaluating or reporting anything.
        var visitBlock: BlockDisplay? = null

        try {
            while (serviceScope.isActive) {
                // Sprint 73: one failing cycle (a system call throwing, an unexpected row...) used to
                // end the coroutine and with it all enforcement until the service restarted. Now it
                // is logged by exception type only (no package, no token) and the next cycle runs.
                try {
                    val visitStarted = visits.consume(detector.pollTransitions())
                    val now = System.currentTimeMillis()

                    // Sprint 20: the only thing that proves this service is still alive to anything
                    // outside its process (SyncWorker, SupervisedShell's heartbeat loop). Stamped
                    // every cycle so a stale marker means the service really stopped — see
                    // EnforcementLiveness.
                    EnforcementLiveness.markActive(applicationContext)

                    if (now - lastTokenRefreshAt >= TOKEN_REFRESH_INTERVAL_MS) {
                        BackgroundTokenRefresher.refresh(applicationContext)?.let { accessToken = it }
                        lastTokenRefreshAt = now
                    }

                    if (now - lastRulesFetchAt >= RULES_REFRESH_INTERVAL_MS || forceRulesRefresh.getAndSet(false)) {
                        runCatching { client.getActiveRules(accessToken, deviceId) }
                            .onSuccess { active ->
                                cachedRules = active.rules
                                cachedCategoryAssignments = active.categoryAssignments
                                cachedCategoryRules = active.categoryRules
                                defaultPolicy = active.defaultPolicy
                                schoolMode = active.schoolMode
                                runCatching { rulesCacheStore.replaceAll(deviceId, active) }
                            }
                        // Refreshed on the same beat as the rules: the user can change their launcher
                        // or default phone app at any time, and the protected set must follow.
                        protectedPackages = ProtectedPackages.resolve(applicationContext)
                        lastRulesFetchAt = now
                        // Opportunistic retry of anything evaluateAndMaybeBlock couldn't report
                        // earlier — connectivity just came back is exactly when a successful fetch
                        // above is likely, so piggyback on the same beat rather than adding a timer.
                        runCatching { pendingEventStore.flush(client, accessToken, deviceId) }
                    }

                    if (visitStarted) {
                        // The foreground app genuinely changed (at least once in this window):
                        // whatever overlay was covering the previous visit no longer applies.
                        // evaluateAndMaybeBlock below draws a fresh one if the new visit is blocked.
                        BlockOverlayController.hide(applicationContext)
                        blockedThisVisit = false
                        nextLimitCheckAt = null
                        visitBlock = null
                    }

                    val current = visits.currentPackage
                    if (current == null || current == packageName) {
                        // No visit seen yet, or our own app in front: never evaluated — blocking
                        // ourselves would loop.
                    } else if (visitStarted) {
                        val outcome = evaluateAndMaybeBlock(
                            foregroundPackage = current,
                            rules = cachedRules,
                            categoryAssignments = cachedCategoryAssignments,
                            categoryRules = cachedCategoryRules,
                            defaultPolicy = defaultPolicy,
                            schoolMode = schoolMode,
                            // Protected apps are exempt from the *default policy* only, not from a
                            // rule the tutor wrote on purpose. The protected set includes the user's
                            // chosen dialer and launcher, which the supervised user can change in
                            // Settings — exempting those from every rule would turn "set this app as
                            // my default phone app" into a three-tap way to bypass any block.
                            exemptFromDefaultPolicy = current in protectedPackages,
                            detector = detector,
                            client = client,
                            accessToken = accessToken,
                            deviceId = deviceId,
                            pendingEventStore = pendingEventStore,
                        )
                        blockedThisVisit = outcome.blocked
                        nextLimitCheckAt = outcome.nextLimitCheckAt
                        visitBlock = outcome.display
                    } else if (blockedThisVisit) {
                        // Sprint 73: the blocked app is still the visit in front, but its block
                        // screen is gone (taken down with no new visit seen, or removed by the
                        // system). Draw it again — display only: this visit's rule event was already
                        // reported. Without the overlay permission the notification fallback is not
                        // repeated (still one per visit).
                        val block = visitBlock
                        if (block != null && visits.inForeground &&
                            now - goHomeRequestedAt.get() >= GO_HOME_GRACE_MS &&
                            !BlockOverlayController.isShowing() &&
                            OverlayPermission.isGranted(applicationContext)
                        ) {
                            showBlockOverlay(block)
                        }
                    } else if (
                        // B-15: same app still in front (no event, or a repeated one from moving
                        // between its own activities). Re-evaluated only for a daily/weekly limit
                        // whose time may have run out — not a new visit: no overlay reset.
                        LimitRecheck.shouldRecheck(
                            lastHandledPackage = current,
                            changedPackage = null,
                            blockedThisVisit = blockedThisVisit,
                            governedByLimit = LimitRecheck.isGovernedByLimit(
                                cachedRules, cachedCategoryAssignments, cachedCategoryRules, current,
                            ),
                            nextCheckAtMillis = nextLimitCheckAt,
                            nowMillis = now,
                        )
                    ) {
                        val outcome = evaluateAndMaybeBlock(
                            foregroundPackage = current,
                            rules = cachedRules,
                            categoryAssignments = cachedCategoryAssignments,
                            categoryRules = cachedCategoryRules,
                            defaultPolicy = defaultPolicy,
                            schoolMode = schoolMode,
                            exemptFromDefaultPolicy = current in protectedPackages,
                            detector = detector,
                            client = client,
                            accessToken = accessToken,
                            deviceId = deviceId,
                            pendingEventStore = pendingEventStore,
                        )
                        blockedThisVisit = outcome.blocked
                        nextLimitCheckAt = outcome.nextLimitCheckAt
                        visitBlock = outcome.display
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.w(TAG, "Enforcement cycle failed: ${failure.javaClass.simpleName}")
                }

                delay(POLL_INTERVAL_MS)
            }
        } finally {
            // This loop's own socket, also when only the loop ends and the service lives on.
            realtime.disconnect()
        }
    }

    private suspend fun evaluateAndMaybeBlock(
        foregroundPackage: String,
        rules: List<AppRule>,
        categoryAssignments: List<CategoryAssignment>,
        categoryRules: List<CategoryRule>,
        defaultPolicy: DefaultAppPolicy,
        schoolMode: SchoolMode,
        exemptFromDefaultPolicy: Boolean,
        detector: ForegroundAppDetector,
        client: RuleEnforcementClient,
        accessToken: String,
        deviceId: String,
        pendingEventStore: PendingRuleEventStore,
    ): EvaluationOutcome {
        val statsToday = AppInventoryCollector.collectTodayUsage(applicationContext)
            .associate { it.packageName to it.foregroundSeconds }
        val statsWeek = AppInventoryCollector.collectWeekUsage(applicationContext)
            .associate { it.packageName to it.foregroundSeconds }
        // B-15: queryUsageStats may not include the session still open for the app in front, so
        // the limit would only be reached after the user leaves it. Only the missing difference
        // is added (see LimitRecheck.missingSeconds), and only for this evaluation — nothing here
        // changes what AppInventoryCollector syncs to the tutor.
        val missing = if (LimitRecheck.isGovernedByLimit(rules, categoryAssignments, categoryRules, foregroundPackage)) {
            LimitRecheck.missingSeconds(
                statsTodaySeconds = statsToday[foregroundPackage] ?: 0,
                eventsTodaySeconds = runCatching { detector.todayForegroundSeconds(foregroundPackage) }.getOrNull(),
            )
        } else {
            0
        }
        val todayUsage = statsToday.withExtra(foregroundPackage, missing)
        val weekUsage = statsWeek.withExtra(foregroundPackage, missing)
        val nextLimitCheckAt = LimitRecheck.nextCheckAt(
            System.currentTimeMillis(),
            LimitRecheck.limitRemainingSeconds(
                rules, categoryAssignments, categoryRules, foregroundPackage, todayUsage, weekUsage,
            ),
        )
        val notBlocked = EvaluationOutcome(blocked = false, nextLimitCheckAt = nextLimitCheckAt)
        val reason = RuleEvaluator.evaluate(
            rules,
            categoryAssignments,
            categoryRules,
            foregroundPackage,
            todayUsage,
            weekUsage,
            LocalDateTime.now(),
            defaultPolicy,
            schoolMode,
        )
        if (reason == null) return notBlocked
        val exemptReasons = setOf(BlockReason.DEFAULT_POLICY, BlockReason.SCHOOL_MODE)
        if (exemptFromDefaultPolicy && reason in exemptReasons) return notBlocked

        // Sprint 49: only the display changes — which app is blocked, when and why is decided above.
        // The category shown is the real assigned one (never invented), or none.
        val categoryLabel = categoryAssignments.find { it.packageName == foregroundPackage }
            ?.let { CategoryLabels.categoryLabel(it.category.wireValue) }
        val display = BlockDisplay(foregroundPackage, categoryLabel, reason)
        if (OverlayPermission.isGranted(applicationContext)) {
            showBlockOverlay(display)
        } else {
            // No overlay permission yet: falls back to the notification path, which still works
            // while the device is locked (see launchBlockScreen's docstring) — better than
            // nothing, but the tutor should still be told to grant the overlay permission for
            // this to work while the device is unlocked and in active use.
            launchBlockScreen(foregroundPackage, categoryLabel, reason)
        }
        val occurredAt = Instant.now()
        val reported = runCatching {
            client.reportRuleEvent(accessToken, deviceId, foregroundPackage, reason, occurredAt)
        }.isSuccess
        // Sprint 19: a block that already happened is real regardless of whether the tutor can
        // see it right now — queued instead of dropped, see PendingRuleEventEntity's docstring.
        if (!reported) {
            runCatching { pendingEventStore.enqueue(deviceId, foregroundPackage, reason, occurredAt) }
        }
        return EvaluationOutcome(blocked = true, nextLimitCheckAt = null, display = display)
    }

    private class EvaluationOutcome(
        val blocked: Boolean,
        val nextLimitCheckAt: Long?,
        val display: BlockDisplay? = null,
    )

    /** What one visit's block screen shows; kept so the poll loop's watch can draw it again. */
    private class BlockDisplay(val packageName: String, val categoryLabel: String?, val reason: BlockReason)

    /** Draws the block screen overlay; the caller has checked OverlayPermission. «Ir al inicio»
     * takes it down and opens the launcher, and notes the time so the poll loop's watch does not
     * draw it again over the launcher before the launcher's own foreground event is logged. */
    private fun showBlockOverlay(block: BlockDisplay) {
        val appLabel = resolveAppLabel(block.packageName)
        BlockOverlayController.show(applicationContext, block.packageName, appLabel, block.categoryLabel, block.reason) {
            goHomeRequestedAt.set(System.currentTimeMillis())
            BlockOverlayController.hide(applicationContext)
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun Map<String, Int>.withExtra(packageName: String, extraSeconds: Int): Map<String, Int> =
        if (extraSeconds <= 0) this else this + (packageName to (this[packageName] ?: 0) + extraSeconds)

    /** Verified live (17/09/2026, API 36 emulator): a foreground service with no visible
     * Activity can no longer open one with a plain startActivity() — ActivityTaskManager logs
     * "Background activity launch blocked!" and silently drops the intent, no exception thrown.
     * A full-screen-intent notification is Android's own sanctioned exemption from that
     * restriction (the same mechanism incoming calls and alarms use). Needs its own
     * IMPORTANCE_HIGH channel: the ongoing "service is running" notification is deliberately
     * IMPORTANCE_LOW/silent, and reusing it would make every block silent too.
     */
    private fun launchBlockScreen(foregroundPackage: String, categoryLabel: String?, reason: BlockReason) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(BLOCK_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    BLOCK_CHANNEL_ID,
                    "Bloqueo de aplicación",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
        val intent = Intent(this, BlockScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(BlockScreenActivity.EXTRA_PACKAGE_NAME, foregroundPackage)
            .putExtra(BlockScreenActivity.EXTRA_REASON, reason.wireValue)
            .putExtra(BlockScreenActivity.EXTRA_CATEGORY_LABEL, categoryLabel)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            BLOCK_NOTIFICATION_ID,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, BLOCK_CHANNEL_ID)
            .setContentTitle("Aplicación bloqueada")
            .setContentText("NetProtect bloqueó una app según las reglas configuradas.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(BLOCK_NOTIFICATION_ID, notification)
    }

    private fun resolveAppLabel(packageName: String): String =
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Reglas de aplicaciones",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetProtect está activo")
            .setContentText(NOTIFICATION_TEXT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
