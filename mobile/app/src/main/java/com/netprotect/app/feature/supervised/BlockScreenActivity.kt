package com.netprotect.app.feature.supervised

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.feature.supervised.block.BlockScreenContent
import com.netprotect.app.ui.theme.NetProtectTheme

/** Full-screen cover shown over a blocked app. Launched by RuleEnforcementService with
 * FLAG_ACTIVITY_NEW_TASK from outside any activity context.
 *
 * This only covers the blocked app's UI — it does not, and cannot, force-stop or otherwise
 * disable it (that needs device-owner privileges this project doesn't have). Pressing back or
 * recents can reveal the blocked app again, at which point the next poll re-detects it and this
 * screen reappears — see docs/android/capability-matrix.md (Sprint 8) for the full list of what
 * a technically capable user could still do to get around this.
 */
class BlockScreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_REASON = "reason"
        /** Sprint 49: the real assigned category's Spanish name, absent if the app has none. */
        const val EXTRA_CATEGORY_LABEL = "category_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }
        val reason = intent.getStringExtra(EXTRA_REASON)
            ?.let { wire -> BlockReason.entries.find { it.wireValue == wire } }
            ?: BlockReason.BLOCK
        val appLabel = resolveAppLabel(packageName)
        val categoryLabel = intent.getStringExtra(EXTRA_CATEGORY_LABEL)

        setContent {
            NetProtectTheme {
                BlockScreenContent(
                    packageName = packageName,
                    appLabel = appLabel,
                    categoryLabel = categoryLabel,
                    reason = reason,
                    onGoHome = {
                        startActivity(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        )
                    },
                )
            }
        }
    }

    private fun resolveAppLabel(packageName: String): String =
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
}
