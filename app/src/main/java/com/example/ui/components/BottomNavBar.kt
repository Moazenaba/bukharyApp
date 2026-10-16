package com.example.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
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
    BottomNavItem(AppNavScreen.COURSE_TRACKER, "كشف الدورات", Icons.Filled.TableChart, Icons.Outlined.TableChart),
    BottomNavItem(AppNavScreen.COMMITTEES, "اللجان والمراحل", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(AppNavScreen.EXAMS, "الامتحانات", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    BottomNavItem(AppNavScreen.SCHEDULE, "التقويم والمهام", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
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
        containerColor = Color.White,
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
                    selectedIconColor = Primary700,
                    selectedTextColor = Primary800,
                    indicatorColor = Primary100,
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
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
        BottomNavItems.filter { item -> currentUser?.canAccessScreen(item.screen) != false }
    }

    NavigationRail(
        modifier = modifier.testTag("navigation_rail"),
        containerColor = Color.White
    ) {
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
                    selectedIconColor = Primary700,
                    selectedTextColor = Primary800,
                    indicatorColor = Primary100,
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
                ),
                modifier = Modifier.testTag("rail_item_${item.screen.route}")
            )
        }
    }
}
