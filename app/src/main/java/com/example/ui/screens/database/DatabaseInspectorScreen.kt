package com.example.ui.screens.database

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DatabaseStatus
import com.example.ui.theme.Primary900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * This device's real sync state: when data last came from the server, which changes are
 * still waiting or failed, and local record counts. Super admins also see the server's
 * database facts. Nothing here is estimated or invented.
 */
@Composable
fun DatabaseInspectorScreen(
    databaseStatus: DatabaseStatus?,
    isSyncing: Boolean,
    isNetworkAvailable: Boolean,
    onRefresh: () -> Unit,
    onLoadStatus: () -> Unit,
    onRetryFailed: () -> Unit,
    onDiscardFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { onLoadStatus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("حالة البيانات والمزامنة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary900)

        if (databaseStatus == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                Text("جارٍ تحميل الحالة...")
            }
            return@Column
        }

        Section("الاتصال") {
            Line("الشبكة على الجهاز", if (isNetworkAvailable) "متصلة" else "غير متصلة")
            Line("آخر تحديث من الخادم", databaseStatus.lastSyncAt?.let { formatTime(it) } ?: "لم يتم في هذه الجلسة")
            Line("الحالة", databaseStatus.message)
            Line("عنوان الخادم", databaseStatus.serverUrl.ifBlank { "غير مضبوط" })
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRefresh, enabled = !isSyncing, modifier = Modifier.fillMaxWidth()) {
                Text(if (isSyncing) "جارٍ التحديث..." else "تحديث الآن")
            }
        }

        Section("التعديلات المحفوظة على الجهاز") {
            Line("بانتظار الإرسال", databaseStatus.pendingWrites.toString())
            Line("تعذر إرسالها", databaseStatus.failedWrites.size.toString())
            if (databaseStatus.failedWrites.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                databaseStatus.failedWrites.forEach { f ->
                    Text("• ${f.label}: ${f.error ?: "خطأ في الخادم"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB91C1C))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRetryFailed, modifier = Modifier.weight(1f)) { Text("إعادة المحاولة") }
                    OutlinedButton(onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f)) { Text("تجاهلها") }
                }
            }
        }

        Section("البيانات المحفوظة على الجهاز") {
            databaseStatus.localTables.forEach { Line(it.nameAr, it.count.toString()) }
        }

        databaseStatus.server?.let { server ->
            Section("قاعدة بيانات الخادم (للمشرف العام)") {
                Line("قاعدة البيانات", server.database)
                Line("الإصدار", server.postgresVersion)
                Line("زمن الاستجابة", "${server.latencyMs} ms")
                Line("تشفير الاتصال", if (server.tls == "none") "غير مشفر" else server.tls)
                Line("ترحيلات معلّقة", if (server.pendingMigrations.isEmpty()) "لا يوجد" else server.pendingMigrations.joinToString("، "))
                Spacer(Modifier.height(6.dp))
                Text("عدد الصفوف (تقديري):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                server.tables.forEach { Line(it.name, it.count.toString()) }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("تجاهل التعديلات؟") },
            text = { Text("لن تُرسل هذه التعديلات للخادم، وستُستبدل ببيانات الخادم الحالية. لا يمكن التراجع.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscardFailed()
                }) { Text("تجاهل", color = Color(0xFFB91C1C)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Primary900)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(millis: Long): String = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date(millis))
