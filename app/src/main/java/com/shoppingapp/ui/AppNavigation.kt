package com.shoppingapp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shoppingapp.feature.money.SavingsScreen
import com.shoppingapp.feature.scanner.BarcodeScannerScreen
import com.shoppingapp.feature.offers.FlyerListScreen
import com.shoppingapp.feature.offers.FlyerListViewModel
import com.shoppingapp.feature.offers.OffersScreen
import com.shoppingapp.feature.stats.PfandScreen
import com.shoppingapp.feature.stats.StatsScreen

data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)

// Alt bar 5 sekmeyle sınırlı: "İstatistik" artık liste ekranının üst barındaki grafik
// düğmesinden açılıyor, yerine günlük kullanılan "Cebim" sekmesi geldi.
private val bottomItems = listOf(
    BottomNavItem("Liste", "home", Icons.Default.Home),
    BottomNavItem("Prospekt", "offers", Icons.Default.LocalOffer),
    BottomNavItem("Fırsatlar", "flyerList", Icons.Default.Bookmark),
    BottomNavItem("Cebim", "money", Icons.Default.Savings),
    BottomNavItem("Pfand", "pfand", Icons.Default.Redeem)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isScanner = currentRoute == "scanner"
    val flyerListViewModel: FlyerListViewModel = hiltViewModel()
    val flyerCount by flyerListViewModel.count.collectAsState()

    Scaffold(
        bottomBar = {
            if (!isScanner) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = {
                                if (item.route == "flyerList" && flyerCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$flyerCount") } }) {
                                        Icon(item.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = null)
                                }
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(if (isScanner) PaddingValues() else padding)
        ) {
            composable("home") {
                ShoppingScreen(
                    onNavigateToStats = { navController.navigateTopLevel("stats") },
                    onNavigateToScanner = { navController.navigate("scanner") }
                )
            }
            composable("offers") { OffersScreen() }
            composable("flyerList") { FlyerListScreen() }
            composable("money") {
                SavingsScreen(onOpenPfand = { navController.navigateTopLevel("pfand") })
            }
            composable("pfand") { PfandScreen() }
            composable("stats") {
                StatsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("scanner") {
                BarcodeScannerScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
