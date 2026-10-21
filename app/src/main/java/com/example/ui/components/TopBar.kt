package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.data.model.NotificationItem
import com.example.data.model.UserRole
import com.example.ui.AppNavScreen
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary200
import com.example.ui.theme.Primary500
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.ui.draw.rotate

@Composable
fun BukhariTopBar(
    currentUser: AppUser?,
    notifications: List<NotificationItem>,
    isSyncing: Boolean = false,
    /** Device has network AND the last sync with the server succeeded. */
    isOnline: Boolean = false,
    /** Name of the default course, shown under the title (nothing when there is none). */
    activeCourseName: String? = null,
    onSync: () -> Unit = {},
    onNavigate: (AppNavScreen) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "topBarSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "spin"
    )

    var showMenu by remember { mutableStateOf(false) }
    var showNotifs by remember { mutableStateOf(false) }

    val unreadCount = notifications.count { !it.read }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Primary800)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: Mosque Brand & Live Pulse
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mosque icon symbol
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary700),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕌",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مسجد الإمام البخاري",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Connection indicator: green only when the last sync with the server worked.
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFF59E0B))
                        )
                    }

                    Text(
                        text = activeCourseName ?: if (isOnline) "متصل بالخادم" else "غير متصل — تعرض آخر بيانات محفوظة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary200,
                        fontSize = 12.sp
                    )
                }
            }

            // Left Side: Action Icons & User Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Cloud Sync Button
                        IconButton(
                    onClick = onSync,
                    modifier = Modifier.testTag("btn_cloud_sync")
                ) {
                    if (isSyncing) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "جاري المزامنة",
                            tint = Color(0xFF86EFAC),
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    } else {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = if (isOnline) "متصل بالخادم — اضغط للتحديث" else "غير متصل — اضغط لإعادة المحاولة",
                            tint = if (isOnline) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                        )
                    }
                }

                // Notifications Icon with Unread Badge
                Box {
                    IconButton(
                        onClick = { onNavigate(AppNavScreen.NOTIFICATIONS) },
                        modifier = Modifier.testTag("btn_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount", fontSize = 12.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "الإشعارات والتنبيهات",
                                tint = Primary100
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showNotifs,
                        onDismissRequest = { showNotifs = false }
                    ) {
                        Text(
                            text = "🔔 آخر التنبيهات الميدانية:",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary800
                        )
                        if (notifications.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("لا توجد إشعارات حالية") },
                                onClick = { showNotifs = false }
                            )
                        } else {
                            notifications.take(5).forEach { notif ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = notif.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (notif.type == "urgent") Color(0xFFB91C1C) else Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = notif.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = notif.createdAt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onMarkNotificationRead(notif.id)
                                        showNotifs = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Side Menu for Extra Tools
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("btn_side_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "القائمة الإضافية",
                            tint = Primary100
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        currentUser?.let { user ->
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = user.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Primary900
                                )
                                Text(
                                    text = "${user.role.emoji} ${user.role.titleAr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary700
                                )
                            }
                        }

                        if (currentUser?.canAccessScreen(AppNavScreen.ASSISTANTS) == true) {
                            DropdownMenuItem(
                                text = { Text("👮 الشيوخ وإدارة الصلاحيات") },
                                onClick = {
                                    onNavigate(AppNavScreen.ASSISTANTS)
                                    showMenu = false
                                }
                            )
                        }
                        if (currentUser?.canAccessScreen(AppNavScreen.DATABASE_INSPECTOR) == true) {
                            DropdownMenuItem(
                                text = { Text("🗄️ حالة البيانات والمزامنة") },
                                onClick = {
                                    onNavigate(AppNavScreen.DATABASE_INSPECTOR)
                                    showMenu = false
                                }
                            )
                        }
                        if (currentUser?.canAccessScreen(AppNavScreen.STATISTICS) == true) {
                            DropdownMenuItem(
                                text = { Text("📊 الإحصائيات الشاملة ونسب الإنجاز") },
                                onClick = {
                                    onNavigate(AppNavScreen.STATISTICS)
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("🔑 تغيير كلمة المرور") },
                            onClick = {
                                onChangePassword()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🚪 تسجيل الخروج", color = Color(0xFFEF4444)) },
                            onClick = {
                                onLogout()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
