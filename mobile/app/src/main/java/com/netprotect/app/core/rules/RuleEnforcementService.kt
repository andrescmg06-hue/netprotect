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
import com.netprotect.app.core.network.ReconnectingRealtimeChannel
import com.netprotect.app.core.network.isDeviceGone
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

        // Sprint 73 (T5): how far back the loop looks, when it (re)starts, to learn which app is
        // already in front (VisitTracker.seed). Bounded so one read stays cheap; an app opened
        // before that and still in front is only seen at its next transition.
        private const val SEED_LOOKBACK_MS = 30 * 60_000L

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
    // Sprint 73 (T5): the reconnecting rules_changed channel of the running loop.
    @Volatile private var realtimeChannel: ReconnectingRealtimeChannel? = null
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
        // Sprint 73 (T5): "running" means a loop that is still active — a loop that ended (the
        // link ended, or it died) must not make a later start a no-op.
        val running = pollingJob?.isActive == true
        if (deviceId == null) {
            if (!running) stopSelf()
            return if (running) START_STICKY else START_NOT_STICKY
        }
        // Idempotent: the screen calls start() every time it is composed; the same device keeps
        // the loop that is already running. A different device (the link's identity changed)
        // replaces it — the old loop's socket closes in its own finally.
        if (running && deviceId == runningDeviceId) return START_STICKY
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
        if (pollingJob?.isActive != true) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ServiceStatusRegistry.enforcementRunning(false)
        BlockOverlayController.hide(applicationContext)
        realtimeChannel?.stop()
        serviceJob.cancel()
        super.onDestroy()
    }

    /** Sprint 73 (T5): the loop's preparation (database, protected packages, realtime channel,
     * look-back read) used to run outside any error handling, so one exception there ended the
     * coroutine — and with it all blocking — while the service stayed "running" (and a restart was
     * a no-op). Now any failure outside a cycle is logged by type and the whole loop is prepared
     * again after one poll interval. Returns only when the link ended (the service is then stopped).
     */
    private suspend fun runPollingLoop(baseUrl: String, initialAccessToken: String, deviceId: String) {
        while (serviceScope.isActive) {
            try {
                enforce(baseUrl, initialAccessToken, deviceId)
                return
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Enforcement loop failed, preparing it again: ${failure.javaClass.simpleName}")
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun enforce(baseUrl: String, initialAccessToken: String, deviceId: String) {
        val detector = ForegroundAppDetector(applicationContext)
        val client = RuleEnforcementClient(baseUrl)
        val database = NetProtectDatabase.getInstance(applicationContext)
        val rulesCacheStore = RulesCacheStore(database)
        val pendingEventStore = PendingRuleEventStore(database)
        // Sprint 41: the token handed over by the screen may already be stale (or empty).
        // Sprint 73 (T5): renewed in the first cycle, AFTER evaluating the app in front with the
        // cached rules — the network no longer delays the first block (see the cycle below).
        var accessToken = initialAccessToken
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
        var lastTokenRefreshAt = 0L
        // Set from the WebSocket listener's onMessage callback, which runs on OkHttp's own
        // thread, not this coroutine — an AtomicBoolean, not a plain var, so that write is
        // guaranteed visible to the polling loop below without adding a lock for something
        // this simple.
        val forceRulesRefresh = AtomicBoolean(false)
        // Sprint 73 (T5): a channel that reconnects by itself (growing waits, a fresh valid token
        // from the shared session as the first frame of each attempt) — the same one the shell
        // uses. The plain socket used before connected once: a network drop, a backend restart or
        // a start with no network (an empty token after boot) left "rules_changed" deaf until the
        // service restarted, and new rules then waited for the 60 s refresh.
        val realtime = ReconnectingRealtimeChannel(
            baseUrl = baseUrl,
            deviceId = deviceId,
            tokenProvider = { BackgroundTokenRefresher.refresh(applicationContext) },
        ) { event, _ ->
            if (event == "rules_changed") forceRulesRefresh.set(true)
        }
        realtimeChannel = realtime
        realtime.start(serviceScope)
        try {
            // Sprint 73: which app the current "visit" is, decided from every foreground transition
            // of each poll window (VisitTracker), not only from the last one — leaving a blocked
            // app for the home screen and coming back inside the same window used to look like the
            // same, already-blocked visit, so it was never blocked again. An own-package visit (the
            // block screen included) resets the per-visit state and is never evaluated, so
            // returning to a blocked app afterwards is a fresh visit and blocked again.
            val visits = VisitTracker()
            // B-15: per-visit state for re-evaluating a daily/weekly limit while the user stays in
            // the same app. blockedThisVisit keeps the rule event to one per crossing (the overlay
            // covers the app until a new visit); nextLimitCheckAt comes from the last evaluation.
            var blockedThisVisit = false
            var nextLimitCheckAt: Long? = null
            // Sprint 73: what the current visit's block screen shows, so the watch below can draw
            // it again without evaluating or reporting anything.
            var visitBlock: BlockDisplay? = null
            // Sprint 73 (T5): the visit in front still has to be evaluated — it began, or the loop
            // (re)started with it already in front, or its evaluation failed and is retried next
            // cycle (before, a failed evaluation left the visit unevaluated until the next rules
            // beat, up to 60 s, or for good at the start of a visit).
            // At (re)start nothing used to be known about the app in front, so it was never
            // evaluated until the child left it and came back: seeded from a look-back read now.
            var evaluationPending = visits.seed(detector.readBack(SEED_LOOKBACK_MS))
            // Sprint 73 (T5): set when the backend answers the uniform device_not_found.
            var linkEnded = false
            // Sprint 73 (T5): false when an evaluation failed this cycle; then the liveness marker
            // is not stamped (below).
            var cycleHealthy = true

            fun apply(outcome: EvaluationOutcome) {
                blockedThisVisit = outcome.blocked
                nextLimitCheckAt = outcome.nextLimitCheckAt
                visitBlock = outcome.display
                if (outcome.deviceGone) linkEnded = true
            }

            /** Null when the evaluation threw (logged by type only). */
            suspend fun evaluate(foregroundPackage: String, decideOnly: Boolean = false): EvaluationOutcome? =
                try {
                    evaluateAndMaybeBlock(
                        foregroundPackage = foregroundPackage,
                        rules = cachedRules,
                        categoryAssignments = cachedCategoryAssignments,
                        categoryRules = cachedCategoryRules,
                        defaultPolicy = defaultPolicy,
                        schoolMode = schoolMode,
                        // Protected apps are exempt from the *default policy* only, not from a rule
                        // the tutor wrote on purpose. The protected set includes the user's chosen
                        // dialer and launcher, which the supervised user can change in Settings —
                        // exempting those from every rule would turn "set this app as my default
                        // phone app" into a three-tap way to bypass any block.
                        exemptFromDefaultPolicy = foregroundPackage in protectedPackages,
                        detector = detector,
                        client = client,
                        accessToken = accessToken,
                        deviceId = deviceId,
                        pendingEventStore = pendingEventStore,
                        decideOnly = decideOnly,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.w(TAG, "Evaluation failed: ${failure.javaClass.simpleName}")
                    cycleHealthy = false
                    null
                }

            while (serviceScope.isActive) {
                // Sprint 73: one failing cycle (a system call throwing, an unexpected row...) used
                // to end the coroutine and with it all enforcement until the service restarted.
                // Now it is logged by exception type only (no package, no token) and the next
                // cycle runs.
                try {
                    cycleHealthy = true
                    val visitStarted = visits.consume(detector.pollTransitions())
                    if (visitStarted) {
                        // The foreground app genuinely changed (at least once in this window):
                        // whatever overlay was covering the previous visit no longer applies.
                        // The evaluation below draws a fresh one if the new visit is blocked.
                        BlockOverlayController.hide(applicationContext)
                        blockedThisVisit = false
                        nextLimitCheckAt = null
                        visitBlock = null
                        evaluationPending = true
                    }
                    val current = visits.currentPackage
                    // No visit seen yet, or our own app in front: never evaluated — blocking
                    // ourselves would loop.
                    val currentIsOtherApp = current != null && current != packageName
                    if (!currentIsOtherApp) evaluationPending = false

                    // Sprint 73 (T5): the new visit is evaluated FIRST, with the rules already in
                    // hand (Room cache or the last fetch). The token renewal, the rules fetch and
                    // the queue flush below can take up to 5 s each on a bad network, and used to
                    // run before this, delaying the block screen by as much.
                    var evaluatedThisCycle = false
                    if (evaluationPending && current != null && currentIsOtherApp) {
                        evaluatedThisCycle = true
                        evaluate(current)?.let { outcome ->
                            apply(outcome)
                            evaluationPending = false
                        }
                    }

                    var now = System.currentTimeMillis()
                    if (now - lastTokenRefreshAt >= TOKEN_REFRESH_INTERVAL_MS) {
                        BackgroundTokenRefresher.refresh(applicationContext)?.let { accessToken = it }
                        lastTokenRefreshAt = now
                    }

                    // Sprint 73 (Fallo 3): the rules beat, every RULES_REFRESH_INTERVAL_MS or at
                    // once on a realtime "rules_changed". The visit in front is re-evaluated on it.
                    val rulesBeat = now - lastRulesFetchAt >= RULES_REFRESH_INTERVAL_MS || forceRulesRefresh.getAndSet(false)
                    var fetchedFresh = false
                    if (rulesBeat) {
                        val fetch = runCatching { client.getActiveRules(accessToken, deviceId) }
                        fetch.getOrNull()?.let { active ->
                            cachedRules = active.rules
                            cachedCategoryAssignments = active.categoryAssignments
                            cachedCategoryRules = active.categoryRules
                            defaultPolicy = active.defaultPolicy
                            schoolMode = active.schoolMode
                            runCatching { rulesCacheStore.replaceAll(deviceId, active) }
                            fetchedFresh = true
                        }
                        // Sprint 73 (T5): no active link any more (the backend's uniform 404).
                        if (fetch.exceptionOrNull()?.isDeviceGone() == true) linkEnded = true
                        // Refreshed on the same beat as the rules: the user can change their
                        // launcher or default phone app at any time, and the protected set must
                        // follow.
                        protectedPackages = ProtectedPackages.resolve(applicationContext)
                        lastRulesFetchAt = now
                        // Opportunistic retry of anything evaluateAndMaybeBlock couldn't report
                        // earlier — connectivity just came back is exactly when a successful fetch
                        // above is likely, so piggyback on the same beat rather than adding a timer.
                        if (!linkEnded) {
                            val flush = runCatching { pendingEventStore.flush(client, accessToken, deviceId) }
                            if (flush.exceptionOrNull()?.isDeviceGone() == true) linkEnded = true
                        }
                    }

                    if (!linkEnded && current != null && currentIsOtherApp) {
                        now = System.currentTimeMillis()
                        val recheck = VisitRecheck.onRulesBeat(
                            rulesBeat = rulesBeat,
                            currentIsOtherApp = true,
                            // Just evaluated with these very rules: nothing new to check. Rules
                            // fetched fresh in this same cycle are checked (never a second report:
                            // EVALUATE only runs for a visit not blocked yet, CONFIRM_BLOCK never
                            // reports).
                            visitStarted = evaluatedThisCycle && !fetchedFresh,
                            inForeground = visits.inForeground,
                            blockedThisVisit = blockedThisVisit,
                        )
                        if (recheck == VisitRecheck.Action.EVALUATE) {
                            // Sprint 73 (Fallo 3): new or changed rules (or a schedule that has
                            // just begun) for the app already in front: blocked now if they say so,
                            // with its rule event like any visit — at most one per block, the visit
                            // is not blocked yet. A failure is retried next cycle.
                            val outcome = evaluate(current)
                            if (outcome != null) {
                                apply(outcome)
                                evaluationPending = false
                            } else {
                                evaluationPending = true
                            }
                        } else if (recheck == VisitRecheck.Action.CONFIRM_BLOCK) {
                            // Sprint 73 (Fallo 3): the visit is blocked; if the latest rules no
                            // longer block it (the rule was removed, the schedule ended), take the
                            // block screen down. Nothing is reported either way.
                            val outcome = evaluate(current, decideOnly = true)
                            if (outcome != null && !outcome.blocked) {
                                BlockOverlayController.hide(applicationContext)
                                blockedThisVisit = false
                                nextLimitCheckAt = outcome.nextLimitCheckAt
                                visitBlock = null
                            }
                        } else if (evaluatedThisCycle) {
                            // Evaluated (or tried) above this cycle: nothing more until the next.
                        } else if (blockedThisVisit) {
                            // Sprint 73: the blocked app is still the visit in front, but its block
                            // screen is gone (taken down with no new visit seen, or removed by the
                            // system). Draw it again — display only: this visit's rule event was
                            // already reported. Without the overlay permission the notification
                            // fallback is not repeated (still one per visit).
                            val block = visitBlock
                            if (block != null && visits.inForeground &&
                                now - goHomeRequestedAt.get() >= GO_HOME_GRACE_MS &&
                                !BlockOverlayController.isShowing() &&
                                OverlayPermission.isGranted(applicationContext)
                            ) {
                                showBlockOverlay(block)
                            }
                        } else if (
                            // B-15: same app still in front (no event, or a repeated one from
                            // moving between its own activities). Re-evaluated only for a
                            // daily/weekly limit whose time may have run out — not a new visit: no
                            // overlay reset.
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
                            evaluate(current)?.let { apply(it) }
                        }
                    }

                    if (linkEnded) {
                        // Sprint 73 (T5): the tutor unlinked this device (maybe with the app
                        // closed). Forget the link, delete its data and stop everything — this
                        // service included — instead of enforcing old rules for nobody.
                        SupervisionServices.endLinkFromBackground(applicationContext, deviceId)
                        return
                    }

                    // Sprint 20: the only thing that proves this service is still alive to anything
                    // outside its process (SyncWorker, SupervisedShell's heartbeat loop).
                    // Sprint 73 (T5): stamped only for a cycle that completed and could evaluate
                    // (it used to be stamped at the top of every cycle, even one that then failed
                    // every time), so a loop that cannot block anything reads as dead to the watch
                    // and to the tutor's SERVICE_INACTIVE signal.
                    if (cycleHealthy) EnforcementLiveness.markActive(applicationContext)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.w(TAG, "Enforcement cycle failed: ${failure.javaClass.simpleName}")
                }

                delay(POLL_INTERVAL_MS)
            }
        } finally {
            // This loop's own channel, also when only the loop ends and the service lives on.
            realtime.stop()
            if (realtimeChannel === realtime) realtimeChannel = null
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
        // Sprint 73 (Fallo 3): only decide — no block screen, no rule event. Used to confirm that
        // a visit already blocked is still blocked under the latest rules.
        decideOnly: Boolean = false,
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
        if (decideOnly) return EvaluationOutcome(blocked = true, nextLimitCheckAt = null, display = display)
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
        val reportFailure = runCatching {
            client.reportRuleEvent(accessToken, deviceId, foregroundPackage, reason, occurredAt)
        }.exceptionOrNull()
        // Sprint 73 (T5): the uniform 404 means the link ended: nothing to queue for nobody.
        val deviceGone = reportFailure?.isDeviceGone() == true
        // Sprint 19: a block that already happened is real regardless of whether the tutor can
        // see it right now — queued instead of dropped, see PendingRuleEventEntity's docstring.
        if (reportFailure != null && !deviceGone) {
            runCatching { pendingEventStore.enqueue(deviceId, foregroundPackage, reason, occurredAt) }
        }
        return EvaluationOutcome(blocked = true, nextLimitCheckAt = null, display = display, deviceGone = deviceGone)
    }

    private class EvaluationOutcome(
        val blocked: Boolean,
        val nextLimitCheckAt: Long?,
        val display: BlockDisplay? = null,
        // Sprint 73 (T5): the rule event was answered with device_not_found (the link ended).
        val deviceGone: Boolean = false,
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
