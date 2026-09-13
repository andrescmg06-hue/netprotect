package com.netprotect.app.core.storage

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.netprotect.app.core.network.ActiveRules
import com.netprotect.app.core.rules.AppRule
import com.netprotect.app.core.rules.Category
import com.netprotect.app.core.rules.CategoryAssignment
import com.netprotect.app.core.rules.CategoryRule
import com.netprotect.app.core.rules.DefaultAppPolicy
import com.netprotect.app.core.rules.RuleType
import com.netprotect.app.core.rules.SchoolMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 25: the Sprint 19 offline cache had zero coverage of any kind until now — Room needs a
 * real SQLite from the Android runtime (an in-memory one here, via room-testing, but real SQLite
 * all the same), which a JVM unit test cannot provide and this project has no Robolectric to fake
 * either. This is therefore an instrumented test, run on a real device/emulator, not a unit test.
 *
 * Covers exactly the guarantee `docs/sprint-19.md` documents as this project's whole conflict
 * strategy: "reemplazo total en cada fetch exitoso, sin fusión" — a fresh device sees no cache
 * (null, not empty), and a second fetch replaces the first wholesale rather than accumulating.
 */
@RunWith(AndroidJUnit4::class)
class RulesCacheStoreTest {

    private lateinit var database: NetProtectDatabase
    private lateinit var store: RulesCacheStore

    @Before
    fun createInMemoryDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, NetProtectDatabase::class.java)
            // Matches production's single-process usage (NetProtectDatabase.getInstance) closely
            // enough for this test's purposes; the in-memory store itself is what makes this
            // hermetic, not this flag.
            .allowMainThreadQueries()
            .build()
        store = RulesCacheStore(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun sampleRules(packageName: String = "com.instagram.android") = ActiveRules(
        rules = listOf(
            AppRule(
                packageName = packageName,
                ruleType = RuleType.BLOCK,
                dailyLimitMinutes = null,
                weeklyLimitMinutes = null,
                scheduleStartMinute = null,
                scheduleEndMinute = null,
                scheduleDaysMask = null,
            )
        ),
        categoryAssignments = listOf(
            CategoryAssignment(packageName = packageName, category = Category.SOCIAL_MEDIA)
        ),
        categoryRules = listOf(
            CategoryRule(
                category = Category.GAMES,
                ruleType = RuleType.DAILY_LIMIT,
                dailyLimitMinutes = 60,
                weeklyLimitMinutes = null,
                scheduleStartMinute = null,
                scheduleEndMinute = null,
                scheduleDaysMask = null,
            )
        ),
        defaultPolicy = DefaultAppPolicy.ALLOW,
        schoolMode = SchoolMode(enabled = true, startMinute = 420, endMinute = 840, daysMask = 31),
    )

    @Test
    fun a_device_that_never_synced_has_no_cache() = runBlocking {
        assertNull(store.loadCached("device-1"))
    }

    @Test
    fun what_was_cached_is_what_comes_back() = runBlocking {
        val active = sampleRules()

        store.replaceAll("device-1", active)
        val loaded = store.loadCached("device-1")

        assertEquals(active, loaded)
    }

    @Test
    fun a_second_fetch_replaces_the_first_wholesale_not_merged() = runBlocking {
        store.replaceAll("device-1", sampleRules(packageName = "com.instagram.android"))

        val second = sampleRules(packageName = "com.tiktok.android")
        store.replaceAll("device-1", second)

        val loaded = store.loadCached("device-1")
        // Sprint 19's whole conflict strategy: the newest fetch wins unconditionally. If this
        // were a merge, both packages would show up here instead of only the second.
        assertEquals(listOf("com.tiktok.android"), loaded?.rules?.map { it.packageName })
    }

    @Test
    fun caches_for_two_devices_do_not_leak_into_each_other() = runBlocking {
        store.replaceAll("device-1", sampleRules(packageName = "com.instagram.android"))
        store.replaceAll("device-2", sampleRules(packageName = "com.tiktok.android"))

        assertEquals(
            listOf("com.instagram.android"),
            store.loadCached("device-1")?.rules?.map { it.packageName }
        )
        assertEquals(
            listOf("com.tiktok.android"),
            store.loadCached("device-2")?.rules?.map { it.packageName }
        )
    }
}
