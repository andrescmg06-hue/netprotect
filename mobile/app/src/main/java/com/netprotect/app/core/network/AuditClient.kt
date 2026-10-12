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

/** One page of the audit log plus the total that matches, to know whether there is more. */
data class AuditPage(val entries: List<AuditLogEntry>, val total: Int)

/** Sprint 22, read-only and account-level (not per-device): the tutor's own audited actions
 * (backend/app/api/v1/endpoints/audit.py scopes it to actor_user_id == current_user.id). Sprint 47
 * adds real paging (`limit`/`offset` + `total`, like the web) for "Cargar más". No filters or
 * export here — those stay in the web panel.
 */
class AuditClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listMyAuditLog(accessToken: String, offset: Int = 0, limit: Int = PAGE_SIZE): AuditPage {
        val payload = getJson("/api/v1/users/me/audit?limit=$limit&offset=$offset", accessToken)
        val logs = payload.getJSONArray("logs")
        return AuditPage(
            entries = (0 until logs.length()).map { index ->
                val entry = logs.getJSONObject(index)
                AuditLogEntry(
                    id = entry.getString("id"),
                    action = entry.getString("action"),
                    resourceType = entry.optNullableString("resource_type"),
                    resourceId = entry.optNullableString("resource_id"),
                    createdAt = entry.getString("created_at"),
                )
            },
            total = payload.optInt("total", 0),
        )
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
