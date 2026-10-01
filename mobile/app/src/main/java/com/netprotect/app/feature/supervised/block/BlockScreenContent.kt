package com.netprotect.app.feature.supervised.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.core.rules.BlockReason

/** INTERIM (Sprint 49, Claude): shared with BlockOverlayController, which renders this same content
 * as a window overlay when the overlay permission is available. Plain but complete (every reason,
 * the real category, "Ir al inicio"); DeepSeek replaces this body with the mockup 16 design and
 * keeps it in this file. The signature is final. */
@Composable
fun BlockScreenContent(
    packageName: String,
    appLabel: String,
    categoryLabel: String?,
    reason: BlockReason,
    onGoHome: () -> Unit,
) {
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
            val presentation = blockPresentation(reason, categoryLabel)
            categoryLabel?.let { Text(it, color = Color(0xFFABB5C4), fontSize = 14.sp) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(presentation.message, color = Color(0xFFABB5C4), fontSize = 16.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(presentation.hint, color = Color(0xFFFFB4AB), fontSize = 14.sp, lineHeight = 20.sp)

            Spacer(modifier = Modifier.height(28.dp))
            Surface(color = Color(0xFF121722), shape = RoundedCornerShape(16.dp)) {
                Text(
                    BLOCK_COVER_NOTE,
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
