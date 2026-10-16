package com.netprotect.app.feature.supervised.permissions

import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.DeviceAdmin
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.Location
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.Overlay
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.UsageAccess
import com.netprotect.app.ui.components.RailStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStepsTest {

    private fun ui(usage: Boolean, location: Boolean, overlay: Boolean, admin: Boolean) =
        PermissionsUi(usageAccess = usage, location = location, overlay = overlay, deviceAdmin = admin)

    @Test
    fun slideCasePendingFirstAndTheFirstPendingIsNext() {
        val steps = permissionSteps(ui(usage = true, location = true, overlay = false, admin = false))
        assertEquals(
            listOf(
                PermissionStep(Overlay, RailStatus.Next),
                PermissionStep(DeviceAdmin, RailStatus.Pending),
                PermissionStep(UsageAccess, RailStatus.Done),
                PermissionStep(Location, RailStatus.Done),
            ),
            steps,
        )
    }

    // Sprint 73 (T6): precise granted but «Permitir todo el tiempo» missing (Android 11+): the shell reports location as
    // not configured, so it is a pending step and counts as one, never "Configurado" with a half-done permission.
    @Test
    fun locationWithoutTheBackgroundStepIsStillAPendingStep() {
        val permissions = PermissionsUi(
            usageAccess = true, location = false, overlay = true, deviceAdmin = true, locationBackgroundMissing = true,
        )
        assertEquals(1, permissions.pending)
        assertEquals(PermissionStep(Location, RailStatus.Next), permissionSteps(permissions).first())
        assertEquals("3 de 4 configurados", configuredCaption(permissions))
    }

    @Test
    fun approximateOnlyLocationIsStillAPendingStep() {
        val permissions = PermissionsUi(
            usageAccess = true, location = false, overlay = true, deviceAdmin = true, locationApproximateOnly = true,
        )
        assertEquals(1, permissions.pending)
        assertEquals(PermissionStep(Location, RailStatus.Next), permissionSteps(permissions).first())
    }

    @Test
    fun allConfiguredKeepsTheCanonicalOrderWithNoNext() {
        val steps = permissionSteps(ui(usage = true, location = true, overlay = true, admin = true))
        assertEquals(listOf(UsageAccess, Location, Overlay, DeviceAdmin), steps.map { it.permission })
        assertEquals(List(4) { RailStatus.Done }, steps.map { it.status })
    }

    @Test
    fun allPendingHasExactlyOneNext() {
        val steps = permissionSteps(ui(usage = false, location = false, overlay = false, admin = false))
        assertEquals(listOf(UsageAccess, Location, Overlay, DeviceAdmin), steps.map { it.permission })
        assertEquals(
            listOf(RailStatus.Next, RailStatus.Pending, RailStatus.Pending, RailStatus.Pending),
            steps.map { it.status },
        )
    }

    @Test
    fun aSinglePendingPermissionGoesFirstAsNext() {
        val steps = permissionSteps(ui(usage = true, location = false, overlay = true, admin = true))
        assertEquals(PermissionStep(Location, RailStatus.Next), steps.first())
        assertEquals(1, steps.count { it.status != RailStatus.Done })
    }

    @Test
    fun configuredCaptionCountsTheGrantedOnes() {
        assertEquals("2 de 4 configurados", configuredCaption(ui(usage = true, location = true, overlay = false, admin = false)))
        assertEquals("0 de 4 configurados", configuredCaption(ui(usage = false, location = false, overlay = false, admin = false)))
        assertEquals("3 de 4 configurados", configuredCaption(ui(usage = true, location = true, overlay = true, admin = false)))
    }
}
