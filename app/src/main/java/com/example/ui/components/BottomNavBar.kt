package com.example.ui.components

import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import com.example.ui.theme.TextMuted
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.data.model.AppUser
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AppNavScreen
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800

data class BottomNavItem(
    val screen: AppNavScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(AppNavScreen.HOME, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(AppNavScreen.COMMITTEES, "اللجان", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(AppNavScreen.COURSE_TRACKER, "الكشوفات", Icons.Filled.TableChart, Icons.Outlined.TableChart),
    BottomNavItem(AppNavScreen.DAILY_ALERTS, "التنبيهات", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive),
    BottomNavItem(AppNavScreen.MENU, "القائمة", Icons.Filled.Menu, Icons.Outlined.Menu)
)

/** The rail on wide screens has room for every screen, in the web sidebar's order. */
val RailItems = listOf(
    BottomNavItem(AppNavScreen.HOME, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(AppNavScreen.COURSE_TRACKER, "الكشوفات", Icons.Filled.TableChart, Icons.Outlined.TableChart),
    BottomNavItem(AppNavScreen.COMMITTEES, "اللجان", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(AppNavScreen.EXAMS, "الامتحانات", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    BottomNavItem(AppNavScreen.CEREMONIES, "الحفلات", Icons.Filled.Celebration, Icons.Outlined.Celebration),
    BottomNavItem(AppNavScreen.SCHEDULE, "التقويم", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(AppNavScreen.DAILY_ALERTS, "التنبيهات", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive),
    BottomNavItem(AppNavScreen.MENU, "القائمة", Icons.Filled.Menu, Icons.Outlined.Menu)
)

@Composable
fun BukhariBottomNavBar(
    currentUser: AppUser?,
    currentScreen: AppNavScreen,
    onScreenSelected: (AppNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibleItems = remember(currentUser) {
        BottomNavItems.filter { item -> currentUser?.canAccessScreen(item.screen) != false }
    }

    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar"),
        containerColor = tBg(Color.White),
        tonalElevation = 8.dp
    ) {
        accessibleItems.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = tFg(Primary700),
                    selectedTextColor = tFg(Primary800),
                    indicatorColor = tBg(Primary100),
                    unselectedIconColor = tFg(TextMuted),
                    unselectedTextColor = tFg(TextMuted)
                ),
                modifier = Modifier.testTag("nav_item_${item.screen.route}")
            )
        }
    }
}

/** Wide-window (tablet / foldable / landscape) counterpart of [BukhariBottomNavBar]. */
@Composable
fun BukhariNavigationRail(
    currentUser: AppUser?,
    currentScreen: AppNavScreen,
    onScreenSelected: (AppNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibleItems = remember(currentUser) {
        RailItems.filter { item -> currentUser?.canAccessScreen(item.screen) != false }
    }

    NavigationRail(
        modifier = modifier.testTag("navigation_rail"),
        containerColor = tBg(Color.White)
    ) {
        androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        accessibleItems.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationRailItem(
                selected = isSelected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = { Text(text = item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = tFg(Primary700),
                    selectedTextColor = tFg(Primary800),
                    indicatorColor = tBg(Primary100),
                    unselectedIconColor = tFg(TextMuted),
                    unselectedTextColor = tFg(TextMuted)
                ),
                modifier = Modifier.testTag("rail_item_${item.screen.route}")
            )
        }
        }
    }
}
