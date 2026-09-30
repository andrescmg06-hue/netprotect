package com.netprotect.app.feature.tutor

import com.netprotect.app.core.network.AuditLogEntry
import com.netprotect.app.core.network.AuditPage
import com.netprotect.app.feature.tutor.activity.MyActivityController
import com.netprotect.app.feature.tutor.activity.activityDays
import com.netprotect.app.ui.format.AuditLabels
import com.netprotect.app.ui.state.LoadState
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyActivityTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 9, 29)

    private fun entry(id: String, action: String, at: String, type: String? = null, resource: String? = null) =
        AuditLogEntry(id, action, type, resource, at)

    // ---- etiquetas ------------------------------------------------------------------------

    /** Every action the backend records (all record_audit_event calls, Sprint 47) has a label. */
    private val backendActions = listOf(
        "LOGIN", "LOGOUT", "TOKEN_REFRESH", "ROLE_GRANTED", "PAIRING_CODE_GENERATED", "PAIRING_CODE_REVOKED",
        "DEVICE_LINKED", "DEVICE_UNLINKED", "DEVICE_RENAMED", "DEVICE_POLICY_CHANGED", "SCHOOL_MODE_CHANGED",
        "APP_RULE_CREATED", "APP_RULE_UPDATED", "APP_RULE_DELETED", "CATEGORY_RULE_CREATED",
        "CATEGORY_RULE_UPDATED", "CATEGORY_RULE_DELETED", "APP_CATEGORY_ASSIGNED", "APP_CATEGORY_REASSIGNED",
        "APP_CATEGORY_UNASSIGNED", "GEOFENCE_CREATED", "GEOFENCE_UPDATED", "GEOFENCE_DELETED", "ALERT_READ",
        "ALERT_SILENCED", "ALERT_SILENCE_REMOVED", "LOCATION_VIEWED", "LOCATION_HISTORY_VIEWED",
        "SCREEN_SHARE_REQUESTED", "SCREEN_SHARE_CONSENT_GRANTED", "SCREEN_SHARE_CONSENT_DENIED",
        "SCREEN_SHARE_STARTED", "SCREEN_SHARE_STOPPED",
    )

    @Test
    fun everyBackendActionHasASpanishLabelAndAFamily() {
        backendActions.forEach { action ->
            assertFalse("$action has no label", AuditLabels.auditActionLabel(action) == action)
            assertFalse("$action has no family", AuditLabels.family(action) == AuditLabels.Family.Other)
        }
        assertEquals("Ubicación consultada", AuditLabels.auditActionLabel("LOCATION_VIEWED"))
        assertEquals("SOMETHING_NEW", AuditLabels.auditActionLabel("SOMETHING_NEW"))
        assertEquals(AuditLabels.Family.Location, AuditLabels.family("LOCATION_HISTORY_VIEWED"))
        assertEquals(AuditLabels.Family.Rules, AuditLabels.family("APP_CATEGORY_REASSIGNED"))
    }

    @Test
    fun resourceIsTheDeviceNameOnlyWhenKnown() {
        val names = mapOf("d1" to "Tablet de Sofía")
        assertEquals("Tablet de Sofía", AuditLabels.resourceLabel("device", "d1", names))
        assertEquals("Dispositivo", AuditLabels.resourceLabel("device", "gone", names))
        assertEquals("Alerta", AuditLabels.resourceLabel("alert_silence", "APP_BLOCKED:com.x", names))
        assertEquals("Modo tutor", AuditLabels.resourceLabel("role", "TUTOR", names))
        assertNull(AuditLabels.resourceLabel(null, null, names))
    }

    // ---- agrupación -----------------------------------------------------------------------

    @Test
    fun groupedByTheTutorsDayNewestFirst() {
        val days = activityDays(
            listOf(
                entry("a", "LOGIN", "2026-09-29T03:00:00Z"), // 10:00 p. m. on the 28th in Bogotá
                entry("b", "LOCATION_VIEWED", "2026-09-29T20:42:00Z", "device", "d1"),
                entry("c", "DEVICE_RENAMED", "2026-09-20T15:00:00Z", "device", "d1"),
            ),
            mapOf("d1" to "Tablet de Sofía"), today, bogota,
        )
        assertEquals(listOf("Hoy", "Ayer", "20 de septiembre de 2026"), days.map { it.title })
        assertEquals("29 de septiembre de 2026", days[0].subtitle)
        assertNull(days[2].subtitle)
        val row = days[0].rows.single()
        assertEquals("Ubicación consultada", row.title)
        assertEquals("Tablet de Sofía", row.subtitle)
        assertEquals("3:42 p. m.", row.time)
        assertEquals("10:00 p. m.", days[1].rows.single().time)
    }

    // ---- paginación -----------------------------------------------------------------------

    private fun page(ids: IntRange, total: Int) =
        AuditPage(ids.map { entry("e$it", "LOGIN", "2026-09-29T15:00:00Z") }, total)

    @Test
    fun loadMoreAppendsAndStopsAtTheTotal() = runBlocking {
        val offsets = mutableListOf<Int>()
        val controller = MyActivityController { offset ->
            offsets += offset
            if (offset == 0) page(1..50, 60) else page(51..60, 60)
        }
        controller.refresh()
        assertTrue(controller.hasMore)
        controller.loadMore()
        assertEquals(listOf(0, 50), offsets)
        assertEquals(60, (controller.state as LoadState.Loaded).value.size)
        assertFalse(controller.hasMore)
        controller.loadMore() // nothing more: no request
        assertEquals(2, offsets.size)
    }

    @Test
    fun entriesShiftedByNewActionsAreNotShownTwice() = runBlocking {
        // Two new actions happened between the pages: the second page starts with e49, e50 again.
        val controller = MyActivityController { offset ->
            if (offset == 0) page(1..50, 60) else page(49..58, 62)
        }
        controller.refresh()
        controller.loadMore()
        val ids = (controller.state as LoadState.Loaded).value.map { it.id }
        assertEquals(ids.distinct(), ids)
        assertEquals(58, ids.size)
    }

    @Test
    fun aPageWithNothingNewStopsOfferingMore() = runBlocking {
        val controller = MyActivityController { page(1..50, 80) }
        controller.refresh()
        controller.loadMore()
        assertFalse(controller.hasMore)
    }

    @Test
    fun aFailedLoadMoreKeepsWhatIsShown() = runBlocking {
        var fail = false
        val controller = MyActivityController { offset ->
            if (fail) throw IOException("offline")
            page(1..50, 60).takeIf { offset == 0 } ?: page(51..60, 60)
        }
        controller.refresh()
        fail = true
        controller.loadMore()
        assertEquals(50, (controller.state as LoadState.Loaded).value.size)
        assertTrue(controller.moreError != null)
        assertFalse(controller.loadingMore)
        assertTrue(controller.hasMore)
    }
}
