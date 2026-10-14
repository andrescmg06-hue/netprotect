package com.netprotect.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class RoleDecisionTest {

    @Test
    fun supervisedRoleEntersDirectly() {
        assertEquals(RoleOutcome.Supervised, decideRole(listOf("SUPERVISADO")))
    }

    @Test
    fun noRolesGrantsSupervised() {
        assertEquals(RoleOutcome.GrantSupervised, decideRole(emptyList()))
    }

    @Test
    fun tutorOnlyIsNeverGrantedSilently() {
        assertEquals(RoleOutcome.TutorAccount, decideRole(listOf("TUTOR")))
    }

    @Test
    fun bothRolesEnterAsSupervised() {
        assertEquals(RoleOutcome.Supervised, decideRole(listOf("TUTOR", "SUPERVISADO")))
    }
}
