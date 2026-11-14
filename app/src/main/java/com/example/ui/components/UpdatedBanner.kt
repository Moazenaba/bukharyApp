package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.WhatsNew
import com.example.ui.motion.MotionSpec
import com.example.ui.motion.rememberReducedMotion

/**
 * «تم تحديث التطبيق»: slides in from the top once, right after the app was updated, and goes away by
 * itself (or on a tap). [visible] is true only on the first open of a new version.
 */
@Composable
fun UpdatedBanner(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(9_000)
            onDismiss()
        }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reduced) EnterTransition.None else
                slideInVertically(tween(MotionSpec.LONG, easing = MotionSpec.EmphasizedDecelerate)) { -it } +
                    fadeIn(tween(MotionSpec.MEDIUM, easing = MotionSpec.EmphasizedDecelerate)),
            exit = if (reduced) ExitTransition.None else
                slideOutVertically(tween(MotionSpec.MEDIUM, easing = MotionSpec.EmphasizedAccelerate)) { -it } +
                    fadeOut(tween(MotionSpec.SHORT))
        ) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF0F766E), RoundedCornerShape(16.dp))
                    .clickable(onClickLabel = "إغلاق") { onDismiss() }
                    .padding(14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Column {
                    Text(WhatsNew.TITLE, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
                    Text(WhatsNew.MESSAGE, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.92f))
                }
            }
        }
    }
}
