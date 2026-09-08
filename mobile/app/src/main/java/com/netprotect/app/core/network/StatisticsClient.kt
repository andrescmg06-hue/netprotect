package com.netprotect.app.core.network

import org.json.JSONObject

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else getString(key)

private fun JSONObject.optNullableDouble(key: String): Double? =
    if (isNull(key)) null else getDouble(key)

data class TopAppEntry(
    val packageName: String,
    val appLabel: String?,
    val totalSeconds: Int,
)

data class CategoryTotalEntry(
    val category: String?,
    val totalSeconds: Int,
)

data class BlockCountEntry(
    val ruleTypeApplied: String,
    val count: Int,
)

data class ComplianceEntry(
    val scope: String,
    val packageName: String?,
    val category: String?,
    val dailyLimitMinutes: Int,
    val daysEvaluated: Int,
    val daysCompliant: Int,
    val complianceRate: Double?,
)

data class DeviceStatistics(
    val period: String,
    val topApps: List<TopAppEntry>,
    val categories: List<CategoryTotalEntry>,
    val blocksByReason: List<BlockCountEntry>,
    val compliance: List<ComplianceEntry>,
)

/** Sprint 16, read-only same as HistoryClient/GeofenceClient: aggregates the backend already
 * computes over data reported earlier (uso diario, categorías, bloqueos, límites) via GET
 * /devices/{id}/statistics?period=... — no local aggregation needed.
 */
class StatisticsClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun getStatistics(accessToken: String, deviceId: String, period: String): DeviceStatistics {
        val payload = getJson("/api/v1/devices/$deviceId/statistics?period=$period", accessToken)

        val topApps = payload.getJSONArray("top_apps")
        val categories = payload.getJSONArray("categories")
        val blocks = payload.getJSONArray("blocks_by_reason")
        val compliance = payload.getJSONArray("compliance")

        return DeviceStatistics(
            period = payload.getString("period"),
            topApps = (0 until topApps.length()).map { index ->
                val entry = topApps.getJSONObject(index)
                TopAppEntry(
                    packageName = entry.getString("package_name"),
                    appLabel = entry.optNullableString("app_label"),
                    totalSeconds = entry.getInt("total_seconds"),
                )
            },
            categories = (0 until categories.length()).map { index ->
                val entry = categories.getJSONObject(index)
                CategoryTotalEntry(
                    category = entry.optNullableString("category"),
                    totalSeconds = entry.getInt("total_seconds"),
                )
            },
            blocksByReason = (0 until blocks.length()).map { index ->
                val entry = blocks.getJSONObject(index)
                BlockCountEntry(
                    ruleTypeApplied = entry.getString("rule_type_applied"),
                    count = entry.getInt("count"),
                )
            },
            compliance = (0 until compliance.length()).map { index ->
                val entry = compliance.getJSONObject(index)
                ComplianceEntry(
                    scope = entry.getString("scope"),
                    packageName = entry.optNullableString("package_name"),
                    category = entry.optNullableString("category"),
                    dailyLimitMinutes = entry.getInt("daily_limit_minutes"),
                    daysEvaluated = entry.getInt("days_evaluated"),
                    daysCompliant = entry.getInt("days_compliant"),
                    complianceRate = entry.optNullableDouble("compliance_rate"),
                )
            },
        )
    }
}
