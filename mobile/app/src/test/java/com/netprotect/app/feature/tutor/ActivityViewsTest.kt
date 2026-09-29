package com.netprotect.app.feature.tutor

import com.netprotect.app.core.network.AlertSilence
import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.BlockCountEntry
import com.netprotect.app.core.network.ComplianceEntry
import com.netprotect.app.core.network.DeviceAlert
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.DeviceStatistics
import com.netprotect.app.core.network.HistoryEvent
import com.netprotect.app.core.network.TopAppEntry
import com.netprotect.app.feature.tutor.sections.ALERT_GONE_MESSAGE
import com.netprotect.app.feature.tutor.sections.AlertFilter
import com.netprotect.app.feature.tutor.sections.AlertsController
import com.netprotect.app.feature.tutor.sections.AlertsData
import com.netprotect.app.feature.tutor.sections.HistoryKind
import com.netprotect.app.feature.tutor.sections.NO_USAGE_IN_PERIOD
import com.netprotect.app.feature.tutor.sections.StatisticsController
import com.netprotect.app.feature.tutor.sections.StatisticsView
import com.netprotect.app.feature.tutor.sections.StatsPeriod
import com.netprotect.app.feature.tutor.sections.alertItems
import com.netprotect.app.feature.tutor.sections.appLabels
import com.netprotect.app.feature.tutor.sections.complianceDays
import com.netprotect.app.feature.tutor.sections.filterAlerts
import com.netprotect.app.feature.tutor.sections.historyDays
import com.netprotect.app.feature.tutor.sections.isSilenceActive
import com.netprotect.app.feature.tutor.sections.statisticsView
import com.netprotect.app.ui.state.LoadState
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityViewsTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 9, 29)
    private val now = Instant.parse("2026-09-29T20:00:00Z") // 3:00 p. m. in Bogotá
    private val labels = appLabels(
        listOf(DeviceApplicationSummary("com.google.android.youtube", "YouTube", false, null, "2026-09-29", 60)),
    )

    // ---- historial ------------------------------------------------------------------------

    private fun block(id: String, at: String, pkg: String? = "com.google.android.youtube", reason: String? = "DAILY_LIMIT") =
        HistoryEvent(id, "APP_RULE", at, pkg, reason, null, null)

    private fun zone(id: String, at: String, type: String, name: String? = "Casa") =
        HistoryEvent(id, "GEOFENCE", at, null, null, name, type)

    @Test
    fun historyGroupsByTheTutorsDayNotUtc() {
        // 03:00 UTC on the 29th is 10:00 p. m. on the 28th in Bogotá: "Ayer", not "Hoy".
        val days = historyDays(
            listOf(block("a", "2026-09-29T03:00:00Z"), zone("b", "2026-09-29T18:42:00Z", "ENTER")),
            labels, today, bogota,
        )
        assertEquals(listOf("Hoy", "Ayer"), days.map { it.title })
        assertEquals("29 de septiembre de 2026", days[0].subtitle)
        assertEquals("10:00 p. m.", days[1].rows.single().time)
    }

    @Test
    fun olderDaysShowOnlyTheDateAndNewestComesFirst() {
        val days = historyDays(
            listOf(block("old", "2026-09-20T15:00:00Z"), block("new", "2026-09-20T16:00:00Z")),
            labels, today, bogota,
        )
        assertEquals("20 de septiembre de 2026", days.single().title)
        assertNull(days.single().subtitle)
        assertEquals(listOf("new", "old"), days.single().rows.map { it.id })
    }

    @Test
    fun historyRowsUseAppNamesAndWebReasonLabels() {
        val rows = historyDays(
            listOf(
                block("a", "2026-09-29T18:00:00Z"),
                block("b", "2026-09-29T17:00:00Z", pkg = "com.unknown.app", reason = null),
                zone("c", "2026-09-29T16:00:00Z", "EXIT", name = null),
            ),
            labels, today, bogota,
        ).single().rows
        assertEquals("Bloqueo de app · YouTube", rows[0].title)
        assertEquals("Motivo: Límite diario", rows[0].subtitle)
        assertEquals("Bloqueo de app · com.unknown.app", rows[1].title)
        assertNull(rows[1].subtitle)
        assertEquals(HistoryKind.Exit, rows[2].kind)
        assertEquals("Salió · una zona", rows[2].title)
    }

    // ---- estadísticas ---------------------------------------------------------------------

    private fun stats(
        blocks: List<BlockCountEntry> = emptyList(),
        compliance: List<ComplianceEntry> = emptyList(),
        top: List<TopAppEntry> = emptyList(),
    ) = DeviceStatistics("7d", top, emptyList(), blocks, compliance)

    @Test
    fun complianceCountsDaysNotPercentOfTime() {
        assertEquals("5 de 7 días", complianceDays(5, 7))
        assertEquals("1 de 1 día", complianceDays(1, 1))
        assertEquals(NO_USAGE_IN_PERIOD, complianceDays(0, 0))
        val rows = statisticsView(
            stats(
                compliance = listOf(
                    ComplianceEntry("APP", "com.google.android.youtube", null, 60, 7, 5, 5.0 / 7),
                    ComplianceEntry("CATEGORY", null, "GAMES", 30, 0, 0, null),
                ),
            ),
            labels,
        ).compliance
        assertEquals("YouTube", rows[0].name)
        assertEquals("Límite: 60 min/día", rows[0].limit)
        assertEquals(5f / 7, rows[0].fraction!!, 0.001f)
        assertEquals("Juegos", rows[1].name)
        assertNull(rows[1].fraction)
    }

    @Test
    fun blocksShowOnlyRealReasonsWithCountMostFirstUpToSeven() {
        val reasons = listOf("BLOCK", "DAILY_LIMIT", "WEEKLY_LIMIT", "SCHEDULE", "CATEGORY", "SCHOOL_MODE", "DEFAULT_POLICY", "ALLOW")
        val blocks = statisticsView(
            stats(blocks = reasons.mapIndexed { i, r -> BlockCountEntry(r, i) }), // BLOCK has 0
            labels,
        ).blocks
        assertEquals(7, blocks.size)
        assertEquals("ALLOW", blocks.first().ruleType)
        assertFalse(blocks.any { it.ruleType == "BLOCK" })
        assertEquals("Horario escolar", blocks.first { it.ruleType == "SCHOOL_MODE" }.label)
    }

    @Test
    fun topAppsAreRelativeToTheMostUsed() {
        val top = statisticsView(
            stats(top = listOf(TopAppEntry("a.b", null, 3600), TopAppEntry("com.google.android.youtube", null, 900))),
            labels,
        ).topApps
        assertEquals(1f, top[0].fraction, 0f)
        assertEquals(0.25f, top[1].fraction, 0.001f)
        assertEquals("a.b", top[0].name)
        assertEquals("YouTube", top[1].name)
        assertEquals("1 h", top[0].duration)
    }

    // ---- alertas --------------------------------------------------------------------------

    private fun alert(id: String, level: String, read: Boolean = false, key: String = "k-$id", count: Int = 1) =
        DeviceAlert(id, level, "APP_BLOCKED", "com.google.android.youtube", null, count, "2026-09-29T18:42:00Z",
            if (read) "2026-09-29T19:00:00Z" else null, key)

    @Test
    fun criticalFilterIncludesHighAndWarningsOnlyWarning() {
        val items = alertItems(
            listOf(alert("c", "CRITICAL"), alert("h", "HIGH", read = true), alert("w", "WARNING"), alert("i", "INFO")),
            emptyList(), labels, now, bogota,
        )
        assertEquals(listOf("c", "h"), filterAlerts(items, AlertFilter.Critical).map { it.id })
        assertEquals(listOf("w"), filterAlerts(items, AlertFilter.Warnings).map { it.id })
        assertEquals(listOf("c", "w", "i"), filterAlerts(items, AlertFilter.Unread).map { it.id })
        assertEquals(4, filterAlerts(items, AlertFilter.All).size)
    }

    @Test
    fun alertTextUsesAppNameTimeAndRepetitions() {
        val item = alertItems(listOf(alert("a", "WARNING", count = 3)), emptyList(), labels, now, bogota).single()
        assertEquals("Se bloqueó YouTube", item.title)
        assertEquals("Hoy, 1:42 p. m.", item.time)
        assertEquals("Repetido 3 veces", item.repeated)
        assertNull(alertItems(listOf(alert("b", "INFO")), emptyList(), labels, now, bogota).single().repeated)
    }

    @Test
    fun onlyActiveSilencesMarkAnAlertSilenced() {
        assertTrue(isSilenceActive(AlertSilence("s", "k", null), now))
        assertTrue(isSilenceActive(AlertSilence("s", "k", "2026-10-01T00:00:00Z"), now))
        assertFalse(isSilenceActive(AlertSilence("s", "k", "2026-09-01T00:00:00Z"), now))
        val items = alertItems(
            listOf(alert("a", "HIGH", key = "one"), alert("b", "HIGH", key = "two")),
            listOf(AlertSilence("s1", "one", null), AlertSilence("s2", "two", "2026-09-01T00:00:00Z")),
            labels, now, bogota,
        )
        assertEquals(listOf(true, false), items.map { it.silenced })
    }

    // ---- controlador de alertas -----------------------------------------------------------

    private fun data(vararg alerts: DeviceAlert) = AlertsData(alerts.toList(), emptyList(), labels)

    @Test
    fun aSecondTapWhileSendingIsIgnored() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var sent = 0
        val controller = AlertsController(
            load = { data(alert("a", "HIGH")) },
            markReadRequest = { sent++; gate.await() },
            silenceRequest = { sent++ },
        )
        controller.refresh()
        val first = async { controller.markRead("a") }
        yield()
        assertEquals("a", controller.busyId)
        controller.markRead("a")
        controller.askSilence("a")
        assertNull(controller.silenceTarget)
        gate.complete(Unit)
        first.await()
        assertEquals(1, sent)
        assertNull(controller.busyId)
    }

    @Test
    fun silenceNeedsConfirmationAndReloadsWithoutEmptyingTheList() = runBlocking {
        var loads = 0
        var silenced: String? = null
        val controller = AlertsController(
            load = { loads++; data(alert("a", "HIGH")) },
            markReadRequest = {},
            silenceRequest = { silenced = it },
        )
        controller.refresh()
        controller.askSilence("a")
        assertNull(silenced)
        controller.dismissSilence()
        controller.confirmSilence()
        assertNull(silenced) // dismissed: nothing sent
        controller.askSilence("a")
        controller.confirmSilence()
        assertEquals("a", silenced)
        assertEquals(2, loads)
        assertTrue(controller.state is LoadState.Loaded)
    }

    @Test
    fun aFailedActionKeepsTheListAndSaysWhy() = runBlocking {
        var failReload = false
        val controller = AlertsController(
            load = { if (failReload) throw IOException("offline") else data(alert("a", "HIGH")) },
            markReadRequest = { throw ApiException("alert_not_found", 404) },
            silenceRequest = {},
        )
        controller.refresh()
        controller.markRead("a")
        assertEquals(ALERT_GONE_MESSAGE, controller.actionError)
        assertTrue(controller.state is LoadState.Loaded)

        failReload = true
        controller.askSilence("a")
        controller.confirmSilence()
        assertTrue(controller.actionError!!.startsWith("Se guardó, pero no se pudo actualizar la lista."))
        assertTrue(controller.state is LoadState.Loaded)
    }

    // ---- controlador de estadísticas ------------------------------------------------------

    @Test
    fun aSlowAnswerForAPeriodAlreadyLeftIsDiscarded() = runBlocking {
        val todayGate = CompletableDeferred<Unit>()
        val empty = StatisticsView(emptyList(), emptyList(), emptyList())
        val week = empty.copy(blocks = emptyList(), compliance = emptyList(), topApps = emptyList())
        val controller = StatisticsController { period ->
            if (period == StatsPeriod.Today) {
                todayGate.await()
                throw IOException("stale")
            }
            week
        }
        val slow = async { controller.refresh() } // Hoy, blocked
        yield()
        controller.select(StatsPeriod.Week)
        todayGate.complete(Unit)
        slow.await()
        assertEquals(StatsPeriod.Week, controller.period)
        assertEquals(LoadState.Loaded(week), controller.state)
    }
}
