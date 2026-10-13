package com.netprotect.app.feature.supervised.linked

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.status.isReportRecent
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpHeroPanel
import com.netprotect.app.ui.components.SEAL_ARC_COUNT
import com.netprotect.app.ui.components.permissionsCaption
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import java.time.Instant
import java.time.ZoneId

/**
 * Inicio del modo supervisado (lámina 03). The headline follows rule §2.3 ([homeStatus]) with the data the
 * screen already receives; "vista remota en curso" is the shell's [com.netprotect.app.ui.components.ActiveShareBanner]
 * above this screen, not a state of it. Read-only: it only calls the callbacks.
 */
@Composable
fun LinkedDeviceScreen(
    deviceName: String?,
    androidVersion: String,
    tutors: List<String>,
    lastContact: Instant?,
    now: Instant,
    pendingPermissions: Int,
    onOpenPermissions: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenMoreSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = homeStatus(pendingPermissions, lastContact, now)
    val reportRecent = isReportRecent(lastContact, now)
    val readyCount = (SEAL_ARC_COUNT - pendingPermissions).coerceIn(0, SEAL_ARC_COUNT)
    val actionLabel = homeStatusAction(status)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream)
            .verticalScroll(rememberScrollState()),
    ) {
        // Navy behind the status bar too; when the share banner is up the shell consumes this inset.
        Box(Modifier.background(NpColors.Navy).statusBarsPadding()) {
            NpHeroPanel(
                eyebrow = "Dispositivo supervisado",
                headline = homeStatusTitle(status),
                sealStates = homeSealStates(status, readyCount),
                sealCaption = permissionsCaption(readyCount),
                summary = homeStatusDescription(status),
                action = if (actionLabel != null) {
                    {
                        NpButton(
                            text = actionLabel,
                            onClick = onOpenPermissions,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = NpIcons.ArrowRight,
                        )
                    }
                } else {
                    null
                },
            )
        }
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = NpSpace.Gutter),
        ) {
            SideBySideOrStacked(
                first = {
                    InfoCell(label = "Dispositivo") {
                        Text(text = deviceName ?: "Este dispositivo", style = NpText.Subtitle, color = NpColors.Ink)
                        Text(text = "Android $androidVersion", style = NpText.Body, color = NpColors.InkMuted)
                    }
                },
                second = {
                    InfoCell(label = "Supervisado por") {
                        tutors.forEach { tutor ->
                            Text(text = tutor, style = NpText.Subtitle, color = NpColors.Ink)
                        }
                        DotLabel(text = "Vinculado", dot = NpColors.Success, color = NpColors.InkMuted)
                    }
                },
            )
            Hairline()
            LastContactRow(lastContact = lastContact, now = now, reportRecent = reportRecent)
            Hairline()
            Spacer(Modifier.height(NpSpace.Xl))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NpColors.Ink),
            )
            SideBySideOrStacked(
                first = {
                    ShortcutTile(
                        icon = NpIcons.Lock,
                        title = "Permisos",
                        onClick = onOpenPermissions,
                    ) {
                        val pendingTone = pendingPermissions > 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(if (pendingTone) NpIcons.CircleAlert else NpIcons.Check),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (pendingTone) NpColors.WarningText else NpColors.SuccessText,
                            )
                            Spacer(Modifier.width(NpSpace.S))
                            Text(
                                text = permissionsTileLabel(pendingPermissions),
                                style = NpText.Body,
                                color = if (pendingTone) NpColors.WarningText else NpColors.SuccessText,
                            )
                        }
                    }
                },
                second = {
                    ShortcutTile(
                        icon = if (reportRecent) NpIcons.Wifi else NpIcons.WifiOff,
                        title = "Estado de NetProtect",
                        onClick = onOpenServices,
                    ) {
                        Text(text = "Funciones activas", style = NpText.Body, color = NpColors.InkMuted)
                    }
                },
            )
            Spacer(Modifier.height(NpSpace.Xl))
            Hairline()
            // «Privacidad» joins this list with its screen (S66).
            NavRow(icon = NpIcons.SlidersHorizontal, title = "Más ajustes", onClick = onOpenMoreSettings)
            Hairline()
            Spacer(Modifier.height(NpSpace.Xl))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LastContactRow(lastContact: Instant?, now: Instant, reportRecent: Boolean) {
    // FlowRow: at a large font the status drops under the time instead of squeezing it.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NpSpace.L),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(NpSpace.S),
    ) {
        Column(Modifier.align(Alignment.CenterVertically)) {
            Text(text = "Última comunicación", style = NpText.Body, color = NpColors.InkMuted)
            Text(
                text = lastContact
                    ?.let { RelativeTime.format(it, now, ZoneId.systemDefault()).replaceFirstChar { c -> c.uppercase() } }
                    ?: "Todavía no",
                style = NpText.Subtitle,
                color = NpColors.Ink,
            )
        }
        DotLabel(
            text = contactStatusLabel(reportRecent),
            dot = if (reportRecent) NpColors.Success else NpColors.InkSubtle,
            color = if (reportRecent) NpColors.SuccessText else NpColors.InkMuted,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun InfoCell(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = NpSpace.L)) {
        Text(text = label, style = NpText.Body, color = NpColors.InkMuted)
        Spacer(Modifier.height(NpSpace.Xs))
        content()
    }
}

@Composable
private fun DotLabel(text: String, dot: Color, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(dot, CircleShape),
        )
        Spacer(Modifier.width(NpSpace.S))
        Text(text = text, style = NpText.Body, color = color)
    }
}

/** A tile of lámina 03: icon and arrow on top, title and status line at the bottom. One button for TalkBack. */
@Composable
private fun ShortcutTile(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    status: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = NpSpace.L),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = NpColors.Ink)
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(NpIcons.ArrowRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.Ink,
            )
        }
        Column(Modifier.padding(top = NpSpace.Xl)) {
            Text(text = title, style = NpText.Subtitle, color = NpColors.Ink)
            Spacer(Modifier.height(NpSpace.Xs))
            status()
        }
    }
}

@Composable
private fun NavRow(@DrawableRes icon: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp), tint = NpColors.Ink)
        Spacer(Modifier.width(NpSpace.L))
        Text(text = title, style = NpText.Subtitle, color = NpColors.Ink, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(NpSpace.M))
        Icon(
            painter = painterResource(NpIcons.ChevronRight),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = NpColors.InkMuted,
        )
    }
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NpColors.Hairline),
    )
}

/**
 * Two halves split by a vertical hairline while the longest word of each fits in its half; otherwise
 * (large font scale, long names) one under the other with a horizontal hairline, so no word is broken
 * letter by letter. Same min-intrinsic-width test as the hero panel's headline.
 */
@Composable
private fun SideBySideOrStacked(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    val gap = NpSpace.L
    Layout(
        content = {
            Box { first() }
            Box { second() }
            Box(Modifier.fillMaxHeight().background(NpColors.Hairline))
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val lineDp = 1.dp.roundToPx()
        val half = (width - gapPx * 2 - lineDp) / 2
        val fitsBeside = half > 0 &&
            measurables[0].minIntrinsicWidth(Constraints.Infinity) <= half &&
            measurables[1].minIntrinsicWidth(Constraints.Infinity) <= half

        if (fitsBeside) {
            val a = measurables[0].measure(Constraints(minWidth = half, maxWidth = half))
            val b = measurables[1].measure(Constraints(minWidth = half, maxWidth = half))
            val height = maxOf(a.height, b.height)
            val line = measurables[2].measure(Constraints.fixed(lineDp, height))
            layout(width, height) {
                a.placeRelative(0, 0)
                line.placeRelative(half + gapPx, 0)
                b.placeRelative(half + gapPx * 2 + lineDp, 0)
            }
        } else {
            val a = measurables[0].measure(Constraints(minWidth = width, maxWidth = width))
            val b = measurables[1].measure(Constraints(minWidth = width, maxWidth = width))
            val line = measurables[2].measure(Constraints.fixed(width, lineDp))
            layout(width, a.height + lineDp + b.height) {
                a.placeRelative(0, 0)
                line.placeRelative(0, a.height)
                b.placeRelative(0, a.height + lineDp)
            }
        }
    }
}
