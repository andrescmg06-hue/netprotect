package com.netprotect.app.core.network

import org.json.JSONObject

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else getString(key)

data class AuditLogEntry(
    val id: String,
    val action: String,
    val resourceType: String?,
    val resourceId: String?,
    val createdAt: String,
)

/** Sprint 22, read-only and account-level (not per-device): the tutor's own audited actions
 * (backend/app/api/v1/endpoints/audit.py scopes it to actor_user_id == current_user.id). No
 * filters/export here — those live only in the web panel, same split already used for
 * crear/editar reglas y geocercas; Android just lists the most recent page.
 */
class AuditClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listMyAuditLog(accessToken: String): List<AuditLogEntry> {
        val payload = getJson("/api/v1/users/me/audit?limit=50&offset=0", accessToken)
        val logs = payload.getJSONArray("logs")
        return (0 until logs.length()).map { index ->
            val entry = logs.getJSONObject(index)
            AuditLogEntry(
                id = entry.getString("id"),
                action = entry.getString("action"),
                resourceType = entry.optNullableString("resource_type"),
                resourceId = entry.optNullableString("resource_id"),
                createdAt = entry.getString("created_at"),
            )
        }
    }
}
