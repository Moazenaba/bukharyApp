package com.example.ui.simple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.WhatsApp
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tFg

/** «رايح فين؟»: the place, large, with a button that opens it in Maps by name. */
@Composable
fun PlaceRow(place: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("المكان", style = MaterialTheme.typography.labelMedium, color = tFg(TextMuted))
        if (place.isBlank()) {
            Text("المكان لم يُحدد بعد", style = MaterialTheme.typography.bodyLarge, color = tFg(TextMuted))
        } else {
            Text(place, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = tFg(Color(0xFF0F172A)))
            OutlinedButton(onClick = { openMaps(context, place) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.Place, null); Text("  افتح في الخرائط")
            }
        }
    }
}

/** Someone to call: a name and call / WhatsApp buttons. [emptyText] is shown when there is no person or no number. */
@Composable
fun ContactRow(label: String, name: String, phone: String, emptyText: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tFg(TextMuted))
        if (name.isBlank()) {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge, color = tFg(TextMuted))
            return@Column
        }
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = tFg(Color(0xFF0F172A)))
        if (phone.isBlank()) {
            Text("لم يُسجل له رقم", style = MaterialTheme.typography.bodyMedium, color = tFg(TextMuted))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { callPhone(context, phone) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Phone, null); Text("  اتصال")
                }
                WhatsApp.link(phone, "السلام عليكم ورحمة الله")?.let { link ->
                    OutlinedButton(onClick = { openLink(context, link) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("واتساب") }
                }
            }
        }
    }
}
