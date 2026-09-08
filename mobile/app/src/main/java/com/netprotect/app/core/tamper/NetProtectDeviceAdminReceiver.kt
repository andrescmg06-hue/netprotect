package com.netprotect.app.core.tamper

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Sprint 20: the only officially supported way for an app that is NOT a device owner to notice
 * an uninstall attempt.
 *
 * Android refuses to uninstall an app while it is an active Device Administrator: the user has to
 * deactivate it first (Ajustes → Seguridad → Apps de administración del dispositivo), and that
 * deactivation is what fires [onDisableRequested] here. That callback cannot block or veto
 * anything — it returns a warning string the system shows in its own confirmation dialog, and the
 * user is free to confirm. This project uses it for exactly what the plan asks for ("se registra
 * el evento y se alerta al tutor"): report the attempt, warn plainly, and let the user proceed.
 * No hiding, no evasion, no attempt to make the app unremovable.
 *
 * Declared with an empty <uses-policies> (res/xml/device_admin.xml): this admin enforces no
 * password/wipe/camera policy at all — the registration exists solely for the uninstall-attempt
 * signal, so requesting any policy the app never uses would be asking the user for more power
 * than it needs.
 */
class NetProtectDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        TamperReportWorker.reportUninstallAttempt(context)
        return "Si desactivas la administración, tu tutor recibirá una alerta y NetProtect " +
            "dejará de proteger este dispositivo."
    }
}
