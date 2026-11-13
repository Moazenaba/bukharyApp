package com.example.ui.simple

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.motion.pressScale
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSoft
import com.example.ui.theme.Primary700
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tEdge
import com.example.ui.theme.tFg

/** The meaning of a small colored label. Color is never the only signal: every label has words. */
enum class Tone { NEUTRAL, INFO, WARN, DANGER, SUCCESS }

@Composable
fun StatusPill(text: String, tone: Tone = Tone.NEUTRAL, modifier: Modifier = Modifier) {
    val (fill, ink, line) = when (tone) {
        Tone.NEUTRAL -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFFE2E8F0))
        Tone.INFO -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE))
        Tone.WARN -> Triple(Color(0xFFFFFBEB), Color(0xFF92400E), Color(0xFFFDE68A))
        Tone.DANGER -> Triple(Color(0xFFFFF1F2), Color(0xFF9F1239), Color(0xFFFECDD3))
        Tone.SUCCESS -> Triple(Color(0xFFF0FDF4), Color(0xFF15803D), Color(0xFFBBF7D0))
    }
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = tFg(ink),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(tBg(fill), shape)
            .border(1.dp, tEdge(line), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/**
 * The one container of the simple look. With [onClick] the whole card is one button (a soft press
 * shrink, a button role for TalkBack); without it, it only holds content.
 */
@Composable
fun SimpleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val source = remember { MutableInteractionSource() }
    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.pressScale(source)
    m = m.clip(shape).background(tBg(SurfacePure)).border(1.dp, tEdge(BorderSoft), shape)
    if (onClick != null) {
        m = m.clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            role = Role.Button,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
    }
    Column(m.padding(padding), content = content)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = tFg(TextMuted),
        modifier = modifier.padding(top = 4.dp).semantics { heading() }
    )
}

/** A small round icon holder, tinted by [tone]. */
@Composable
fun IconDisc(icon: ImageVector, tone: Tone = Tone.NEUTRAL, size: Dp = 40.dp) {
    val (fill, ink) = when (tone) {
        Tone.NEUTRAL -> Color(0xFFF1F5F9) to Color(0xFF475569)
        Tone.INFO -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        Tone.WARN -> Color(0xFFFFFBEB) to Color(0xFF92400E)
        Tone.DANGER -> Color(0xFFFFF1F2) to Color(0xFF9F1239)
        Tone.SUCCESS -> Color(0xFFF0FDF4) to Color(0xFF15803D)
    }
    Box(Modifier.size(size).clip(CircleShape).background(tBg(fill)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tFg(ink), modifier = Modifier.size(size * 0.55f))
    }
}

/** An empty screen says what is missing and, when there is something to do, offers it. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconDisc(icon, Tone.SUCCESS, size = 64.dp)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = tFg(Color(0xFF0F172A)))
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = tFg(TextMuted))
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** The page background of the simple look. */
@Composable
fun Modifier.simplePage(): Modifier = this.background(tBg(AppBackground))

/** The dark teal of the brand, used for the one focal block on a screen. */
val FocusStart = Color(0xFF003833)
val FocusEnd = Color(0xFF0F766E)
val FocusAccent = Primary700
