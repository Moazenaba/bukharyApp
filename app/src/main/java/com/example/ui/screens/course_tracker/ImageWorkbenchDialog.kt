package com.example.ui.screens.course_tracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TextMuted
import java.io.ByteArrayOutputStream

/** Largest side sent to the server; a phone photo is far bigger than the table needs. */
private const val MAX_SIDE = 2400

/** Rotation, crop and contrast applied to the photo, the same steps as the web's ImageWorkbench. */
private fun process(src: Bitmap, rotation: Int, cropX: ClosedFloatingPointRange<Float>, cropY: ClosedFloatingPointRange<Float>, contrast: Boolean): Bitmap {
    val rotated = if (rotation % 360 == 0) src else Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
    val x = (cropX.start * rotated.width).toInt().coerceIn(0, rotated.width - 1)
    val y = (cropY.start * rotated.height).toInt().coerceIn(0, rotated.height - 1)
    val w = ((cropX.endInclusive - cropX.start) * rotated.width).toInt().coerceIn(50.coerceAtMost(rotated.width - x), rotated.width - x)
    val h = ((cropY.endInclusive - cropY.start) * rotated.height).toInt().coerceIn(50.coerceAtMost(rotated.height - y), rotated.height - y)
    val cropped = Bitmap.createBitmap(rotated, x, y, w, h)
    if (!contrast) return cropped
    // contrast(135%) brightness(105%), as the web's canvas filter.
    val c = 1.35f
    val t = (-0.5f * c + 0.5f) * 255f + 0.05f * 255f
    val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(cropped, 0f, 0f, Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f)))
    })
    return out
}

private fun loadScaled(context: android.content.Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
}

/**
 * Prepares a photo of a paper sheet before the server reads its table: rotate, crop to the
 * table, and raise the contrast. Calls [onExtract] with the JPEG to send.
 */
@Composable
fun ImageWorkbenchDialog(uri: Uri, onDismiss: () -> Unit, onExtract: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val source = remember(uri) { runCatching { loadScaled(context, uri) }.getOrNull() }
    var rotation by remember { mutableIntStateOf(0) }
    var cropX by remember { mutableStateOf(0f..1f) }
    var cropY by remember { mutableStateOf(0f..1f) }
    var contrast by remember { mutableStateOf(true) }
    val preview = remember(source, rotation, cropX, cropY, contrast) {
        source?.let { runCatching { process(it, rotation, cropX, cropY, contrast) }.getOrNull() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تجهيز صورة الكشف قبل الاستخراج") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview == null) {
                    Text("تعذر فتح الصورة المختارة.", color = MaterialTheme.colorScheme.error)
                } else {
                    Image(
                        preview.asImageBitmap(), contentDescription = "معاينة الصورة بعد التجهيز",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), contentScale = ContentScale.Fit
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { rotation = (rotation + 270) % 360 }) { Text("↺ تدوير لليسار") }
                        OutlinedButton(onClick = { rotation = (rotation + 90) % 360 }) { Text("↻ تدوير لليمين") }
                    }
                    Text("قص أفقي (من اليمين لليسار)", style = MaterialTheme.typography.labelMedium)
                    RangeSlider(value = cropX, onValueChange = { if (it.endInclusive - it.start >= 0.1f) cropX = it }, valueRange = 0f..1f)
                    Text("قص رأسي (من الأعلى للأسفل)", style = MaterialTheme.typography.labelMedium)
                    RangeSlider(value = cropY, onValueChange = { if (it.endInclusive - it.start >= 0.1f) cropY = it }, valueRange = 0f..1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(contrast, { contrast = it })
                        Text("تحسين التباين لوضوح الخط")
                    }
                    Text("كل صف مستخرج يُعلَّم «يحتاج تدقيق» حتى تراجعه.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        },
        confirmButton = {
            Button(enabled = preview != null, onClick = {
                val out = ByteArrayOutputStream()
                preview?.compress(Bitmap.CompressFormat.JPEG, 92, out)
                onExtract(out.toByteArray())
            }) { Text("بدء الاستخراج") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
