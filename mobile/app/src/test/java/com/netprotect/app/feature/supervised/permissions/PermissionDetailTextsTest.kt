package com.netprotect.app.feature.supervised.permissions

import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.DeviceAdmin
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.Location
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.Overlay
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission.UsageAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 66: the detail's texts are the real steps of each permission (PLAN §2.1), never the slide's 3 identical ones. */
class PermissionDetailTextsTest {

    private val all = SupervisedPermission.entries

    @Test
    fun positionFollowsTheCanonicalOrder() {
        assertEquals(
            listOf("PERMISO 1 DE 4", "PERMISO 2 DE 4", "PERMISO 3 DE 4", "PERMISO 4 DE 4"),
            listOf(UsageAccess, Location, Overlay, DeviceAdmin).map(::permissionPositionLabel),
        )
    }

    @Test
    fun everyPermissionHasThreeStepsAndNoTwoPermissionsShareThem() {
        all.forEach { assertEquals(3, permissionDetailSteps(it).size) }
        assertEquals(all.size, all.map(::permissionDetailSteps).toSet().size)
        // The first step names the button the person really sees.
        all.forEach { permission ->
            assertTrue(permissionDetailSteps(permission).first().contains("«${permissionTexts(permission).actionLabel}»"))
        }
    }

    @Test
    fun onlyUsageAccessAndOverlaySayTheyOpenSettings() {
        assertEquals("Activar en Ajustes", permissionTexts(UsageAccess).actionLabel)
        assertEquals("Activar en Ajustes", permissionTexts(Overlay).actionLabel)
        assertEquals("Permitir ubicación precisa", permissionTexts(Location).actionLabel)
        assertEquals("Activar protección", permissionTexts(DeviceAdmin).actionLabel)
        listOf(Location, DeviceAdmin).forEach { permission ->
            assertTrue(permissionDetailSteps(permission).first().contains("no se abren los Ajustes"))
        }
    }

    @Test
    fun usageAccessOpensTheListNotNetProtectsPage() {
        val steps = permissionDetailSteps(UsageAccess)
        assertTrue(steps[0].contains("no la página de NetProtect"))
        assertTrue(steps[1].startsWith("Busca NetProtect"))
    }

    @Test
    fun locationEndsWithTheAppSettingsWayOut() {
        assertEquals("Si el cuadro de Android no aparece, actívala desde Ajustes de la app.", permissionDetailSteps(Location).last())
    }

    @Test
    fun locationAsksForThePreciseOne() {
        assertEquals("Ubicación precisa", permissionTexts(Location).title)
        assertTrue(permissionTexts(Location).description.contains("ubicación precisa"))
        assertTrue(permissionDetailSteps(Location)[1].contains("ubicación precisa, no la aproximada"))
    }

    @Test
    fun approximateOnlySaysWhatToChangeWithTheSameButton() {
        val texts = permissionTexts(Location, locationApproximateOnly = true)
        assertEquals("Permitir ubicación precisa", texts.actionLabel)
        assertTrue(texts.description.startsWith("Ahora solo está permitida la ubicación aproximada"))
        val steps = permissionDetailSteps(Location, locationApproximateOnly = true)
        assertEquals(3, steps.size)
        assertTrue(steps.first().contains("«Permitir ubicación precisa»"))
        assertTrue(steps.first().contains("no se abren los Ajustes"))
        assertEquals(LOCATION_DENIED_HINT, steps.last())
    }

    // Sprint 73 (T6): the last step of location, «Permitir todo el tiempo» (Android 11+), is a trip to Settings.
    @Test
    fun backgroundMissingNamesTheLastStepAndItsButton() {
        val texts = permissionTexts(Location, locationBackgroundMissing = true)
        assertEquals("Permitir todo el tiempo", texts.actionLabel)
        assertEquals(BACKGROUND_LOCATION_ACTION, texts.actionLabel)
        assertTrue(texts.description.startsWith("Falta un último paso"))
        val steps = permissionDetailSteps(Location, locationBackgroundMissing = true)
        assertEquals(3, steps.size)
        assertTrue(steps.first().contains("«Permitir todo el tiempo»"))
        assertTrue(steps.first().contains("Ajustes de NetProtect"))
    }

    @Test
    fun approximateOnlyWinsOverBackgroundMissing() {
        assertEquals(
            permissionTexts(Location, locationApproximateOnly = true).description,
            permissionTexts(Location, locationApproximateOnly = true, locationBackgroundMissing = true).description,
        )
        assertEquals("Permitir ubicación precisa", permissionTexts(Location, true, true).actionLabel)
    }

    @Test
    fun backgroundMissingChangesNothingForTheOtherPermissions() {
        listOf(UsageAccess, Overlay, DeviceAdmin).forEach { permission ->
            assertEquals(permissionDetailTexts(permission), permissionDetailTexts(permission, locationBackgroundMissing = true))
        }
    }

    // The explanation shown before Android's step says what is shared, how often, with whom, and what is not.
    @Test
    fun theExplanationBeforeAndroidSaysWhatIsAndIsNotShared() {
        val body = BACKGROUND_LOCATION_EXPLANATION_BODY
        assertTrue(body.contains("solo la ubicación"))
        assertTrue(body.contains("15 minutos"))
        assertTrue(body.contains("tutor vinculado"))
        assertTrue(body.contains("nada más"))
        assertTrue(body.contains("«Permitir todo el tiempo»"))
        assertTrue(body.contains("Puedes quitarlo"))
        assertEquals("Ubicación con la app cerrada", BACKGROUND_LOCATION_EXPLANATION_TITLE)
        assertEquals("Continuar", BACKGROUND_LOCATION_EXPLANATION_CONTINUE)
        assertEquals("Ahora no", BACKGROUND_LOCATION_EXPLANATION_DISMISS)
    }

    @Test
    fun approximateOnlyChangesNothingForTheOtherPermissions() {
        listOf(UsageAccess, Overlay, DeviceAdmin).forEach { permission ->
            assertEquals(permissionDetailTexts(permission), permissionDetailTexts(permission, locationApproximateOnly = true))
        }
    }

    @Test
    fun noLocationTextStillPromisesOnlyTheApproximateOne() {
        listOf(false, true).flatMap { permissionDetailTexts(Location, it) }.forEach { text ->
            assertFalse(text, text.contains("No se usa la ubicación precisa"))
            assertFalse(text, text.contains("solo pide la aproximada"))
        }
    }

    @Test
    fun deviceAdminOnlyWarns() {
        assertEquals("Solo avisa a tu tutor: no bloquea la desinstalación.", permissionDetailNote(DeviceAdmin))
        listOf(UsageAccess, Location, Overlay).forEach { assertNull(permissionDetailNote(it)) }
    }

    @Test
    fun noTextClaimsPreventionControlOrRetention() {
        val banned = listOf("impide", "ni controla", "controla", "retención", "conserva", "días", "meses", "pendiente de validar")
        (
            all.flatMap { permissionDetailTexts(it) } +
                permissionDetailTexts(Location, locationApproximateOnly = true) +
                permissionDetailTexts(Location, locationBackgroundMissing = true) +
                BACKGROUND_LOCATION_EXPLANATION_BODY
            ).forEach { text ->
            banned.forEach { word -> assertFalse("«$word» in «$text»", text.contains(word, ignoreCase = true)) }
        }
    }
}
