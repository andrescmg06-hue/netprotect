package com.netprotect.app.core.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented on purpose: desktop org.json (JVM tests) returns "" for JSON null, Android returns "null".
@RunWith(AndroidJUnit4::class)
class JsonExtTest {

    @Test
    fun androidOptStringReturnsTheTextNullForJsonNull() {
        assertEquals("null", JSONObject("""{"a": null}""").optString("a"))
    }

    @Test
    fun absentKeyIsNull() {
        assertNull(JSONObject("{}").optStringOrNull("a"))
    }

    @Test
    fun explicitJsonNullIsNull() {
        assertNull(JSONObject("""{"a": null}""").optStringOrNull("a"))
    }

    @Test
    fun blankValueIsNull() {
        assertNull(JSONObject("""{"a": "  "}""").optStringOrNull("a"))
    }

    @Test
    fun realValueIsReturned() {
        assertEquals("2026-01-01", JSONObject("""{"a": "2026-01-01"}""").optStringOrNull("a"))
    }
}
