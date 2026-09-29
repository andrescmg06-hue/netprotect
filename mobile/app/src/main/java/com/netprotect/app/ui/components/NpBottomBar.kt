package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpElevation
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

data class NpBottomItem(
    val label: String,
    @param:DrawableRes val icon: Int,
    val badgeCount: Int = 0,
)

/** Barra inferior de navegación. El elegido en `SignalBlueText` con una barra corta de 3 dp en
 * `SignalBlue` debajo; los demás `SlateMuted`. Cada ítem ≥ 48 dp, la barra ≥ 64 dp. */
@Composable
fun NpBottomBar(
    items: List<NpBottomItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A rounded white card with the card shadow, as in mockup 03; whoever places it decides
    // whether it floats (outer padding) or sits at the edge.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = NpShapes.Xl,
        color = NpColors.PaperWhite,
        border = BorderStroke(1.dp, NpColors.Hairline),
        shadowElevation = NpElevation.Card,
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val color = if (selected) NpColors.SignalBlueText else NpColors.SlateMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = color,
                    )
                    if (item.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(NpColors.Danger, CircleShape)
                                .semantics { contentDescription = "${item.badgeCount} sin leer" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${item.badgeCount}",
                                style = NpText.Caption,
                                color = NpColors.PaperWhite,
                            )
                        }
                    }
                }
                Text(text = item.label, style = NpText.Caption, color = color)
                if (selected) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .background(NpColors.SignalBlue, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun NpBottomBarPreview() {
    NetProtectTheme {
        NpBottomBar(
            items = listOf(
                NpBottomItem("Inicio", NpIcons.Home),
                NpBottomItem("Dispositivos", NpIcons.Smartphone, badgeCount = 3),
                NpBottomItem("Actividad", NpIcons.Clock),
                NpBottomItem("Más", NpIcons.MoreHorizontal),
            ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
