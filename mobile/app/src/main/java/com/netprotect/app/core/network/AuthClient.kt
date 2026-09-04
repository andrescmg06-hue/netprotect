package com.netprotect.app.core.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)

data class CurrentUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
)

class AuthApiException(message: String, val statusCode: Int?) : Exception(message)

class AuthClient(
    private val baseUrl: String,
) {
    suspend fun loginWithGoogle(idToken: String): TokenPair =
        postForTokens("/api/v1/auth/google", JSONObject().put("id_token", idToken))

    suspend fun refresh(refreshToken: String): TokenPair =
        postForTokens("/api/v1/auth/refresh", JSONObject().put("refresh_token", refreshToken))

    suspend fun logout(refreshToken: String): Unit = withContext(Dispatchers.IO) {
        val connection = openConnection("/api/v1/auth/logout", "POST")
        try {
            writeJsonBody(connection, JSONObject().put("refresh_token", refreshToken))
            connection.responseCode
        } finally {
            connection.disconnect()
        }
        Unit
    }

    suspend fun fetchCurrentUser(accessToken: String): CurrentUser = withContext(Dispatchers.IO) {
        val connection = openConnection("/api/v1/auth/me", "GET").apply {
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            val payload = readJsonBody(connection)
            CurrentUser(
                id = payload.getString("id"),
                email = payload.getString("email"),
                displayName = payload.optString("display_name").takeIf { it.isNotBlank() },
                avatarUrl = payload.optString("avatar_url").takeIf { it.isNotBlank() },
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun postForTokens(path: String, body: JSONObject): TokenPair =
        withContext(Dispatchers.IO) {
            val connection = openConnection(path, "POST")
            try {
                writeJsonBody(connection, body)
                val payload = readJsonBody(connection)
                TokenPair(
                    accessToken = payload.getString("access_token"),
                    refreshToken = payload.getString("refresh_token"),
                    expiresIn = payload.getInt("expires_in"),
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        val endpoint = "${baseUrl.trimEnd('/')}$path"
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun writeJsonBody(connection: HttpURLConnection, body: JSONObject) {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    }

    private fun readJsonBody(connection: HttpURLConnection): JSONObject {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (statusCode !in 200..299) {
            val detail = responseBody.takeIf { it.isNotBlank() }?.let {
                runCatching { JSONObject(it).optString("detail") }.getOrNull()
            }
            throw AuthApiException(detail ?: "HTTP $statusCode", statusCode)
        }

        return JSONObject(responseBody)
    }
}
