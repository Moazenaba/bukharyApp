package com.example.ui.v2

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.UiMessages

// The few building blocks every V2 screen is made of. One look for each job, so the app reads the same everywhere.

/** What a small label says about the thing it sits on; it always has words, colour is never the only signal. */
enum class V2Tone { GO, WAIT, LATE, WATCH, DONE }

@Composable
fun V2Tag(text: String, tone: V2Tone) {
    val p = LocalV2.current
    val (fill, ink) = when (tone) {
        V2Tone.GO -> p.leaf.copy(alpha = 0.15f) to p.leaf
        V2Tone.DONE -> p.leaf.copy(alpha = 0.15f) to p.leaf
        V2Tone.WAIT -> p.paper to p.muted
        V2Tone.LATE -> p.brick.copy(alpha = 0.14f) to p.brick
        V2Tone.WATCH -> p.saffron.copy(alpha = 0.24f) to p.ink
    }
    Text(
        text, style = V2Type.smallStrong, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fill).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** A section name above a group of rows. */
@Composable
fun V2Section(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = V2Type.smallStrong, color = LocalV2.current.muted,
        modifier = modifier.padding(start = 4.dp, top = 6.dp).semantics { heading() }
    )
}

/** Rows that belong together sit in one sheet with hairlines between them (not a card per row). */
@Composable
fun V2Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalV2.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.sheet).border(1.dp, p.mist, RoundedCornerShape(18.dp)),
        content = content
    )
}

@Composable
fun V2Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalV2.current.mist))
}

/** The committee number (or an icon) as the row's mark on the start side. */
@Composable
fun V2Mark(text: String? = null, icon: ImageVector? = null, late: Boolean = false, size: Dp = 44.dp) {
    val p = LocalV2.current
    Box(
        Modifier.size(size).clip(RoundedCornerShape(13.dp)).background(if (late) p.brick.copy(alpha = 0.14f) else p.paper),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, null, tint = if (late) p.brick else p.ink, modifier = Modifier.size(22.dp))
        else Text(text.orEmpty(), style = V2Type.number, color = if (late) p.brick else p.ink, maxLines = 1)
    }
}

/** One line in a group: mark, title and a short line under it, an optional tag, and what tapping it does. */
@Composable
fun V2Row(
    title: String,
    subtitle: String? = null,
    mark: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    under: @Composable (() -> Unit)? = null
) {
    val p = LocalV2.current
    var m = Modifier.fillMaxWidth().heightIn(min = 62.dp)
    if (onClick != null) m = m.clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
    Row(m.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        mark?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = V2Type.bodyStrong, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = V2Type.small, color = p.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            under?.invoke()
        }
        trailing?.invoke(this)
    }
}

/** The one strong block on a screen: what to do now. */
@Composable
fun V2NowCard(eyebrow: String, title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) {
    val p = LocalV2.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.saffron).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(eyebrow, style = V2Type.smallStrong, color = p.onSaffron.copy(alpha = 0.85f))
        Text(title, style = V2Type.hero, color = p.onSaffron)
        content()
    }
}

/** Buttons: solid (the action), outline (the second choice), quiet (on a sheet). Always at least 48dp tall. */
enum class V2ButtonKind { SOLID, OUTLINE, QUIET }

@Composable
fun V2Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: V2ButtonKind = V2ButtonKind.SOLID,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color? = null
) {
    val p = LocalV2.current
    val shape = RoundedCornerShape(14.dp)
    val (bg, fg) = when (kind) {
        V2ButtonKind.SOLID -> p.ink to p.paper
        V2ButtonKind.OUTLINE -> Color.Transparent to (contentColor ?: p.ink)
        V2ButtonKind.QUIET -> p.sheet to p.ink
    }
    var m = modifier.heightIn(min = 48.dp).clip(shape).background(if (enabled) bg else bg.copy(alpha = 0.4f))
    m = when (kind) {
        V2ButtonKind.OUTLINE -> m.border(1.5.dp, fg, shape)
        V2ButtonKind.QUIET -> m.border(1.dp, p.mist, shape)
        else -> m
    }
    Row(
        m.clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = V2Type.bodyStrong, color = if (enabled) fg else fg.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Nine small bars: done stages, the current one, and those still ahead. */
@Composable
fun V2Steps(done: Int, total: Int = 9, modifier: Modifier = Modifier) {
    val p = LocalV2.current
    Row(modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            val c = when {
                i < done -> p.leaf
                i == done -> p.saffron
                else -> p.mist
            }
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c))
        }
    }
}

/** A person's initial in a circle. */
@Composable
fun V2Avatar(name: String) {
    val p = LocalV2.current
    Box(Modifier.size(40.dp).clip(CircleShape).background(p.mist), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.toString() ?: "؟", style = V2Type.bodyStrong, color = p.ink)
    }
}

/** An empty list says what is missing and what will make it appear. */
@Composable
fun V2Empty(title: String, body: String) {
    val p = LocalV2.current
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = V2Type.heading, color = p.ink)
        Text(body, style = V2Type.body, color = p.muted)
    }
}

/** A small tappable pill used to filter a list. */
@Composable
fun V2Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalV2.current
    val shape = RoundedCornerShape(50)
    Text(
        text, style = V2Type.smallStrong, color = if (selected) p.paper else p.ink,
        modifier = Modifier.heightIn(min = 40.dp).clip(shape).background(if (selected) p.ink else p.sheet)
            .border(1.dp, if (selected) p.ink else p.mist, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

/** Phone actions: each one says so when the phone has no app for it. */
object V2Intents {
    private fun start(context: Context, intent: Intent, missing: String) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            UiMessages.show(missing)
        }
    }

    fun call(context: Context, phone: String) =
        start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")), "لا يوجد تطبيق اتصال على هذا الجهاز")

    fun open(context: Context, url: String) =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "لا يوجد تطبيق يفتح هذا الرابط")
}
