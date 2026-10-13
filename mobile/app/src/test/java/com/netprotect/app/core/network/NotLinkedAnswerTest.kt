package com.netprotect.app.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotLinkedAnswerTest {

    @Test
    fun theBackendsNotLinked404IsAnUnlink() {
        assertTrue(isNotLinkedAnswer(404, "not_linked"))
    }

    @Test
    fun aNotLinkedDetailWithAnotherStatusIsNot() {
        assertFalse(isNotLinkedAnswer(403, "not_linked"))
        assertFalse(isNotLinkedAnswer(null, "not_linked"))
    }

    @Test
    fun anyOther404SaysNothingAboutTheLink() {
        // A proxy mid-deploy or a mis-routed path: the client's fallback message, or another detail.
        assertFalse(isNotLinkedAnswer(404, "HTTP 404"))
        assertFalse(isNotLinkedAnswer(404, "device_not_found"))
        assertFalse(isNotLinkedAnswer(404, null))
        assertFalse(isNotLinkedAnswer(404, ""))
    }
}
