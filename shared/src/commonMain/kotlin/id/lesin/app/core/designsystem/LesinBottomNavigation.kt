package id.lesin.app.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class LesinTab(val label: String) {
    HOME("Home"), STUDENTS("Students"), BILLING("Billing"),
}

@Composable
fun LesinBottomNavigation(
    selectedTab: LesinTab,
    onHomeClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onBillingClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        HorizontalDivider(color = LesinColors.Hairline)
        NavigationBar(containerColor = LesinColors.Canvas) {
            LesinTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = when (tab) {
                        LesinTab.HOME -> onHomeClick
                        LesinTab.STUDENTS -> onStudentsClick
                        LesinTab.BILLING -> onBillingClick
                    },
                    icon = {
                        Icon(when (tab) {
                            LesinTab.HOME -> LesinIcons.Home
                            LesinTab.STUDENTS -> LesinIcons.Students
                            LesinTab.BILLING -> LesinIcons.Billing
                        }, contentDescription = null)
                    },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LesinColors.Ink,
                        selectedTextColor = LesinColors.Ink,
                        indicatorColor = LesinColors.Surface,
                        unselectedIconColor = LesinColors.Muted,
                        unselectedTextColor = LesinColors.Muted,
                    ),
                )
            }
        }
    }
}
