package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tEdge
import com.example.ui.theme.tFg
import com.example.update.AppUpdater
import com.example.update.UpdateStatus
import com.example.update.UpdateText
import kotlinx.coroutines.launch

/**
 * «تحديث التطبيق»: the installed version, whether there is a newer one, and if it cannot be installed,
 * why, with the button that fixes it (update now, allow installs, or download by hand).
 */
@Composable
fun UpdateRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val report by AppUpdater.report.collectAsStateWithLifecycle()
    var busy by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
        border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Text("تحديث التطبيق — النسخة الحالية ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tFg(Color(0xFF0F172A)))
            val r = report
            Text(
                if (busy) "جارٍ الفحص…" else r?.let { UpdateText.title(it) } ?: "اضغط «افحص الآن» لمعرفة لو في تحديث.",
                style = MaterialTheme.typography.bodyMedium,
                color = tFg(if (r != null && !UpdateText.actionable(r.status) && r.status != UpdateStatus.UP_TO_DATE) Color(0xFFB91C1C) else Color(0xFF334155))
            )
            if (r != null && r.detail.isNotBlank()) Text(r.detail, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
            ChipFlow(spacing = 8.dp) {
                when (r?.status) {
                    UpdateStatus.READY -> Button(
                        onClick = { scope.launch { busy = true; AppUpdater.updateNow(context); busy = false } },
                        enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("حدّث الآن", fontWeight = FontWeight.Bold) }
                    UpdateStatus.NEEDS_PERMISSION -> Button(
                        onClick = { context.startActivity(AppUpdater.permissionSettingsIntent(context)) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("افتح الإعدادات للسماح", fontWeight = FontWeight.Bold) }
                    UpdateStatus.WRONG_SIGNATURE -> Button(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r?.apkUrl.orEmpty()))) } },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("حمّل النسخة الجديدة", fontWeight = FontWeight.Bold) }
                    else -> Unit
                }
                OutlinedButton(
                    onClick = { scope.launch { busy = true; AppUpdater.check(context); busy = false } },
                    enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("افحص الآن") }
            }
        }
    }
}
