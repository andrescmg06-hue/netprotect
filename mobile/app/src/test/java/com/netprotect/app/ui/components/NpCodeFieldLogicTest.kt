package com.netprotect.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Sprint 63: which digit each box of NpCodeField shows and which box has the caret. */
class NpCodeFieldLogicTest {

    @Test
    fun boxesShowTheirDigitOrNothing() {
        assertEquals('4', codeDigitAt("482", 0))
        assertEquals('2', codeDigitAt("482", 2))
        assertNull(codeDigitAt("482", 3))
        assertNull(codeDigitAt("4a2", 1))
    }

    @Test
    fun theCaretIsOnTheFirstEmptyBoxWhileEditing() {
        assertEquals(0, activeCodeBox("", NpCodeFieldState.Editing))
        assertEquals(3, activeCodeBox("482", NpCodeFieldState.Editing))
        assertNull(activeCodeBox("482915", NpCodeFieldState.Editing))
    }

    @Test
    fun noCaretWhileSendingOrRefused() {
        assertNull(activeCodeBox("482", NpCodeFieldState.Sending))
        assertNull(activeCodeBox("482915", NpCodeFieldState.Error))
    }
}
