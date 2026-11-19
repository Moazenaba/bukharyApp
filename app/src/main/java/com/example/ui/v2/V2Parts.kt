package com.example.ui.v2

import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.defaultMinSize
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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

// The few building blocks every V2 screen is made of. One look for each job, so the app reads the same everywhere:
// soft white sheets on the paper, one radius family (24 sheets, 28 hero, capsules for anything you tap), tonal fills
// instead of borders, and a small spring when something is pressed.

private val SheetShape = RoundedCornerShape(24.dp)
private val HeroShape = RoundedCornerShape(28.dp)
private val Capsule = RoundedCornerShape(50)

/**
 * A sheet on the paper. Lists are flat white sheets (like a phone's settings); the few things that float — the hero,
 * the tab bar, round header buttons — get a soft low shadow in light themes ([lift]).
 */
@Composable
fun Modifier.v2Sheet(shape: Shape = SheetShape, color: Color? = null, lift: Boolean = true): Modifier {
    val p = LocalV2.current
    val shadow = if (p.dark || !lift) Modifier else Modifier.shadow(14.dp, shape, ambientColor = p.ink.copy(alpha = 0.04f), spotColor = p.ink.copy(alpha = 0.10f))
    return this.then(shadow).clip(shape).background(color ?: p.sheet)
}

/** Pressed things sink a little and spring back. */
@Composable
fun Modifier.v2Press(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.8f, stiffness = 900f), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** What a small label says about the thing it sits on; it always has words, colour is never the only signal. */
enum class V2Tone {
    /** Something for me to do («مطلوب منك»): the accent, softly. */
    ACT,
    /** Plain information («امتحان», «حفل», «فتح»). */
    GO,
    WAIT, LATE, WATCH,
    /** Finished — the only green. */
    DONE
}

@Composable
fun V2Tag(text: String, tone: V2Tone) {
    val p = LocalV2.current
    val (fill, ink) = when (tone) {
        V2Tone.ACT -> p.cta.copy(alpha = 0.12f) to p.cta
        V2Tone.GO -> p.tonal to p.ink
        V2Tone.DONE -> p.leaf.copy(alpha = 0.12f) to p.leaf
        V2Tone.WAIT -> p.tonal to p.muted
        V2Tone.LATE -> p.brick.copy(alpha = 0.12f) to p.brick
        V2Tone.WATCH -> p.tonal to p.ink
    }
    Text(
        text, style = V2Type.label, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(Capsule).background(fill).padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

/** A section name above a group of rows. */
@Composable
fun V2Section(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = V2Type.smallStrong, color = LocalV2.current.muted,
        modifier = modifier.padding(start = 8.dp, top = 10.dp).semantics { heading() }
    )
}

/** Rows that belong together sit in one soft sheet with inset hairlines between them (not a card per row). */
@Composable
fun V2Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().v2Sheet(lift = false), content = content)
}

/**
 * A long list drawn as one grouped sheet, but built one row at a time (a lazy item per row), so a few hundred
 * committees scroll smoothly on a cheap phone. [key] must be unique in the whole list screen.
 */
fun <T> LazyListScope.v2Rows(rows: List<T>, key: (T) -> Any, row: @Composable (T) -> Unit) {
    val list = rows.distinctBy(key) // a repeated key would crash the list
    items(list.size, key = { key(list[it]) }) { i ->
        val first = i == 0
        val last = i == list.lastIndex
        val top = if (first) 24.dp else 0.dp
        val bottom = if (last) 24.dp else 0.dp
        Column(
            Modifier.fillMaxWidth().padding(top = if (first) V2Gap else 0.dp)
                .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                .background(LocalV2.current.sheet)
        ) {
            if (!first) V2Divider()
            row(list[i])
        }
    }
}

/**
 * Names being loaded for a picker: «جارٍ التحميل…», and if they have not come after a few seconds, a plain
 * «تعذر التحميل» with a retry (a list that never arrives must not spin forever).
 */
@Composable
fun V2LoadingNames(onRetry: () -> Unit) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(8000); slow = true }
    if (slow) V2Retry("الأسماء", onRetry)
    else Text("جارٍ تحميل الأسماء…", style = V2Type.body, color = LocalV2.current.muted, modifier = Modifier.padding(8.dp))
}

/** A search field: a soft capsule, a «search» key on the keyboard, a clear button, and a label for TalkBack. */
@Composable
fun V2SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "مسح البحث") { onChange("") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "مسح", tint = LocalV2.current.muted)
                }
            }
        },
        placeholder = { Text(placeholder, style = V2Type.body) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = Capsule, textStyle = V2Type.body, colors = v2FieldColors(),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder }
    )
}

/** A phone number inside Arabic text keeps its digits in order (never reversed by the right-to-left run). */
fun v2Phone(phone: String): String = if (phone.isBlank()) "" else "\u2066$phone\u2069"

/** A row that says a load failed and offers to try again (never an empty list that only looks like «nothing»). */
@Composable
fun V2Retry(what: String, onRetry: () -> Unit) {
    V2Row("تعذر تحميل $what", "تأكد من الاتصال واضغط هنا للمحاولة تاني", mark = { V2Mark(icon = Icons.Default.Refresh, late = true) }, onClick = onRetry, actionLabel = "إعادة المحاولة")
}

/** The space between blocks of a screen. */
val V2Gap = 14.dp

/** A hairline between rows, inset from the start so it lines up with the text, not the mark. */
@Composable
fun V2Divider(inset: Dp = 72.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(LocalV2.current.mist))
}

/** The committee number (or an icon) as the row's mark on the start side: a soft circle. */
@Composable
fun V2Mark(text: String? = null, icon: ImageVector? = null, late: Boolean = false, size: Dp = 44.dp) {
    val p = LocalV2.current
    Box(
        // A circle at normal text size that grows into a capsule when the phone's text is very large.
        Modifier.defaultMinSize(minWidth = size, minHeight = size).clip(Capsule).background(if (late) p.brick.copy(alpha = 0.12f) else p.tonal).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, null, tint = if (late) p.brick else p.ink, modifier = Modifier.size(22.dp))
        else Text(text.orEmpty(), style = V2Type.number, color = if (late) p.brick else p.ink, maxLines = 1)
    }
}

/**
 * One line in a group: mark, title and a short line under it, an optional tag, and what tapping it does.
 * Tappable rows end with a small chevron, so it is clear they open something.
 */
@Composable
fun V2Row(
    title: String,
    subtitle: String? = null,
    mark: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    under: @Composable (() -> Unit)? = null,
    actionLabel: String = "فتح",
    /** For a choice in a list: TalkBack hears «محدد/غير محدد», and the row shows no chevron. */
    selected: Boolean? = null,
    /** With [selected]: several may be picked (a checkbox) rather than one (a radio button). */
    multiple: Boolean = false
) {
    val p = LocalV2.current
    var m = Modifier.fillMaxWidth().heightIn(min = 64.dp)
    if (onClick != null) m = when {
        selected != null && multiple -> m.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
        selected != null -> m.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        else -> m.clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)
    }
    Row(m.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        mark?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = V2Type.bodyStrong, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Three lines: a place and a student count are real data, not something to cut with «…».
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = V2Type.small, color = p.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            under?.invoke()
        }
        trailing?.invoke(this)
        if (onClick != null && selected == null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.muted.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

/**
 * The one focal block on a screen: what to do now, with its time or reason under the title. A soft tint of the
 * accent; its filled accent button is the one bright spot of the screen.
 */
@Composable
fun V2NowCard(meta: String, title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) {
    val p = LocalV2.current
    Column(
        modifier.fillMaxWidth().v2Sheet(HeroShape, p.hero).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = V2Type.hero, color = p.onHero)
            if (meta.isNotBlank()) Text(meta, style = V2Type.bodyStrong, color = p.onHero.copy(alpha = 0.7f))
        }
        content()
    }
}

/**
 * Buttons, all capsules at least 48dp tall:
 * CTA is the one filled accent button of a view; SOLID and OUTLINE are tonal (a whisper of ink, never a second loud
 * slab); QUIET is a plain text button for the lesser choice («اعتذار» next to «قبول»).
 */
enum class V2ButtonKind {
    CTA, SOLID, OUTLINE, QUIET,
    /** A secondary button sitting on the focal block: white on light themes, a soft lift on dark ones. */
    ON_HERO
}

@Composable
fun V2Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: V2ButtonKind = V2ButtonKind.SOLID,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color? = null,
    /** A request is on its way: the label gives way to a spinner and the button ignores taps. */
    busy: Boolean = false
) {
    val p = LocalV2.current
    val source = remember { MutableInteractionSource() }
    // A second tap within a moment is the same tap (slow phones, shaky fingers): it never runs twice.
    var lastTap by remember { mutableLongStateOf(0L) }
    val tap = {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastTap > 900) { lastTap = now; onClick() }
    }
    val (bg, fg) = when (kind) {
        V2ButtonKind.CTA -> p.cta to p.onCta
        V2ButtonKind.SOLID -> p.tonal to p.ink
        V2ButtonKind.OUTLINE -> (contentColor ?: p.ink).copy(alpha = if (p.dark) 0.10f else 0.07f) to (contentColor ?: p.ink)
        V2ButtonKind.QUIET -> Color.Transparent to p.ink
        V2ButtonKind.ON_HERO -> (if (p.dark) p.onHero.copy(alpha = 0.12f) else p.sheet) to p.onHero
    }
    Row(
        modifier.heightIn(min = 52.dp).v2Press(source).semantics { if (busy) stateDescription = "جارٍ التنفيذ" }.clip(Capsule).background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.5f))
            .clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled && !busy, role = Role.Button, onClick = tap)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) fg else fg.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
        else
        // Two lines when the phone's text is set very large, rather than cutting the label off.
        Text(text, style = V2Type.bodyStrong, color = if (enabled) fg else fg.copy(alpha = 0.5f), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Nine thin capsules: done stages, the current one in the accent, and those still ahead. */
@Composable
fun V2Steps(done: Int, total: Int = 9, modifier: Modifier = Modifier) {
    val p = LocalV2.current
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp).semantics { contentDescription = "المرحلة ${minOf(done + 1, total)} من $total" },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(total) { i ->
            val c = when {
                i < done -> p.leaf
                i == done -> p.ink
                else -> p.muted.copy(alpha = 0.3f)
            }
            // The current stage is also taller, so it doesn't rely on colour alone.
            Box(Modifier.weight(1f).height(if (i == done) 8.dp else 5.dp).clip(Capsule).background(c).align(Alignment.CenterVertically))
        }
    }
}

/** The letter that stands for a name, skipping titles and «ال» («الشيخ محمود» → «م»). */
fun v2Initial(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val titles = setOf("الشيخ", "شيخ", "الأستاذ", "أستاذ", "د.", "دكتور", "الدكتور", "م.")
    val word = words.firstOrNull { it !in titles } ?: words.firstOrNull() ?: return "؟"
    val bare = if (word.startsWith("ال") && word.length > 3) word.drop(2) else word
    return bare.first().toString()
}

/** A person's initial in a soft circle. */
@Composable
fun V2Avatar(name: String) {
    val p = LocalV2.current
    Box(Modifier.size(44.dp).clip(CircleShape).background(p.tonal), contentAlignment = Alignment.Center) {
        Text(v2Initial(name), style = V2Type.bodyStrong, color = p.ink)
    }
}

/** An empty list says what is missing and what will make it appear. */
@Composable
fun V2Empty(title: String, body: String) {
    val p = LocalV2.current
    Column(
        Modifier.fillMaxWidth().v2Sheet(lift = false).padding(vertical = 32.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = V2Type.heading, color = p.ink, textAlign = TextAlign.Center)
        Text(body, style = V2Type.body, color = p.muted, textAlign = TextAlign.Center)
    }
}

/** A small capsule used to filter a list. */
@Composable
fun V2Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalV2.current
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier.heightIn(min = 48.dp).v2Press(source)
            .selectable(selected = selected, interactionSource = source, indication = LocalIndication.current, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, style = V2Type.smallStrong, color = if (selected) p.paper else p.ink,
            modifier = Modifier.clip(Capsule).background(if (selected) p.ink else p.tonal).padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

/** Search and text fields: a soft filled capsule, no grey frame; the accent outlines it while typing. */
@Composable
fun v2FieldColors() = LocalV2.current.let { p ->
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = p.sheet, unfocusedContainerColor = p.sheet,
        focusedBorderColor = p.cta, unfocusedBorderColor = p.muted.copy(alpha = 0.6f),
        focusedLeadingIconColor = p.ink, unfocusedLeadingIconColor = p.muted, cursorColor = p.cta
    )
}

/** Arabic words with the same 0-9 digits the rest of the app uses (not ١٢٣). */
val V2ArabicLatnDigits: java.util.Locale = java.util.Locale.forLanguageTag("ar-EG-u-nu-latn")

/** «الخميس 19 نوفمبر» for the header: the day the person is working in, instead of repeating their name. */
fun v2TodayText(today: String): String {
    val d = runCatching { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(today.take(10)) }.getOrNull() ?: return ""
    return java.text.SimpleDateFormat("EEEE d MMMM", V2ArabicLatnDigits).format(d)
}

/** [today] («yyyy-MM-dd») plus [days], in the same form. */
fun v2PlusDays(today: String, days: Int): String {
    val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }
    val d = runCatching { f.parse(today.take(10)) }.getOrNull() ?: return ""
    val cal = java.util.Calendar.getInstance().apply { time = d; add(java.util.Calendar.DAY_OF_MONTH, days) }
    return f.format(cal.time)
}

/**
 * An Arabic count: «إشعار جديد» for one, «إشعارين» for two, «3 إشعارات» for 3–10, «11 إشعار» beyond.
 * [one] and [two] are whole phrases (the number is in the word); [few] and [many] get the number in front.
 */
fun v2Count(n: Int, one: String, two: String, few: String, many: String): String = when {
    n == 1 -> one
    n == 2 -> two
    n in 3..10 -> "$n $few"
    else -> "$n $many"
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

/**
 * «تُتاح الأحد 20 نوفمبر، 10:00 ص (بتوقيت القاهرة)»: the same wording and time zone as the classic exams screen,
 * written as a date (never «أمس»), with SimpleDateFormat so it works on Android 7 too.
 */
fun v2OpensText(iso: String): String {
    val date = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX", "yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX").firstNotNullOfOrNull { f ->
        runCatching { java.text.SimpleDateFormat(f, java.util.Locale.US).apply { isLenient = false }.parse(iso) }.getOrNull()
    } ?: return iso.take(16).replace('T', ' ')
    val text = java.text.SimpleDateFormat("EEEE d MMMM، h:mm a", V2ArabicLatnDigits)
        .apply { timeZone = java.util.TimeZone.getTimeZone("Africa/Cairo") }.format(date)
    return "$text (بتوقيت القاهرة)"
}

/** Three dots: the theme's paper, ink and action colour, so a theme can be picked by eye. */
@Composable
fun V2Swatch(spec: com.example.domain.v2.V2ThemeSpec) {
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        listOf(spec.paper, spec.ink, spec.cta).forEach { c ->
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color(c)).border(1.dp, LocalV2.current.muted.copy(alpha = 0.5f), CircleShape))
        }
    }
}
