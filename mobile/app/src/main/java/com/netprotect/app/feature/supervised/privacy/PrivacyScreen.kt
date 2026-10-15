package com.netprotect.app.feature.supervised.privacy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText

/**
 * Privacidad del modo supervisado (lámina 10.5): what NetProtect does on this phone, from [privacySections].
 * Static text only — no state from the shell — and every claim is backed by the code (see PrivacyContent.kt).
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NpSpace.Gutter),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(painter = painterResource(NpIcons.ArrowLeft), contentDescription = "Atrás", tint = NpColors.Ink)
            }
            Spacer(Modifier.weight(1f))
            Image(painter = painterResource(NpIcons.LogoShield), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(NpSpace.S))
        Text(
            text = PRIVACY_TITLE,
            style = NpText.Display,
            color = NpColors.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(NpSpace.S))
        Text(text = PRIVACY_SUBTITLE, style = NpText.Body, color = NpColors.InkMuted)
        privacySections.forEach { section ->
            Spacer(Modifier.height(NpSpace.Xl))
            Column(Modifier.semantics { heading() }) {
                Text(text = section.heading.uppercase(), style = NpText.Eyebrow, color = NpColors.InkMuted)
                Spacer(Modifier.height(NpSpace.S))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NpColors.Ink),
                )
            }
            section.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NpColors.Hairline),
                    )
                }
                // One TalkBack node per item: title and explanation are read together.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) { }
                        .padding(vertical = NpSpace.L),
                ) {
                    Text(text = item.title, style = NpText.BodyStrong, color = NpColors.Ink)
                    Spacer(Modifier.height(NpSpace.Xs))
                    Text(text = item.body, style = NpText.BodySm, color = NpColors.InkMuted)
                }
            }
        }
        Spacer(Modifier.height(NpSpace.M))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(NpIcons.Ban),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
                tint = NpColors.BlueText,
            )
            Spacer(Modifier.size(NpSpace.M))
            Text(
                text = PRIVACY_NO_RECORDING,
                style = NpText.Body,
                color = NpColors.Ink,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(NpSpace.Xl))
    }
}
