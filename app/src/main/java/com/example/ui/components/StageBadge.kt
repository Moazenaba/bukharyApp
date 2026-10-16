package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StageColorMap

@Composable
fun StageBadge(
    stageKey: String,
    completed: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showActiveLabel: Boolean = false
) {
    val stageSet = StageColorMap[stageKey] ?: return

    val bg = if (completed) stageSet.bg else Color(0xFFF8FAFC)
    val text = if (completed) stageSet.text else Color(0xFF94A3B8)
    val border = if (completed) stageSet.border else Color(0xFFE2E8F0)
    val dot = if (completed) stageSet.dot else Color(0xFFCBD5E1)

    val clickableModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(9999.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.clip(RoundedCornerShape(9999.dp))
    }

    Box(
        modifier = clickableModifier
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(9999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("stage_badge_${stageKey}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stageSet.emoji,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (showActiveLabel && completed) stageSet.activeLabel else stageSet.name,
                style = MaterialTheme.typography.labelSmall,
                color = text,
                fontWeight = if (completed) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
