package com.example.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.SurfacePure

private const val TAB_LOGIN = 0
private const val TAB_REGISTER = 1
private const val TAB_TRACK = 2

/** Phone typed with Latin or Arabic digits, spaces or dashes; the server normalises it. */
private val PHONE_PATTERN = Regex("^\\+?[0-9\\u0660-\\u0669\\u06F0-\\u06F9\\s-]{6,30}$")
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Same rule as the server: at least 8 characters, with letters and digits 0-9. */
internal fun passwordProblem(p: String): String? = when {
    p.length < 8 -> "كلمة المرور يجب ألا تقل عن 8 أحرف"
    !p.any { it.isLetter() } || !p.any { it in '0'..'9' } -> "كلمة المرور يجب أن تحتوي على حروف وأرقام (0-9)"
    else -> null
}

/** Validates a join request before it is sent. Returns the first problem, or null. */
internal fun joinRequestProblem(name: String, phone: String, email: String, password: String): String? = when {
    name.isBlank() || phone.isBlank() || password.isEmpty() -> "الاسم ورقم الجوال وكلمة المرور حقول مطلوبة"
    name.trim().length < 2 -> "الاسم مطلوب"
    !PHONE_PATTERN.matches(phone.trim()) -> "رقم الهاتف غير صحيح"
    email.isNotBlank() && !EMAIL_PATTERN.matches(email.trim()) -> "البريد الإلكتروني غير صحيح"
    else -> passwordProblem(password)
}

/** Login, join request and request tracking. Every outcome comes from the server. */
@Composable
fun CentralLoginPortal(
    onLogin: (String, String) -> Unit,
    onRegisterRequest: (name: String, phone: String, email: String, role: UserRole, pass: String, notes: String, onDone: (String?) -> Unit) -> Unit,
    onTrackRequest: (code: String, onResult: (String) -> Unit) -> Unit,
    isSubmitting: Boolean,
    /** Writes saved on this device that have not reached the server yet (MOB-01). */
    unsentCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_LOGIN) }
    var trackCode by rememberSaveable { mutableStateOf("") }
    var receiptCode by rememberSaveable { mutableStateOf<String?>(null) }
    var showForgotPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        if (unsentCount > 0) {
            Text(
                text = "لديك $unsentCount من التعديلات المحفوظة على هذا الجهاز لم تُرسل بعد. سجّل الدخول بنفس الحساب لإرسالها.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF92400E),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Primary800)
                .border(2.dp, Primary100, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🕌", fontSize = 38.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "مسجد الإمام البخاري",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Primary900
        )
        Text(
            text = "منظومة إدارة المسابقات والدورات العلمية الشرعية",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePure),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    listOf(
                        Triple(TAB_LOGIN, "تسجيل الدخول", "tab_login"),
                        Triple(TAB_REGISTER, "طلب انضمام", "tab_register"),
                        Triple(TAB_TRACK, "تتبع الطلب", "tab_track")
                    ).forEach { (index, label, tag) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) Primary800 else Color(0xFF64748B)
                                )
                            },
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    TAB_LOGIN -> LoginForm(
                        isSubmitting = isSubmitting,
                        onLogin = onLogin,
                        onForgotPassword = { showForgotPassword = true }
                    )
                    TAB_REGISTER -> RegisterForm(
                        isSubmitting = isSubmitting,
                        onSubmit = { name, phone, email, role, pass, notes, onDone ->
                            onRegisterRequest(name, phone, email, role, pass, notes) { code ->
                                if (code != null) {
                                    receiptCode = code
                                    trackCode = code
                                }
                                onDone(code != null)
                            }
                        }
                    )
                    else -> TrackForm(
                        code = trackCode,
                        onCodeChange = { trackCode = it },
                        onTrackRequest = onTrackRequest,
                        onGoToLogin = { selectedTab = TAB_LOGIN }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }

    receiptCode?.let { code ->
        RequestReceiptDialog(
            code = code,
            onTrack = {
                receiptCode = null
                selectedTab = TAB_TRACK
            },
            onDismiss = { receiptCode = null }
        )
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("نسيت كلمة المرور؟") },
            text = {
                Text(
                    "تواصل مع المشرف العام ليعيد تعيين كلمة المرور لحسابك. سجّل الدخول بكلمة المرور المؤقتة التي يعطيك إياها، وسيطلب منك التطبيق اختيار كلمة مرور جديدة قبل المتابعة."
                )
            },
            confirmButton = { TextButton(onClick = { showForgotPassword = false }) { Text("حسناً") } }
        )
    }
}

@Composable
private fun LoginForm(isSubmitting: Boolean, onLogin: (String, String) -> Unit, onForgotPassword: () -> Unit) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val canSubmit = identifier.isNotBlank() && password.isNotEmpty() && !isSubmitting
    val submit = { if (canSubmit) onLogin(identifier.trim(), password) }

    OutlinedTextField(
        value = identifier,
        onValueChange = { identifier = it.take(200) },
        label = { Text("رقم الهاتف أو البريد الإلكتروني") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Primary700) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_username"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it.take(128) },
        label = { Text("كلمة المرور") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Primary700) },
        trailingIcon = { PasswordToggle(passwordVisible) { passwordVisible = !passwordVisible } },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_password"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    TextButton(onClick = onForgotPassword, modifier = Modifier.testTag("btn_forgot_password")) {
        Text("نسيت كلمة المرور؟", color = Primary700)
    }
    Button(
        onClick = submit,
        enabled = canSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("btn_submit_login"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary700)
    ) {
        Text(
            text = if (isSubmitting) "جاري تسجيل الدخول..." else "تسجيل الدخول للمنظومة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun RegisterForm(
    isSubmitting: Boolean,
    onSubmit: (name: String, phone: String, email: String, role: UserRole, pass: String, notes: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by rememberSaveable { mutableStateOf(UserRole.ASSISTANT) }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(200) },
        label = { Text("الاسم الرباعي الكامل *") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_name"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it.take(30) },
        label = { Text("رقم الجوال (اسم الدخول) *") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_phone"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it.take(200) },
        label = { Text("البريد الإلكتروني (اختياري)") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_email"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it.take(128) },
        label = { Text("كلمة المرور للحساب *") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = { PasswordToggle(passwordVisible) { passwordVisible = !passwordVisible } },
        supportingText = { Text("8 أحرف على الأقل، وتحتوي على حروف وأرقام") },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_password"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "الدور المطلوب:",
        style = MaterialTheme.typography.labelMedium,
        color = Primary900,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    listOf(
        listOf(UserRole.ASSISTANT to "معاون ميداني", UserRole.TEACHER to "معلم / مقرئ"),
        listOf(UserRole.COMMITTEE_HEAD to "رئيس لجنة", UserRole.AUDITOR to "مراجع ومدقق")
    ).forEach { rowRoles ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowRoles.forEach { (option, label) ->
                val isSelected = role == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Primary100 else Color(0xFFF1F5F9))
                        .border(1.dp, if (isSelected) Primary700 else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                        .semantics {
                            this.role = Role.RadioButton
                            selected = isSelected
                        }
                        .clickable { role = option }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Primary800 else Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
    Text(
        text = "هذا طلب فقط؛ الدور النهائي يحدده المشرف العام عند الاعتماد.",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF64748B)
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it.take(2000) },
        label = { Text("ملاحظات أو تزكية (اختياري)") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3,
        shape = RoundedCornerShape(12.dp)
    )
    error?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("reg_error"))
    }
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = {
            error = joinRequestProblem(name, phone, email, password)
            if (error == null) {
                onSubmit(name.trim(), phone.trim(), email.trim(), role, password, notes.trim()) { ok ->
                    if (ok) {
                        name = ""
                        phone = ""
                        email = ""
                        password = ""
                        notes = ""
                        role = UserRole.ASSISTANT
                    }
                }
            }
        },
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("btn_submit_register"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary700)
    ) {
        Text(
            text = if (isSubmitting) "جاري إرسال الطلب..." else "إرسال طلب الانضمام",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun TrackForm(
    code: String,
    onCodeChange: (String) -> Unit,
    onTrackRequest: (code: String, onResult: (String) -> Unit) -> Unit,
    onGoToLogin: () -> Unit
) {
    var result by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    val search = {
        if (code.isNotBlank() && !searching) {
            searching = true
            onTrackRequest(code.trim()) {
                result = it
                searching = false
            }
        }
    }

    Text(
        "أدخل كود الطلب الذي ظهر لك عند التسجيل (مثل REQ-2026-XXXXXXXX).",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF475569)
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { onCodeChange(it.take(40)) },
        label = { Text("كود الطلب") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_track_code"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = search,
        enabled = code.isNotBlank() && !searching,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_track"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary700)
    ) {
        Text(if (searching) "جاري الاستعلام..." else "استعلام عن الطلب", color = Color.White, fontWeight = FontWeight.Bold)
    }
    result?.let {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Primary900,
            modifier = Modifier.testTag("track_result")
        )
        if (it.startsWith("تم اعتماد")) {
            TextButton(onClick = onGoToLogin) { Text("الانتقال لتسجيل الدخول", color = Primary700) }
        }
    }
}

@Composable
private fun RequestReceiptDialog(code: String, onTrack: () -> Unit, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تم إرسال طلب الانضمام") },
        text = {
            Column {
                Text("طلبك الآن قيد مراجعة المشرف العام. كود متابعة الطلب:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary900,
                    modifier = Modifier.testTag("receipt_code")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "احتفظ بهذا الكود: هو الطريقة الوحيدة لمتابعة حالة طلبك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                }) {
                    Text(if (copied) "تم النسخ ✓" else "نسخ الكود")
                }
            }
        },
        confirmButton = { TextButton(onClick = onTrack) { Text("متابعة الطلب") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (visible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
            tint = Color(0xFF64748B)
        )
    }
}
