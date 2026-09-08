package com.netprotect.app.core.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.netprotect.app.core.tamper.NetProtectDeviceAdminReceiver

/** Device Administrator registration (Sprint 20): user-granted through the system's own
 * confirmation screen (`ACTION_ADD_DEVICE_ADMIN`), not a manifest permission and not device-owner
 * provisioning. Optional — nothing in the app depends on it — but while it is active Android
 * requires deactivating it before the app can be uninstalled, which is what makes the attempt
 * detectable (see NetProtectDeviceAdminReceiver).
 */
object DeviceAdminPermission {
    fun componentName(context: Context): ComponentName =
        ComponentName(context.applicationContext, NetProtectDeviceAdminReceiver::class.java)

    fun isActive(context: Context): Boolean {
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return manager.isAdminActive(componentName(context))
    }

    fun requestIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Permite que NetProtect avise a tu tutor si alguien intenta desinstalar la app. " +
                    "No da acceso a tus datos ni permite borrar el dispositivo.",
            )
}
