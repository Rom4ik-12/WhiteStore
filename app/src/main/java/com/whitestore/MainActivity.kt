package com.whitestore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whitestore.ui.screens.*
import com.whitestore.ui.theme.ThemePreferences
import com.whitestore.ui.theme.WhiteStoreTheme
import com.whitestore.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        enableEdgeToEdge()

        val app = application as WhiteStoreApp

        setContent {
            WhiteStoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WhiteStoreNavigation(app = app)
                }
            }
        }
    }
}

@Composable
fun WhiteStoreNavigation(app: WhiteStoreApp) {
    val navController = rememberNavController()
    val viewModel = remember {
        StoreViewModel(app.apiService, app.appInstaller, app.selfUpdateManager, app)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show Bottom Navigation only on main tabs
    val showBottomBar = currentRoute == "catalog" || currentRoute == "profile"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Apps, contentDescription = "Каталог") },
                        label = { Text("Каталог") },
                        selected = currentRoute == "catalog",
                        onClick = {
                            if (currentRoute != "catalog") {
                                navController.navigate("catalog") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Моё") },
                        label = { Text("Моё") },
                        selected = currentRoute == "profile",
                        onClick = {
                            if (currentRoute != "profile") {
                                navController.navigate("profile") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "catalog",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("catalog") {
                MainCatalogScreen(
                    viewModel = viewModel,
                    installer = app.appInstaller,
                    onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    installer = app.appInstaller,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToMyApps = { navController.navigate("my_apps") },
                    onNavigateToUpdates = { navController.navigate("updates") },
                    onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    }
                )
            }
            composable("updates") {
                UpdatesScreen(
                    viewModel = viewModel,
                    installer = app.appInstaller,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("my_apps") {
                MyAppsScreen(
                    installer = app.appInstaller,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "detail/{packageName}",
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailScreen(
                    packageName = packageName,
                    viewModel = viewModel,
                    installer = app.appInstaller,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
