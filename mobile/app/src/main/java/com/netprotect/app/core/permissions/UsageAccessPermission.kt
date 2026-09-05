package com.netprotect.app.core.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/** PACKAGE_USAGE_STATS is a special-access permission: declaring it in the manifest isn't
 * enough, and there's no runtime dialog for it. The user grants it separately in Settings, and
 * AppOpsManager is the only reliable way to ask whether it's currently on.
 */
object UsageAccessPermission {
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            /* attributionTag = */ null,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
