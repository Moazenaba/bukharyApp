package com.example.ui.motion

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min

/**
 * The app's motion, in one place. Durations and curves follow Material 3: things arrive with a long
 * deceleration and leave faster with an acceleration; presses use a soft spring. Motion explains a
 * change (where something came from, what was pressed, what finished); a view animates one or two
 * things, not everything that moves.
 */
object MotionSpec {
    const val SHORT = 120
    const val MEDIUM = 250
    const val LONG = 400

    /** Arrivals: fast start, long gentle stop. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Exits: slow start, quick finish. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    const val PRESSED_SCALE = 0.97f
    const val SPRING_DAMPING = 0.8f

    /** Siblings of a list appear one after another, but never more than [STAGGER_MAX_ITEMS] steps. */
    const val STAGGER_STEP = 30
    const val STAGGER_MAX_ITEMS = 6

    fun staggerDelay(index: Int): Int = min(index.coerceAtLeast(0), STAGGER_MAX_ITEMS - 1) * STAGGER_STEP

    /** The device's «Remove animations» setting (animator duration scale 0). */
    fun isReduced(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

    /** The duration to use: zero when the person asked for no animation. */
    fun duration(base: Int, reduced: Boolean): Int = if (reduced) 0 else base

    /** Things arriving. */
    fun <T> enter(reduced: Boolean, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(MEDIUM, delayMillis, EmphasizedDecelerate)

    /** Things leaving: faster than arriving. */
    fun <T> exit(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(SHORT, easing = EmphasizedAccelerate)

    /** For pressed states, expanding and settling. */
    fun <T> soft(): FiniteAnimationSpec<T> = spring(dampingRatio = SPRING_DAMPING, stiffness = Spring.StiffnessMediumLow)

    /** Switching between sibling screens: the old one fades out quickly, the new one fades and grows in. */
    fun fadeThrough(reduced: Boolean): ContentTransform =
        if (reduced) {
            fadeIn(snap()) togetherWith fadeOut(snap())
        } else {
            (fadeIn(tween(210, 90, EmphasizedDecelerate)) + scaleIn(tween(210, 90, EmphasizedDecelerate), initialScale = 0.92f)) togetherWith
                fadeOut(tween(90, easing = EmphasizedAccelerate))
        }
}

/** True when the device is set to remove animations. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        MotionSpec.isReduced(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}

/** Shrinks slightly while pressed, with a soft spring. Give the same [source] to `clickable`. */
fun Modifier.pressScale(source: MutableInteractionSource, enabled: Boolean = true): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) MotionSpec.PRESSED_SCALE else 1f,
        animationSpec = MotionSpec.soft(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
