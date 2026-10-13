package com.netprotect.app.feature.supervised.permissions

import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.ui.components.RailStatus

/** The four permissions of the supervised mode, in the order lámina 04 lists them when all are configured. */
enum class SupervisedPermission { UsageAccess, Location, Overlay, DeviceAdmin }

/** One row of the permissions rail: which permission and where it stands. */
data class PermissionStep(val permission: SupervisedPermission, val status: RailStatus)

/**
 * Rail order of lámina 04: pending permissions first, then the configured ones, each group in the
 * canonical order of [SupervisedPermission]. The first pending one is the [RailStatus.Next] step — the
 * only one with a button — so the child always has exactly one thing to do.
 */
fun permissionSteps(permissions: PermissionsUi): List<PermissionStep> {
    val granted = SupervisedPermission.entries.associateWith { permissions.isGranted(it) }
    val pending = SupervisedPermission.entries.filter { granted[it] == false }
    val done = SupervisedPermission.entries.filter { granted[it] == true }
    return pending.mapIndexed { index, permission ->
        PermissionStep(permission, if (index == 0) RailStatus.Next else RailStatus.Pending)
    } + done.map { PermissionStep(it, RailStatus.Done) }
}

fun PermissionsUi.isGranted(permission: SupervisedPermission): Boolean = when (permission) {
    SupervisedPermission.UsageAccess -> usageAccess
    SupervisedPermission.Location -> location
    SupervisedPermission.Overlay -> overlay
    SupervisedPermission.DeviceAdmin -> deviceAdmin
}

/** Progress line above the rail while something is pending, e.g. "2 de 4 configurados". */
fun configuredCaption(permissions: PermissionsUi): String {
    val total = SupervisedPermission.entries.size
    return "${total - permissions.pending} de $total configurados"
}
