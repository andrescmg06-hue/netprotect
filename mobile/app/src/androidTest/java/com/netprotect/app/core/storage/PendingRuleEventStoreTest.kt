package com.netprotect.app.core.storage

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.rules.BlockReason
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 25: real-Room coverage for the other half of the Sprint 19 offline queue — a block the
 * device enforced but couldn't report stays queued until it can be. `flush()` is exercised
 * against a real `RuleEnforcementClient` pointed at a port nothing listens on, so its network
 * attempt fails the same way a real outage would (a genuine `ConnectException`, not a mocked
 * failure) — this project has no mocking library, and a real client aimed at an unreachable
 * address is a more honest double than one anyway.
 */
@RunWith(AndroidJUnit4::class)
class PendingRuleEventStoreTest {

    private lateinit var database: NetProtectDatabase
    private lateinit var store: PendingRuleEventStore

    // Nothing binds to this port in the test environment; connections to it fail fast (refused)
    // rather than hanging until HttpJsonClient's 5s timeout.
    private val unreachableClient = RuleEnforcementClient("http://127.0.0.1:59999")

    @Before
    fun createInMemoryDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, NetProtectDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = PendingRuleEventStore(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun an_enqueued_block_survives_until_flushed() = runBlocking {
        store.enqueue("device-1", "com.instagram.android", BlockReason.DAILY_LIMIT, Instant.now())

        assertEquals(1, database.pendingRuleEventDao().getAll("device-1").size)
    }

    @Test
    fun a_block_that_cannot_be_reported_stays_queued_after_flush() = runBlocking {
        store.enqueue("device-1", "com.instagram.android", BlockReason.BLOCK, Instant.now())

        store.flush(unreachableClient, "fake-access-token", "device-1")

        // The network attempt genuinely failed (no server on that port) — flush() must leave the
        // row in place for the next attempt, not drop a block that really happened.
        assertEquals(1, database.pendingRuleEventDao().getAll("device-1").size)
    }

    @Test
    fun an_unrecognized_reason_is_dropped_without_ever_touching_the_network() = runBlocking {
        // A row a future app version wrote with a reason this build doesn't have in its enum
        // (BlockReason.entries) — flush() must recognize this is a retry that can never succeed
        // and discard it, rather than trying (and failing) forever.
        val unrecognized = PendingRuleEventEntity(
            deviceId = "device-1",
            packageName = "com.example.app",
            ruleTypeApplied = "SOME_FUTURE_REASON_THIS_BUILD_DOES_NOT_KNOW",
            occurredAtEpochMs = Instant.now().toEpochMilli(),
        )
        database.pendingRuleEventDao().insert(unrecognized)
        store.enqueue("device-1", "com.tiktok.android", BlockReason.BLOCK, Instant.now())

        store.flush(unreachableClient, "fake-access-token", "device-1")

        // The unrecognized row is gone (dropped on sight); the recognized one is still queued
        // because the flush stopped at the first row it actually tried to report and failed on —
        // proving the unrecognized row was never attempted over the network at all, since a
        // network attempt for it would have also stopped the loop right there, before reaching
        // the second row.
        val remaining = database.pendingRuleEventDao().getAll("device-1")
        assertEquals(listOf("com.tiktok.android"), remaining.map { it.packageName })
    }

    @Test
    fun flush_stops_at_the_first_failure_instead_of_retrying_every_row() = runBlocking {
        store.enqueue("device-1", "com.first.app", BlockReason.BLOCK, Instant.now())
        store.enqueue("device-1", "com.second.app", BlockReason.BLOCK, Instant.now())

        store.flush(unreachableClient, "fake-access-token", "device-1")

        // Both rows are for the same outage; flush() is documented to stop at the first failure
        // rather than burn a request per row only to fail all of them the same way.
        val remaining = database.pendingRuleEventDao().getAll("device-1")
        assertEquals(listOf("com.first.app", "com.second.app"), remaining.map { it.packageName })
    }
}
