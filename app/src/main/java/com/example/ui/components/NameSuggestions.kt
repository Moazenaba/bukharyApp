package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Names that may still be picked: the list without the ones already chosen (trimmed, any order). */
fun remainingNames(all: List<String>, chosen: List<String>): List<String> {
    val taken = chosen.map { it.trim() }.toSet()
    return all.filter { it.trim().isNotEmpty() && it.trim() !in taken }
}

/**
 * One tap adds a name from the website's «إعدادات النظام» lists (observers, ceremony sheikhs),
 * like the pick lists on the web. Nothing is shown when the list is empty.
 */
@Composable
fun NameSuggestions(title: String, names: List<String>, chosen: List<String>, onPick: (String) -> Unit) {
    val left = remainingNames(names, chosen)
    if (left.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            left.forEach { n -> SuggestionChip(onClick = { onPick(n) }, label = { Text("+ $n") }) }
        }
    }
}
