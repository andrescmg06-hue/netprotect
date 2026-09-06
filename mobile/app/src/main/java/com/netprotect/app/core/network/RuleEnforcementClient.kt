package com.netprotect.app.core.network

import com.netprotect.app.core.rules.AppRule
import com.netprotect.app.core.rules.RuleType
import java.time.Instant
import org.json.JSONObject

/** Only what the supervised device itself needs: fetching the rules to evaluate locally, and
 * reporting a block once it enforces one. Tutor-side rule management (create/list/delete) is a
 * separate client — this device never calls those endpoints.
 */
class RuleEnforcementClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun getActiveRules(accessToken: String, deviceId: String): List<AppRule> {
        val payload = getJson("/api/v1/devices/$deviceId/rules/active", accessToken)
        val rules = payload.getJSONArray("rules")
        return (0 until rules.length()).mapNotNull { index ->
            val rule = rules.getJSONObject(index)
            val type = RuleType.fromWire(rule.getString("rule_type")) ?: return@mapNotNull null
            AppRule(
                packageName = rule.getString("package_name"),
                ruleType = type,
                dailyLimitMinutes = rule.intOrNull("daily_limit_minutes"),
                scheduleStartMinute = rule.intOrNull("schedule_start_minute"),
                scheduleEndMinute = rule.intOrNull("schedule_end_minute"),
                scheduleDaysMask = rule.intOrNull("schedule_days_mask"),
            )
        }
    }

    suspend fun reportRuleEvent(
        accessToken: String,
        deviceId: String,
        packageName: String,
        ruleTypeApplied: RuleType,
        occurredAt: Instant,
    ) {
        val body = JSONObject()
            .put("package_name", packageName)
            .put("rule_type_applied", ruleTypeApplied.wireValue)
            .put("occurred_at", occurredAt.toString())
        sendJson("/api/v1/devices/$deviceId/rule-events", "POST", body, accessToken)
    }
}

/** org.json's optInt(name, fallback) can't distinguish "absent" from "explicitly 0" without a
 * sentinel; the backend always sends these fields, explicitly null when not applicable, so
 * isNull() is the correct check here.
 */
private fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else getInt(name)
