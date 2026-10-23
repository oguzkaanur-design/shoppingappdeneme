package com.shoppingapp.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shoppingapp.R
import com.shoppingapp.feature.backup.BackupScreen
import com.shoppingapp.feature.money.SavingsScreen
import com.shoppingapp.feature.offers.FlyerListScreen
import com.shoppingapp.feature.offers.FlyerListViewModel
import com.shoppingapp.feature.offers.OffersScreen
import com.shoppingapp.feature.pantry.PantryScreen
import com.shoppingapp.feature.scanner.BarcodeScannerScreen
import com.shoppingapp.feature.stats.PfandScreen
import com.shoppingapp.feature.stats.StatsScreen

data class BottomNavItem(@StringRes val labelRes: Int, val route: String, val icon: ImageVector)

// Alt bar 5 sekmeyle sınırlı. Kiler v2.0'da günlük kullanılan bir sekme oldu;
// Pfand artık Cebim ekranındaki karttan açılıyor (iade tutarı zaten oraya yazılıyor).
private val bottomItems = listOf(
    BottomNavItem(R.string.nav_list, "home", Icons.Default.Home),
    BottomNavItem(R.string.nav_pantry, "pantry", Icons.Default.Kitchen),
    BottomNavItem(R.string.nav_offers, "offers", Icons.Default.LocalOffer),
    BottomNavItem(R.string.nav_flyer_list, "flyerList", Icons.Default.Bookmark),
    BottomNavItem(R.string.nav_money, "money", Icons.Default.Savings)
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    routeRequest: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isScanner = currentRoute == "scanner"
    val flyerListViewModel: FlyerListViewModel = hiltViewModel()
    val flyerCount by flyerListViewModel.count.collectAsState()

    // Bildirim ve widget kısayolları: istek bir kez tüketilir.
    LaunchedEffect(routeRequest) {
        val route = routeRequest ?: return@LaunchedEffect
        if (route == "scanner" || route == "backup" || route == "stats" || route == "pfand") {
            navController.navigate(route) { launchSingleTop = true }
        } else {
            navController.navigateTopLevel(route)
        }
        onRouteHandled()
    }

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
                            label = { Text(stringResource(item.labelRes)) }
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
                    onNavigateToScanner = { navController.navigate("scanner") },
                    onNavigateToBackup = { navController.navigate("backup") }
                )
            }
            composable("pantry") { PantryScreen() }
            composable("offers") { OffersScreen() }
            composable("flyerList") { FlyerListScreen() }
            composable("money") {
                SavingsScreen(onOpenPfand = { navController.navigateTopLevel("pfand") })
            }
            composable("pfand") { PfandScreen() }
            composable("stats") {
                StatsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("backup") {
                BackupScreen(onNavigateBack = { navController.popBackStack() })
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
