package com.netprotect.app.feature.tutor

import com.netprotect.app.ui.navigation.NavStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TutorNavigationTest {

    private fun start() = NavStack.start<TutorRoute>(TutorRoute.Home)

    @Test
    fun theHighlightedTabIsTheRootOfTheStack() {
        val fromHome = start().push(TutorRoute.Detail("d1"))
        assertEquals(TutorTab.Home, fromHome.currentTab)
        val fromDevices = start().selectTab(TutorTab.Devices).push(TutorRoute.Detail("d1"))
        assertEquals(TutorTab.Devices, fromDevices.currentTab)
    }

    @Test
    fun selectingATabStartsItFromItsRoot() {
        val deep = start().selectTab(TutorTab.Devices).push(TutorRoute.Detail("d1"))
            .push(TutorRoute.Section("d1", DeviceSection.Apps))
        assertEquals(listOf<TutorRoute>(TutorRoute.Devices), deep.selectTab(TutorTab.Devices).entries)
        assertEquals(listOf<TutorRoute>(TutorRoute.More), deep.selectTab(TutorTab.More).entries)
    }

    @Test
    fun backGoesUpOneLevelThenToInicioThenLeavesTheApp() {
        var stack = start().selectTab(TutorTab.Devices).push(TutorRoute.Detail("d1"))
            .push(TutorRoute.Section("d1", DeviceSection.Alerts))
        stack = stack.back()!!
        assertEquals(TutorRoute.Detail("d1"), stack.current)
        stack = stack.back()!!
        assertEquals(TutorRoute.Devices, stack.current)
        stack = stack.back()!!
        assertEquals(TutorRoute.Home, stack.current)
        assertNull(stack.back())
    }

    @Test
    fun everyRouteSurvivesSavingAndRestoring() {
        val routes = listOf(
            TutorRoute.Home, TutorRoute.Devices, TutorRoute.Activity, TutorRoute.More,
            TutorRoute.Detail("3f1c9a2e-0000-4000-8000-000000000001"),
        ) + DeviceSection.entries.map { TutorRoute.Section("3f1c9a2e-0000-4000-8000-000000000001", it) }
        routes.forEach { assertEquals(it, decodeTutorRoute(encodeTutorRoute(it))) }
    }

    @Test
    fun malformedSavedRoutesAreDropped() {
        listOf("", "detail", "detail/", "section/d1", "section/d1/Nope", "somewhere").forEach {
            assertNull(it, decodeTutorRoute(it))
        }
    }
}
