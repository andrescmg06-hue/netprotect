package com.netprotect.app.feature.supervised

import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.MyDeviceInfo
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkCheckTest {

    private fun device(tutors: List<String>, deviceId: String = "dev-1") = MyDeviceInfo(
        deviceId = deviceId,
        deviceName = "Tablet",
        status = "ONLINE",
        tutorLabel = if (tutors.isEmpty()) null else tutors.joinToString(", "),
        tutors = tutors,
    )

    // ---- classification --------------------------------------------------------------------

    @Test
    fun notFoundIsUnlinked() {
        assertEquals(LinkCheck.Unlinked, classifyLinkCheck(Result.success(null)))
    }

    @Test
    fun aRowWithNoTutorsLeftIsUnlinked() {
        assertEquals(LinkCheck.Unlinked, classifyLinkCheck(Result.success(device(emptyList()))))
    }

    @Test
    fun tutorsPresentIsLinkedWithTheirLabel() {
        val check = classifyLinkCheck(Result.success(device(listOf("Ana", "Luis"))))
        assertTrue(check is LinkCheck.Linked)
        check as LinkCheck.Linked
        assertEquals("dev-1", check.deviceId)
        assertEquals("Ana, Luis", check.tutorLabel)
    }

    @Test
    fun aNetworkFailureIsNeverAnUnlink() {
        assertEquals(LinkCheck.Inconclusive, classifyLinkCheck(Result.failure(IOException("offline"))))
        assertEquals(LinkCheck.Inconclusive, classifyLinkCheck(Result.failure(SocketTimeoutException())))
    }

    @Test
    fun anyOtherFailureIsNeverAnUnlink() {
        assertEquals(LinkCheck.Inconclusive, classifyLinkCheck(Result.failure(ApiException("down", 503))))
        assertEquals(LinkCheck.Inconclusive, classifyLinkCheck(Result.failure(IllegalArgumentException("bad body"))))
        assertEquals(LinkCheck.Inconclusive, classifyLinkCheck(Result.failure(IllegalStateException())))
    }

    // ---- transitions (G-19) ----------------------------------------------------------------

    @Test
    fun aChangeInTutorsIsNotATransition() {
        val before = classifyLinkCheck(Result.success(device(listOf("Ana"))))
        val after = classifyLinkCheck(Result.success(device(listOf("Ana", "Luis"))))
        assertFalse(changesLinkIdentity("dev-1", before))
        assertFalse(changesLinkIdentity("dev-1", after))
    }

    @Test
    fun inconclusiveIsNotATransition() {
        assertFalse(changesLinkIdentity("dev-1", LinkCheck.Inconclusive))
    }

    @Test
    fun unlinkedIsATransition() {
        assertTrue(changesLinkIdentity("dev-1", LinkCheck.Unlinked))
    }

    @Test
    fun aDifferentDeviceIdentityIsATransition() {
        val check = classifyLinkCheck(Result.success(device(listOf("Ana"), deviceId = "dev-2")))
        assertTrue(changesLinkIdentity("dev-1", check))
    }
}
