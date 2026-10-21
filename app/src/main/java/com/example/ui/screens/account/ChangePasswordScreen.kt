package com.example.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** Same rule as the server (Web/server/routes/users.ts passwordSchema). */
fun passwordProblem(password: String): String? = when {
    password.length < 8 -> "كلمة المرور يجب ألا تقل عن 8 أحرف"
    !password.any { it.isLetter() } || !password.any { it.isDigit() } -> "كلمة المرور يجب أن تحتوي على حروف وأرقام"
    else -> null
}

/**
 * Changes the own password (MOB-08). With [forced], it is shown instead of the app after an admin
 * reset the password: the server refuses everything else until a new password is chosen.
 */
@Composable
fun ChangePasswordScreen(
    forced: Boolean,
    isSubmitting: Boolean,
    onSubmit: (current: String, next: String, onDone: (Boolean) -> Unit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (forced) "اختر كلمة مرور جديدة" else "تغيير كلمة المرور",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (forced) {
            Text(
                "أعاد المشرف العام تعيين كلمة مرورك. أدخل كلمة المرور المؤقتة التي أعطاك إياها، ثم اختر كلمة مرور جديدة لا يعرفها غيرك.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
        }
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text(if (forced) "كلمة المرور المؤقتة" else "كلمة المرور الحالية") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = next,
            onValueChange = { next = it },
            label = { Text("كلمة المرور الجديدة (8 أحرف على الأقل، حروف وأرقام)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("تأكيد كلمة المرور الجديدة") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall) }
        Button(
            enabled = !isSubmitting,
            onClick = {
                error = when {
                    current.isBlank() -> "أدخل كلمة المرور الحالية"
                    next != confirm -> "كلمتا المرور الجديدتان غير متطابقتين"
                    next == current -> if (forced) "اختر كلمة مرور جديدة مختلفة عن كلمة المرور المؤقتة" else "كلمة المرور الجديدة يجب أن تختلف عن الحالية"
                    else -> passwordProblem(next)
                }
                if (error == null) onSubmit(current, next) { ok -> if (ok) { current = ""; next = ""; confirm = "" } }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isSubmitting) "جارٍ الحفظ..." else "حفظ كلمة المرور") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(if (forced) "تسجيل الخروج" else "رجوع")
        }
    }
}
