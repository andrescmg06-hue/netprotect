package com.netprotect.app.core.network

import com.netprotect.app.core.rules.AppRule
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.core.rules.Category
import com.netprotect.app.core.rules.CategoryAssignment
import com.netprotect.app.core.rules.CategoryRule
import com.netprotect.app.core.rules.DefaultAppPolicy
import com.netprotect.app.core.rules.RuleType
import com.netprotect.app.core.rules.SchoolMode
import java.time.Instant
import org.json.JSONObject

/** Everything the device needs to evaluate locally in one call: per-app rules, each app's
 * category assignment and each category's rule (Sprint 10), the default-policy fallback, and
 * the school-mode window (Sprint 12) — bundled together so the pieces can't drift apart between
 * two separate fetches.
 */
data class ActiveRules(
    val rules: List<AppRule>,
    val categoryAssignments: List<CategoryAssignment>,
    val categoryRules: List<CategoryRule>,
    val defaultPolicy: DefaultAppPolicy,
    val schoolMode: SchoolMode,
)

/** Only what the supervised device itself needs: fetching the rules to evaluate locally, and
 * reporting a block once it enforces one. Tutor-side rule management (create/list/delete) is a
 * separate client — this device never calls those endpoints.
 */
class RuleEnforcementClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun getActiveRules(accessToken: String, deviceId: String): ActiveRules {
        val payload = getJson("/api/v1/devices/$deviceId/rules/active", accessToken)
        val rules = payload.getJSONArray("rules")
        val parsed = (0 until rules.length()).mapNotNull { index ->
            val rule = rules.getJSONObject(index)
            val type = RuleType.fromWire(rule.getString("rule_type")) ?: return@mapNotNull null
            AppRule(
                packageName = rule.getString("package_name"),
                ruleType = type,
                dailyLimitMinutes = rule.intOrNull("daily_limit_minutes"),
                weeklyLimitMinutes = rule.intOrNull("weekly_limit_minutes"),
                scheduleStartMinute = rule.intOrNull("schedule_start_minute"),
                scheduleEndMinute = rule.intOrNull("schedule_end_minute"),
                scheduleDaysMask = rule.intOrNull("schedule_days_mask"),
            )
        }
        // An unrecognized policy falls back to ALLOW rather than BLOCK: if a future backend
        // sends something this build doesn't know, the safe failure is to keep the device
        // usable, not to lock the user out of every app.
        val policy = DefaultAppPolicy.fromWire(payload.getString("default_app_policy"))
            ?: DefaultAppPolicy.ALLOW

        val assignmentsJson = payload.getJSONArray("category_assignments")
        val assignments = (0 until assignmentsJson.length()).mapNotNull { index ->
            val entry = assignmentsJson.getJSONObject(index)
            val category = Category.fromWire(entry.getString("category")) ?: return@mapNotNull null
            CategoryAssignment(packageName = entry.getString("package_name"), category = category)
        }

        val categoryRulesJson = payload.getJSONArray("category_rules")
        val categoryRules = (0 until categoryRulesJson.length()).mapNotNull { index ->
            val entry = categoryRulesJson.getJSONObject(index)
            val category = Category.fromWire(entry.getString("category")) ?: return@mapNotNull null
            val type = RuleType.fromWire(entry.getString("rule_type")) ?: return@mapNotNull null
            CategoryRule(
                category = category,
                ruleType = type,
                dailyLimitMinutes = entry.intOrNull("daily_limit_minutes"),
                weeklyLimitMinutes = entry.intOrNull("weekly_limit_minutes"),
                scheduleStartMinute = entry.intOrNull("schedule_start_minute"),
                scheduleEndMinute = entry.intOrNull("schedule_end_minute"),
                scheduleDaysMask = entry.intOrNull("schedule_days_mask"),
            )
        }

        val schoolModeJson = payload.getJSONObject("school_mode")
        val schoolMode = SchoolMode(
            enabled = schoolModeJson.getBoolean("enabled"),
            startMinute = schoolModeJson.intOrNull("start_minute"),
            endMinute = schoolModeJson.intOrNull("end_minute"),
            daysMask = schoolModeJson.intOrNull("days_mask"),
        )

        return ActiveRules(
            rules = parsed,
            categoryAssignments = assignments,
            categoryRules = categoryRules,
            defaultPolicy = policy,
            schoolMode = schoolMode,
        )
    }

    suspend fun reportRuleEvent(
        accessToken: String,
        deviceId: String,
        packageName: String,
        reason: BlockReason,
        occurredAt: Instant,
    ) {
        val body = JSONObject()
            .put("package_name", packageName)
            .put("rule_type_applied", reason.wireValue)
            .put("occurred_at", occurredAt.toString())
        sendJson("/api/v1/devices/$deviceId/rule-events", "POST", body, accessToken)
    }
}

/** org.json's optInt(name, fallback) can't distinguish "absent" from "explicitly 0" without a
 * sentinel; the backend always sends these fields, explicitly null when not applicable, so
 * isNull() is the correct check here.
 */
private fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else getInt(name)
