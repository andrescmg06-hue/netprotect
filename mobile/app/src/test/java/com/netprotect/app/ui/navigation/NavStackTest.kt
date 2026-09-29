package com.netprotect.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NavStackTest {

    private sealed interface Route {
        data object Home : Route
        data class Device(val id: String) : Route
        data class Section(val deviceId: String, val name: String) : Route
    }

    private fun encode(route: Route): String = when (route) {
        Route.Home -> "home"
        is Route.Device -> "device/${route.id}"
        is Route.Section -> "section/${route.deviceId}/${route.name}"
    }

    private fun decode(text: String): Route? {
        val parts = text.split("/")
        return when (parts[0]) {
            "home" -> Route.Home
            "device" -> parts.getOrNull(1)?.let(Route::Device)
            "section" -> if (parts.size == 3) Route.Section(parts[1], parts[2]) else null
            else -> null
        }
    }

    @Test
    fun startsAtItsRootWithNowhereToGoBack() {
        val stack = NavStack.start<Route>(Route.Home)
        assertEquals(Route.Home, stack.current)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun pushOpensOnTopAndPopReturns() {
        val stack = NavStack.start<Route>(Route.Home).push(Route.Device("d1"))
        assertEquals(Route.Device("d1"), stack.current)
        assertTrue(stack.canGoBack)
        assertEquals(Route.Home, stack.pop().current)
    }

    @Test
    fun aDoubleTapDoesNotStackTheSameScreenTwice() {
        val once = NavStack.start<Route>(Route.Home).push(Route.Device("d1"))
        assertSame(once, once.push(Route.Device("d1")))
        assertEquals(2, once.entries.size)
    }

    @Test
    fun theSameScreenForAnotherDeviceIsADifferentRoute() {
        val stack = NavStack.start<Route>(Route.Home).push(Route.Device("d1")).push(Route.Device("d2"))
        assertEquals(3, stack.entries.size)
    }

    @Test
    fun popOnTheRootStaysOnTheRoot() {
        val stack = NavStack.start<Route>(Route.Home)
        assertSame(stack, stack.pop())
    }

    @Test
    fun popToDropsEverythingAboveTheMatch() {
        val stack = NavStack.start<Route>(Route.Home)
            .push(Route.Device("d1"))
            .push(Route.Section("d1", "apps"))
        assertEquals(Route.Device("d1"), stack.popTo { it is Route.Device }.current)
        assertEquals(Route.Home, stack.popTo { it == Route.Home }.current)
    }

    @Test
    fun popToWithNoMatchChangesNothing() {
        val stack = NavStack.start<Route>(Route.Home).push(Route.Device("d1"))
        assertSame(stack, stack.popTo { it is Route.Section })
    }

    @Test
    fun replaceTopSwapsOnlyTheTopEntry() {
        val stack = NavStack.start<Route>(Route.Home)
            .push(Route.Device("d1"))
            .replaceTop(Route.Device("d2"))
        assertEquals(listOf(Route.Home, Route.Device("d2")), stack.entries)
    }

    @Test
    fun resetToForgetsTheHistory() {
        val stack = NavStack.start<Route>(Route.Home).push(Route.Device("d1")).resetTo(Route.Home)
        assertEquals(listOf<Route>(Route.Home), stack.entries)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun aSavedStackComesBackIdentical() {
        val stack = NavStack.start<Route>(Route.Home)
            .push(Route.Device("d1"))
            .push(Route.Section("d1", "alerts"))
        val restored = NavStack.decode(stack.encode(::encode), ::decode)
        assertEquals(stack, restored)
    }

    @Test
    fun entriesThatNoLongerDecodeAreDroppedAndAnEmptyResultMeansStartOver() {
        val partial = NavStack.decode(listOf("home", "renamed-in-a-newer-build", "device/d1"), ::decode)
        assertEquals(listOf<Route>(Route.Home, Route.Device("d1")), partial?.entries)
        assertNull(NavStack.decode(listOf("gone", "also-gone"), ::decode))
        assertNull(NavStack.decode(emptyList<String>(), ::decode))
    }

    @Test
    fun poppingPastTheRootNeverThrowsOrEmptiesTheStack() {
        try {
            val stack = NavStack.start<Route>(Route.Home).pop().pop()
            assertEquals(Route.Home, stack.current)
        } catch (_: IllegalArgumentException) {
            fail("pop on the root must not throw")
        }
    }
}
