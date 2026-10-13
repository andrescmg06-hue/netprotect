package com.netprotect.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Position of a permission in the rail: configured, the one to do now, or still waiting. */
enum class RailStatus { Done, Next, Pending }

/**
 * One permission of [NpRail]. [statusLabel] overrides the chip text (defaults to [railStatusLabel]);
 * [action] is shown under the description, typically only for the [RailStatus.Next] step.
 */
class NpRailStep(
    val title: String,
    val description: String,
    val status: RailStatus,
    val statusLabel: String? = null,
    val action: (@Composable () -> Unit)? = null,
)

/** Default chip text per status, as in lámina 04. */
internal fun railStatusLabel(status: RailStatus): String = when (status) {
    RailStatus.Done -> "Configurado"
    RailStatus.Next -> "Pendiente · siguiente"
    RailStatus.Pending -> "Pendiente"
}

/** What TalkBack announces as the state of each step. */
internal fun railStateDescription(status: RailStatus): String = when (status) {
    RailStatus.Done -> "Configurado"
    RailStatus.Next -> "Siguiente paso"
    RailStatus.Pending -> "Permiso pendiente"
}

internal fun railTone(status: RailStatus): NpTone = when (status) {
    RailStatus.Done -> NpTone.Success
    RailStatus.Next, RailStatus.Pending -> NpTone.Warning
}

private val RailColumnWidth = 48.dp
private val IndicatorSize = 28.dp
private val StepSpacing = 20.dp

/**
 * Vertical permission rail (lámina 04). A fixed narrow left column draws the indicator and the connector
 * to the next step (green under a configured step, grey otherwise); the text takes the rest of the
 * width and wraps normally. The title and its chip share a [FlowRow], so the chip drops to its own line
 * instead of squeezing the title. Each step is one TalkBack node with a state description; an action
 * inside it stays its own focusable button.
 */
@Composable
fun NpRail(steps: List<NpRailStep>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            RailStepRow(step = step, isLast = index == steps.lastIndex)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RailStepRow(step: NpRailStep, isLast: Boolean) {
    val lineColor = if (step.status == RailStatus.Done) NpColors.SuccessText else NpColors.Hairline
    val stateText = railStateDescription(step.status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { stateDescription = stateText }
            .drawBehind {
                if (!isLast) {
                    val x = IndicatorSize.toPx() / 2f
                    val gap = 4.dp.toPx()
                    drawLine(
                        color = lineColor,
                        start = Offset(x, IndicatorSize.toPx() + gap),
                        end = Offset(x, size.height - gap),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
            .padding(bottom = if (isLast) 0.dp else StepSpacing),
    ) {
        Box(Modifier.width(RailColumnWidth)) {
            RailIndicator(step.status)
        }
        Column(Modifier.weight(1f)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NpSpace.S),
                verticalArrangement = Arrangement.spacedBy(NpSpace.Xs),
            ) {
                Text(
                    text = step.title,
                    style = NpText.BodyStrong,
                    color = NpColors.Ink,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                StatusPill(
                    text = step.statusLabel ?: railStatusLabel(step.status),
                    tone = railTone(step.status),
                    showDot = false,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            Spacer(Modifier.height(NpSpace.Xs))
            Text(text = step.description, style = NpText.BodySm, color = NpColors.InkMuted)
            if (step.action != null) {
                Spacer(Modifier.height(NpSpace.L))
                Box(Modifier.fillMaxWidth()) { step.action.invoke() }
            }
        }
    }
}

/** Done = filled green circle with a check; Next = amber ring with a dot; Pending = dashed grey circle. */
@Composable
private fun RailIndicator(status: RailStatus) {
    Box(Modifier.size(IndicatorSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            when (status) {
                RailStatus.Done -> drawCircle(color = NpColors.SuccessText, radius = radius)
                RailStatus.Next -> {
                    val stroke = 2.dp.toPx()
                    drawCircle(color = NpColors.WarningText, radius = radius - stroke / 2f, style = Stroke(stroke))
                    drawCircle(color = NpColors.WarningText, radius = 4.dp.toPx())
                }
                RailStatus.Pending -> {
                    val stroke = 1.75.dp.toPx()
                    drawCircle(
                        color = NpColors.InkSubtle,
                        radius = radius - stroke / 2f,
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                        ),
                    )
                }
            }
        }
        if (status == RailStatus.Done) {
            Icon(
                painter = painterResource(NpIcons.Check),
                contentDescription = null,
                tint = NpColors.Paper,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpRailPreview() {
    NetProtectTheme {
        NpRail(
            steps = listOf(
                NpRailStep("Mostrar sobre otras apps", "Necesario para mostrar la pantalla de bloqueo cuando corresponda.", RailStatus.Next),
                NpRailStep("Protección contra desinstalación", "Avisa a tu tutor si alguien intenta desinstalar NetProtect.", RailStatus.Pending),
                NpRailStep("Acceso al uso de apps", "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.", RailStatus.Done),
            ),
        )
    }
}
