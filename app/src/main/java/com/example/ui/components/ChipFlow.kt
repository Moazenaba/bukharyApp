package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A row of chips or small buttons that wraps onto more lines instead of scrolling sideways, so every
 * choice is on screen at once. (A sideways-scrolling row hides what is off the edge and nothing
 * on the screen says it can scroll.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp,
    content: @Composable RowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // FlowRowScope is a RowScope; callers only see the stable type, so none of them needs an opt-in.
        content()
    }
}
