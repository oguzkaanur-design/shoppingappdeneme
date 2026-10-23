package com.shoppingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shoppingapp.notifications.NotificationHelper
import com.shoppingapp.ui.AppNavigation
import com.shoppingapp.ui.theme.ShoppingAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Bildirim veya widget'tan gelen ekran isteği; tüketilince null olur. */
    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRoute = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE)
        setContent {
            ShoppingAppTheme {
                AppNavigation(
                    routeRequest = requestedRoute,
                    onRouteHandled = { requestedRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(NotificationHelper.EXTRA_ROUTE)
        if (!route.isNullOrBlank()) requestedRoute = route
    }
}
