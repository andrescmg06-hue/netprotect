package com.netprotect.app.core.network

import org.json.JSONObject

/** Null when the key is absent, JSON null or blank: Android's `optString` returns the text "null" for JSON null. */
fun JSONObject.optStringOrNull(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
