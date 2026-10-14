package com.netprotect.app.feature.supervised.permissions

import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.DeviceAdmin
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.Location
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.Overlay
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.UsageAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 67: detecting a removed permission (lámina 13.4) and the notice's wording. */
class PermissionRevokedTest {

    private val all = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true)
    private val allSet = SupervisedPermission.entries.toSet()

    @Test
    fun firstReadHasNoBaselineSoNothingIsRevoked() {
        assertEquals(emptyList<SupervisedPermission>(), revokedPermissions(null, all))
        assertEquals(
            emptyList<SupervisedPermission>(),
            revokedPermissions(null, PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false)),
        )
    }

    @Test
    fun nothingChanged() {
        assertEquals(emptyList<SupervisedPermission>(), revokedPermissions(allSet, all))
        val some = all.copy(location = false, deviceAdmin = false)
        assertEquals(emptyList<SupervisedPermission>(), revokedPermissions(grantedPermissions(some), some))
    }

    @Test
    fun oneRemoved() {
        assertEquals(listOf(Overlay), revokedPermissions(allSet, all.copy(overlay = false)))
    }

    @Test
    fun severalRemovedComeInCanonicalOrder() {
        assertEquals(
            listOf(UsageAccess, Location, DeviceAdmin),
            revokedPermissions(allSet, all.copy(deviceAdmin = false, usageAccess = false, location = false)),
        )
    }

    @Test
    fun neverGrantedIsNotRevoked() {
        // Location was never granted: still pending now is not a removal.
        val before = setOf(UsageAccess, Overlay, DeviceAdmin)
        assertEquals(emptyList<SupervisedPermission>(), revokedPermissions(before, all.copy(location = false)))
    }

    @Test
    fun newlyGrantedIsNotRevoked() {
        assertEquals(emptyList<SupervisedPermission>(), revokedPermissions(setOf(UsageAccess), all))
    }

    @Test
    fun grantedAgainIsNotRevoked() {
        val removed = all.copy(overlay = false)
        assertEquals(listOf(Overlay), revokedPermissions(allSet, removed))
        // The shell writes the snapshot after each read; granting it again then reports nothing.
        assertEquals(emptyList<SupervisedPermission>(), revokedPermissions(grantedPermissions(removed), all))
    }

    @Test
    fun grantedPermissionsFollowsTheFlags() {
        assertEquals(setOf(UsageAccess, Overlay), grantedPermissions(all.copy(location = false, deviceAdmin = false)))
    }

    @Test
    fun bodyNamesThePermissionAndSettings() {
        SupervisedPermission.entries.forEach { permission ->
            val texts = permissionRevokedTexts(permission)
            assertEquals(permissionTexts(permission).title, texts.permissionTitle)
            assertTrue(texts.body, texts.body.startsWith("«${texts.permissionTitle}» se quitó en Ajustes de Android."))
        }
    }

    @Test
    fun onlyUsageAccessSaysTheTutorCanSeeIt() {
        // Only usage_access_granted travels in the heartbeat (backend services/tamper.py → PERMISSION_REVOKED).
        assertEquals("Tu tutor puede ver que este permiso está desactivado.", permissionRevokedTexts(UsageAccess).note)
        listOf(Location, Overlay, DeviceAdmin).forEach { permission ->
            assertEquals("Activa el permiso para que NetProtect vuelva a funcionar.", permissionRevokedTexts(permission).note)
            permissionRevokedAllTexts(permission).forEach { text ->
                assertFalse("«$text» mentions the tutor", text.contains("tutor", ignoreCase = true))
            }
        }
    }

    @Test
    fun overlayOnlySaysItMayNotShowTheBlockScreen() {
        assertTrue(permissionRevokedTexts(Overlay).body.contains("es posible que NetProtect no pueda mostrar la pantalla de bloqueo"))
    }

    @Test
    fun noControlRetentionOrTutorPromise() {
        val banned = listOf("controla", "retención", "el tutor lo sabrá", "tu tutor lo sabrá", "recibirá una alerta", "nunca", "garantiza")
        SupervisedPermission.entries.forEach { permission ->
            permissionRevokedAllTexts(permission).forEach { text ->
                banned.forEach { word -> assertFalse("«$word» in «$text»", text.contains(word, ignoreCase = true)) }
            }
        }
    }
}
