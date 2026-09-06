package com.netprotect.app.feature.supervised

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.core.rules.RuleType

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
        const val EXTRA_RULE_TYPE = "rule_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }
        val ruleType = intent.getStringExtra(EXTRA_RULE_TYPE)?.let(RuleType::fromWire)
            ?: RuleType.BLOCK
        val appLabel = resolveAppLabel(packageName)

        setContent {
            MaterialTheme {
                BlockScreenContent(
                    appLabel = appLabel,
                    ruleType = ruleType,
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

private fun reasonText(ruleType: RuleType): String = when (ruleType) {
    RuleType.BLOCK -> "Tu tutor bloqueó esta app."
    RuleType.DAILY_LIMIT -> "Ya usaste el tiempo diario permitido para esta app."
    RuleType.SCHEDULE -> "Esta app está bloqueada en este horario."
    RuleType.ALLOW -> "" // never reached: ALLOW never triggers a block (see RuleEvaluator).
}

@Composable
private fun BlockScreenContent(appLabel: String, ruleType: RuleType, onGoHome: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B10)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "APP BLOQUEADA",
                color = Color(0xFFFFB4AB),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = appLabel,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(reasonText(ruleType), color = Color(0xFFABB5C4), fontSize = 16.sp, lineHeight = 22.sp)

            Spacer(modifier = Modifier.height(28.dp))
            Surface(color = Color(0xFF121722), shape = RoundedCornerShape(16.dp)) {
                Text(
                    "Este bloqueo cubre la pantalla, pero no puede impedir que la app siga " +
                        "abierta de fondo ni que alguien con conocimientos técnicos lo evada " +
                        "(por ejemplo, revocando el acceso a uso en Ajustes). NetProtect no " +
                        "tiene privilegios de administrador de dispositivo.",
                    color = Color(0xFF7D899A),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
            ) {
                Text("Ir al inicio")
            }
        }
    }
}
