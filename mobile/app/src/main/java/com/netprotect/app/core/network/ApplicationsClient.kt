package com.netprotect.app.core.network

import com.netprotect.app.core.inventory.AppUsageInfo
import com.netprotect.app.core.inventory.InstalledAppInfo
import org.json.JSONArray
import org.json.JSONObject

data class SyncApplicationsResult(
    val appsSynced: Int,
    val appsMarkedUninstalled: Int,
    val usageRowsSynced: Int,
)

class ApplicationsClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun syncApplications(
        accessToken: String,
        deviceId: String,
        usageDate: String,
        installedApps: List<InstalledAppInfo>,
        dailyUsage: List<AppUsageInfo>,
    ): SyncApplicationsResult {
        val installedAppsJson = JSONArray()
        installedApps.forEach { app ->
            installedAppsJson.put(
                JSONObject()
                    .put("package_name", app.packageName)
                    .put("app_label", app.appLabel)
                    .put("is_system_app", app.isSystemApp)
            )
        }
        val dailyUsageJson = JSONArray()
        dailyUsage.forEach { usage ->
            dailyUsageJson.put(
                JSONObject()
                    .put("package_name", usage.packageName)
                    .put("foreground_seconds", usage.foregroundSeconds)
            )
        }
        val body = JSONObject()
            .put("usage_date", usageDate)
            .put("installed_apps", installedAppsJson)
            .put("daily_usage", dailyUsageJson)

        val payload = sendJson(
            "/api/v1/devices/$deviceId/applications/sync", "POST", body, accessToken
        )
        return SyncApplicationsResult(
            appsSynced = payload.getInt("apps_synced"),
            appsMarkedUninstalled = payload.getInt("apps_marked_uninstalled"),
            usageRowsSynced = payload.getInt("usage_rows_synced"),
        )
    }

}
