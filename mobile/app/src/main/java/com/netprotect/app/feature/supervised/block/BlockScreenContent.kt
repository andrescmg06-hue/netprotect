package com.netprotect.app.feature.supervised.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.ui.components.AppIcon
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Pantalla «App bloqueada» del modo supervisado. Se dibuja como actividad y como overlay del
 * sistema; solo muestra presentación y usa un relleno fijo arriba. */
@Composable
fun BlockScreenContent(
    packageName: String,
    appLabel: String,
    categoryLabel: String?,
    reason: BlockReason,
    onGoHome: () -> Unit,
) {
    val presentation = blockPresentation(reason, categoryLabel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NpColors.SkyGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(24.dp))
            Box(modifier = Modifier.size(width = 120.dp, height = 96.dp)) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.TopStart)
                        .background(presentation.tone.wash, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(NpIcons.Lock),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = NpColors.ShieldNavy,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .background(NpColors.PaperWhite, CircleShape)
                        .border(2.dp, presentation.tone.wash, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(presentation.badgeIcon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = presentation.tone.text,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = presentation.title,
                style = NpText.Display,
                color = NpColors.ShieldNavy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = presentation.message,
                style = NpText.Body,
                color = NpColors.SlateMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            NpCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(packageName = packageName, label = appLabel, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appLabel,
                            style = NpText.Title,
                            color = NpColors.ShieldNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (categoryLabel != null) {
                            Text(text = categoryLabel, style = NpText.Body, color = NpColors.SlateMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NpCard(containerColor = presentation.tone.wash, borderColor = presentation.tone.wash) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(presentation.hintIcon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = presentation.tone.text,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = presentation.hint,
                        style = NpText.Body,
                        color = NpColors.Ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            NpButton(
                text = "Ir al inicio",
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth(),
                icon = NpIcons.Home,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = BLOCK_COVER_NOTE,
                style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                color = NpColors.SlateMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
