package com.netprotect.app.core.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ApiException(message: String, val statusCode: Int?) : Exception(message)

/** Shared low-level JSON-over-HttpURLConnection plumbing for the API clients.
 *
 * The project deliberately has no networking library dependency (Retrofit/OkHttp): the API
 * surface is small enough that java.net + org.json cover it without adding one.
 */
open class HttpJsonClient(private val baseUrl: String) {

    protected suspend fun getJson(path: String, accessToken: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = open(path, "GET", accessToken)
            try {
                readJsonBody(connection)
            } finally {
                connection.disconnect()
            }
        }

    protected suspend fun sendJson(
        path: String,
        method: String,
        body: JSONObject,
        accessToken: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = open(path, method, accessToken)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            readJsonBody(connection)
        } finally {
            connection.disconnect()
        }
    }

    protected suspend fun deleteForJson(path: String, accessToken: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = open(path, "DELETE", accessToken)
            try {
                readJsonBody(connection)
            } finally {
                connection.disconnect()
            }
        }

    private fun open(path: String, method: String, accessToken: String?): HttpURLConnection {
        val endpoint = "${baseUrl.trimEnd('/')}$path"
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            if (accessToken != null) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }
    }

    private fun readJsonBody(connection: HttpURLConnection): JSONObject {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (statusCode !in 200..299) {
            val detail = responseBody.takeIf { it.isNotBlank() }?.let {
                runCatching { JSONObject(it).optString("detail") }.getOrNull()
            }
            throw ApiException(detail?.takeIf { it.isNotBlank() } ?: "HTTP $statusCode", statusCode)
        }

        return if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
    }
}
