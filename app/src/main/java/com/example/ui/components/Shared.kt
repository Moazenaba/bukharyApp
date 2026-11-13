package com.example.ui.components

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.TextMuted
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

val ARABIC_DAYS = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

/** Arabic weekday of a YYYY-MM-DD date (web: getArabicDayName). */
fun arabicDayName(iso: String?): String {
    val millis = iso?.take(10)?.let { Dates.toMillis(it) } ?: return ""
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return ARABIC_DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

/** The next date (today or later this week) that falls on [day]. */
fun nextDateForDay(day: String): String {
    val cal = Calendar.getInstance()
    repeat(7) {
        if (ARABIC_DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1] == day) return Dates.fromMillis(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return Dates.today()
}

/** Prints (or saves as PDF) an HTML page through Android's print service, like window.print() on the web. */
fun printHtml(context: Context, jobName: String, bodyHtml: String) {
    val html = """
        <html dir="rtl" lang="ar"><head><meta charset="utf-8">
        <style>
          body { font-family: sans-serif; padding: 16px; color: #0f172a; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; }
          th, td { border: 1px solid #cbd5e1; padding: 6px; text-align: right; }
          th { background: #ecfdf5; }
          h1, h2, h3 { color: #064e3b; }
          .center { text-align: center; }
        </style></head><body>$bodyHtml</body></html>
    """.trimIndent()
    val webView = WebView(context)
    // Android's docs: keep the WebView referenced until the print job is created, or it can be
    // garbage collected first and nothing opens.
    printingViews.add(webView)
    webView.webViewClient = object : WebViewClient() {
        private var started = false
        override fun onPageFinished(view: WebView, url: String?) {
            if (started) return
            started = true
            try {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "تعذر إنشاء ملف PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                printingViews.remove(view)
            }
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

/** WebViews whose page is loading for [printHtml]. */
private val printingViews = mutableSetOf<WebView>()

fun escapeHtml(s: String?): String = (s ?: "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

/** Gradient banner used at the top of each screen, as on the web. */
@Composable
fun ScreenBanner(title: String, subtitle: String, icon: ImageVector, colors: List<Color>, content: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(colors), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        content()
    }
}

@Composable
fun StatTile(title: String, value: String, modifier: Modifier = Modifier, tint: Color = Color(0xFF0F172A), background: Color = Color.White) {
    Column(
        modifier.background(background, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = tint)
    }
}

/**
 * The committee's current state for filtering and counts, the same rule as the web's
 * calculateCurrentStage: the last stage that is done («تم …»); nothing after registration done is
 * "registration" («في انتظار الامتحان»). Checked against the shared rules.json in the tests.
 */
fun currentStageKey(stages: com.example.data.model.StagesMap): String {
    var key = "registration"
    for ((k, _) in OrderedStageKeys) if (stages.getStage(k).completed) key = k
    return key
}

/** Committees still waiting for their exam (web: isAwaitingExam). */
fun isAwaitingExam(stages: com.example.data.model.StagesMap): Boolean = currentStageKey(stages) == "registration"

/** Writes [bytes] to the app cache and opens the share sheet (save to Files, Drive, WhatsApp, e-mail...). */
fun shareFile(context: Context, bytes: ByteArray, fileName: String, mime: String) {
    val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = java.io.File(dir, fileName)
    file.writeBytes(bytes)
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
        .setType(mime)
        .putExtra(android.content.Intent.EXTRA_STREAM, uri)
        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(android.content.Intent.createChooser(send, fileName).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
