package com.netprotect.app.core.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class InfrastructureHealth(
    val backend: String,
    val database: String,
    val redis: String,
)

class InfrastructureHealthClient(
    private val baseUrl: String,
) {
    suspend fun check(): InfrastructureHealth = withContext(Dispatchers.IO) {
        val endpoint = "${baseUrl.trimEnd('/')}/api/v1/health/ready"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                error("La API respondió HTTP $statusCode")
            }

            val payload = JSONObject(responseBody)
            if (payload.optString("status") != "ready") {
                error("La infraestructura no está lista")
            }

            InfrastructureHealth(
                backend = payload.getString("backend"),
                database = payload.getString("database"),
                redis = payload.getString("redis"),
            )
        } finally {
            connection.disconnect()
        }
    }
}

/** Sprint 63: what the login says when [InfrastructureHealthClient.check] fails. An I/O failure is
 * "no Internet" or "service down" depending on the phone's network; anything else (non-2xx, not
 * ready, malformed answer) means the service answered but is not usable. */
fun Throwable.toHealthUiError(online: () -> Boolean = NetworkState::isOnline): UiError =
    if (this is IOException) classifyIoFailure(online()) else UiError.ServiceUnavailable
