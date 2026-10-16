package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Calendar dates as the server stores them (YYYY-MM-DD), checked for real days (MOB-16). */
object Dates {
    private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** True for a real calendar day such as 2026-02-28; false for 2026-02-30 or free text. */
    fun isRealDate(value: String): Boolean {
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(value)) return false
        return try {
            isoFormat().parse(value) != null
        } catch (e: ParseException) {
            false
        }
    }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun fromMillis(millis: Long): String = isoFormat().format(Date(millis))

    fun toMillis(value: String): Long? = if (isRealDate(value)) isoFormat().parse(value)?.time else null

    /** Server times are ISO; shown in the device's locale. Older free-text values are shown as they are. */
    fun displayTime(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        for (f in formats) {
            try {
                val parsed = SimpleDateFormat(f, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value) ?: continue
                return SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(parsed)
            } catch (e: ParseException) {
                // try the next format
            }
        }
        return value
    }
}

/**
 * A date chosen from a calendar instead of typed (MOB-16): a typed impossible date used to reach
 * the server and fail. The field shows the chosen day; [value] is YYYY-MM-DD or blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ")
            }
        },
        modifier = modifier.fillMaxWidth()
    )
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toMillis(value) ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onValueChange(Dates.fromMillis(it)) }
                    open = false
                }) { Text("اختيار") }
            },
            dismissButton = {
                if (allowClear) {
                    TextButton(onClick = {
                        onValueChange("")
                        open = false
                    }) { Text("بدون تاريخ") }
                } else {
                    TextButton(onClick = { open = false }) { Text("إلغاء") }
                }
            }
        ) { DatePicker(state = state) }
    }
}
