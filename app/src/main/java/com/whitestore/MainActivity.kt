package com.whitestore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import com.whitestore.data.prefs.AppPreferences
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
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

    val pendingShortcut = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        AppPreferences.init(this)
        com.whitestore.util.AppIconManager.syncCurrentAppIcon(this)
        handleShortcutIntent(intent)
        enableEdgeToEdge()

        val app = application as WhiteStoreApp

        setContent {
            WhiteStoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WhiteStoreNavigation(app = app, mainActivity = this)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: android.content.Intent?) {
        val sc = intent?.getStringExtra("shortcut")
        if (!sc.isNullOrBlank()) {
            pendingShortcut.value = sc
        }
    }
}

@Composable
fun WhiteStoreNavigation(app: WhiteStoreApp, mainActivity: MainActivity? = null) {
    val navController = rememberNavController()
    val viewModel = remember {
        StoreViewModel(app.apiService, app.appInstaller, app.selfUpdateManager, app)
    }

    val shortcutTarget = mainActivity?.pendingShortcut?.value
    LaunchedEffect(shortcutTarget) {
        if (shortcutTarget != null) {
            when (shortcutTarget) {
                "updates" -> {
                    navController.navigate("updates") {
                        launchSingleTop = true
                    }
                }
                "profile" -> {
                    navController.navigate("profile") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            mainActivity.pendingShortcut.value = null
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show Bottom Navigation only on main tabs
    val showBottomBar = currentRoute == "catalog" || currentRoute == "recent" || currentRoute == "profile"

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
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
                        icon = { Icon(Icons.Default.History, contentDescription = "Недавнее") },
                        label = { Text("Недавнее") },
                        selected = currentRoute == "recent",
                        onClick = {
                            if (currentRoute != "recent") {
                                navController.navigate("recent") {
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
        val startDest = remember {
            when (mainActivity?.pendingShortcut?.value) {
                "updates" -> {
                    mainActivity.pendingShortcut.value = null
                    "updates"
                }
                "profile" -> {
                    mainActivity.pendingShortcut.value = null
                    "profile"
                }
                else -> if (AppPreferences.openMyAppsOnStart.value) "profile" else "catalog"
            }
        }
        NavHost(
            navController = navController,
            startDestination = startDest,
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
            composable("recent") {
                RecentAppsScreen(
                    viewModel = viewModel,
                    installer = app.appInstaller,
                    onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    },
                    onNavigateToSettings = {
                        navController.navigate("recent_settings")
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
                    onNavigateToIgnoredApps = { navController.navigate("ignored_apps") },
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToDesignSettings = { navController.navigate("design_settings") },
                    onNavigateToHomeScreenSettings = { navController.navigate("home_settings") },
                    onNavigateToRecentSettings = { navController.navigate("recent_settings") },
                    onNavigateToStorageSettings = { navController.navigate("storage_settings") },
                    onNavigateToInstallSettings = { navController.navigate("install_settings") },
                    onNavigateToIgnoredApps = { navController.navigate("ignored_apps") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("recent_settings") {
                RecentSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("home_settings") {
                HomeScreenSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("design_settings") {
                DesignSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("storage_settings") {
                StorageSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("install_settings") {
                InstallSettingsScreen(
                    installer = app.appInstaller,
                    onNavigateToIgnoredApps = { navController.navigate("ignored_apps") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("ignored_apps") {
                IgnoredAppsScreen(
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
                    onBack = { navController.popBackStack() },
                    onNavigateToDeveloper = { devName, companyId ->
                        val encodedName = java.net.URLEncoder.encode(devName, "UTF-8")
                        val encodedId = companyId?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                        navController.navigate("developer/$encodedName?id=$encodedId")
                    }
                )
            }
            composable(
                route = "developer/{developerName}?id={companyId}",
                arguments = listOf(
                    navArgument("developerName") { type = NavType.StringType },
                    navArgument("companyId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val encodedName = backStackEntry.arguments?.getString("developerName") ?: ""
                val developerName = try {
                    java.net.URLDecoder.decode(encodedName, "UTF-8")
                } catch (e: Exception) {
                    encodedName
                }
                val rawCompanyId = backStackEntry.arguments?.getString("companyId")?.takeIf { it.isNotBlank() }
                val companyId = rawCompanyId?.let {
                    try {
                        java.net.URLDecoder.decode(it, "UTF-8")
                    } catch (e: Exception) {
                        it
                    }
                }
                DeveloperAppsScreen(
                    developerName = developerName,
                    companyId = companyId,
                    viewModel = viewModel,
                    installer = app.appInstaller,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    }
                )
            }
        }
    }
}
