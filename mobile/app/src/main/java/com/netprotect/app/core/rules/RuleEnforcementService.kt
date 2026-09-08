package com.netprotect.app.core.rules

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.netprotect.app.core.auth.BackgroundTokenRefresher
import com.netprotect.app.core.inventory.AppInventoryCollector
import com.netprotect.app.core.network.RealtimeClient
import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.storage.NetProtectDatabase
import com.netprotect.app.core.storage.PendingRuleEventStore
import com.netprotect.app.core.storage.RulesCacheStore
import com.netprotect.app.feature.supervised.BlockScreenActivity
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that keeps sondeando (polling) which app is in the foreground and blocks
 * it locally when a rule says to — this has to keep running while the supervised device uses
 * OTHER apps, i.e. while this app itself is not on screen, which a plain Compose LaunchedEffect
 * (tied to SupervisedScreen's composition) cannot do.
 *
 * Requires a persistent notification: not a choice, but a requirement of Android itself for any
 * foreground service (since API 26), independent of the Play anti-stalkerware policy — see
 * docs/android/capability-matrix.md (Sprint 8). Declared as foregroundServiceType="specialUse"
 * because no predefined type (camera, location, mediaPlayback...) fits "watch which app is in
 * the foreground"; that type is Android's own designated catch-all for such cases.
 *
 * No restart-on-death, no boot receiver: if the user swipes the app away or the system kills the
 * process, *enforcement itself* stops until the supervised device reopens NetProtect. That is a
 * real, documented limit of this mechanism (see BlockScreenActivity and the capability matrix),
 * unchanged by Sprint 19 — SyncWorker (core/sync/SyncWorker.kt) runs independently of this
 * service's process and keeps heartbeat/usage-sync/pending-event delivery going even then, but it
 * cannot itself watch the foreground app or block anything.
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

        // No official guidance found for a recommended polling interval (see capability
        // matrix); chosen empirically as a balance between block latency and battery/CPU use
        // for a foreground service that runs continuously.
        private const val POLL_INTERVAL_MS = 3_000L
        private const val RULES_REFRESH_INTERVAL_MS = 60_000L
        // Comfortably under access_token_ttl_minutes (15 on the backend): this service can run
        // for hours between app opens, so it has to renew its own token instead of 401ing
        // silently forever after the first 15 minutes of any session (Sprint 19).
        private const val TOKEN_REFRESH_INTERVAL_MS = 10 * 60_000L

        fun start(context: Context, baseUrl: String, accessToken: String, deviceId: String) {
            val intent = Intent(context, RuleEnforcementService::class.java)
                .putExtra(EXTRA_BASE_URL, baseUrl)
                .putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RuleEnforcementService::class.java))
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var pollingJob: Job? = null
    private var realtimeClient: RealtimeClient? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (intent == null || pollingJob != null) return START_NOT_STICKY
        val baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: return START_NOT_STICKY
        val accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN) ?: return START_NOT_STICKY
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return START_NOT_STICKY

        pollingJob = serviceScope.launch { runPollingLoop(baseUrl, accessToken, deviceId) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
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
        var lastTokenRefreshAt = System.currentTimeMillis()
        // Set from the WebSocket listener's onMessage callback, which runs on OkHttp's own
        // thread, not this coroutine — an AtomicBoolean, not a plain var, so that write is
        // guaranteed visible to the polling loop below without adding a lock for something
        // this simple.
        val forceRulesRefresh = AtomicBoolean(false)
        RealtimeClient(baseUrl).also { realtimeClient = it }
            .connect(deviceId, accessToken) { forceRulesRefresh.set(true) }
        // Tracks the last *other* app we evaluated, so returning to an app already handled
        // this "visit" doesn't spam the block screen. Reset to null whenever our own package
        // (including the block screen itself) comes to the foreground, so leaving and coming
        // back to the blocked app is treated as a fresh visit and re-blocked.
        var lastHandledPackage: String? = null

        while (serviceScope.isActive) {
            val changedPackage = detector.pollForegroundChange()
            val now = System.currentTimeMillis()

            if (now - lastTokenRefreshAt >= TOKEN_REFRESH_INTERVAL_MS) {
                BackgroundTokenRefresher.refresh(applicationContext, baseUrl)?.let { accessToken = it }
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
                // Opportunistic retry of anything evaluateAndMaybeBlock couldn't report earlier
                // — connectivity just came back is exactly when a successful fetch above is
                // likely, so piggyback on the same beat rather than adding a third timer.
                runCatching { pendingEventStore.flush(client, accessToken, deviceId) }
            }

            if (changedPackage == packageName) {
                // Our own block screen coming to the front: reset the tracking so returning to
                // a blocked app counts as a fresh visit. Never evaluated — blocking ourselves
                // would loop.
                lastHandledPackage = null
            } else if (changedPackage != null && changedPackage != lastHandledPackage) {
                lastHandledPackage = changedPackage
                evaluateAndMaybeBlock(
                    foregroundPackage = changedPackage,
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
                    exemptFromDefaultPolicy = changedPackage in protectedPackages,
                    client = client,
                    accessToken = accessToken,
                    deviceId = deviceId,
                    pendingEventStore = pendingEventStore,
                )
            }

            delay(POLL_INTERVAL_MS)
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
        client: RuleEnforcementClient,
        accessToken: String,
        deviceId: String,
        pendingEventStore: PendingRuleEventStore,
    ) {
        val todayUsage = AppInventoryCollector.collectTodayUsage(applicationContext)
            .associate { it.packageName to it.foregroundSeconds }
        val weekUsage = AppInventoryCollector.collectWeekUsage(applicationContext)
            .associate { it.packageName to it.foregroundSeconds }
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
        ) ?: return
        val exemptReasons = setOf(BlockReason.DEFAULT_POLICY, BlockReason.SCHOOL_MODE)
        if (exemptFromDefaultPolicy && reason in exemptReasons) return

        startActivity(
            Intent(this, BlockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(BlockScreenActivity.EXTRA_PACKAGE_NAME, foregroundPackage)
                .putExtra(BlockScreenActivity.EXTRA_REASON, reason.wireValue)
        )
        val occurredAt = Instant.now()
        val reported = runCatching {
            client.reportRuleEvent(accessToken, deviceId, foregroundPackage, reason, occurredAt)
        }.isSuccess
        // Sprint 19: a block that already happened is real regardless of whether the tutor can
        // see it right now — queued instead of dropped, see PendingRuleEventEntity's docstring.
        if (!reported) {
            runCatching { pendingEventStore.enqueue(deviceId, foregroundPackage, reason, occurredAt) }
        }
    }

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
            .setContentText("Verificando las reglas de apps de este dispositivo.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
