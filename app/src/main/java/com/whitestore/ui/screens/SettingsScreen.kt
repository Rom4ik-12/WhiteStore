package com.whitestore.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.whitestore.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.whitestore.data.prefs.AutoCheckInterval
import com.whitestore.data.prefs.UpdateNetworkMode
import com.whitestore.data.updater.UpdateCheckReceiver
import com.whitestore.ui.components.ConnectedButtonGroup
import com.whitestore.ui.theme.AccentColor
import com.whitestore.ui.theme.AppThemeMode
import com.whitestore.ui.theme.ThemePreferences
import com.whitestore.util.AppIcon
import com.whitestore.util.CachedApkItem
import com.whitestore.util.StorageManager
import com.whitestore.util.StorageUsage
import com.whitestore.util.TimeFormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDesignSettings: () -> Unit = {},
    onNavigateToStorageSettings: () -> Unit = {},
    onNavigateToInstallSettings: () -> Unit = {},
    onNavigateToIgnoredApps: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Дизайн
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToDesignSettings),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Дизайн",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Тема оформления, акцентный цвет, вид обновлений",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 2. Память и данные
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToStorageSettings),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Память и данные",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Кеш APK, лимиты хранилища, сеть для загрузки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 3. Установка и обновления
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToInstallSettings),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Установка и обновления",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Параметры установки, автопроверка, исключения",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 4. Поддержать проект донатом (CloudTips)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            val donateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pay.cloudtips.ru/p/6aea26b5"))
                            context.startActivity(donateIntent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Поддержать проект",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Донат автору на CloudTips • Спасибо!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 6. Developer / GitHub Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rom4ik-12/WhiteStore"))
                            context.startActivity(browserIntent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Разработчик",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Репозиторий на GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // App Version Info
            item {
                val versionName = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.6"
                    } catch (e: Exception) {
                        "1.0.6"
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WhiteStore v$versionName • Material Expressive",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Screen to choose which installed apps should be excluded/ignored from updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredAppsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val ignoredSet by AppPreferences.ignoredPackages

    var rawAppsList by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            packages.filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    InstalledAppInfo(
                        name = label,
                        packageName = appInfo.packageName,
                        versionName = ""
                    )
                }
        }
        rawAppsList = result
    }

    // Sort apps so that already selected/ignored apps appear at the very top
    val appsList = remember(rawAppsList, ignoredSet) {
        rawAppsList.sortedWith(
            compareByDescending<InstalledAppInfo> { ignoredSet.contains(it.packageName) }
                .thenBy { it.name.lowercase() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Исключения обновлений") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Отмеченные приложения не будут проверяться на наличие обновлений и не появятся в списке доступных обновлений.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(appsList, key = { it.packageName }) { app ->
                val isIgnored = ignoredSet.contains(app.packageName)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            AppPreferences.toggleIgnorePackage(context, app.packageName)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIgnored) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            packageName = app.packageName,
                            size = 44.dp,
                            shapeRadius = 12.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Checkbox(
                            checked = isIgnored,
                            onCheckedChange = {
                                AppPreferences.toggleIgnorePackage(context, app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sub-screen: "Дизайн" with sections "Общее" and "Обновления"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(ThemePreferences.currentThemeMode.value) }
    var dynamicColorEnabled by remember { mutableStateOf(ThemePreferences.dynamicColorEnabled.value) }
    var currentAccentColor by remember { mutableStateOf(ThemePreferences.currentAccentColor.value) }
    var showInitialVersion by remember { mutableStateOf(AppPreferences.showInitialVersion.value) }
    var useIconUpdateButton by remember { mutableStateOf(AppPreferences.useIconUpdateButton.value) }
    var showUpdateCountBadge by remember { mutableStateOf(AppPreferences.showUpdateCountBadge.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дизайн") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Общее
            item {
                Text(
                    text = "Общее",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Тема оформления",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ConnectedButtonGroup(
                            items = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK),
                            selectedItem = currentMode,
                            onItemSelected = { mode ->
                                currentMode = mode
                                ThemePreferences.setThemeMode(context, mode)
                            },
                            labelProvider = { mode ->
                                when (mode) {
                                    AppThemeMode.SYSTEM -> "Система"
                                    AppThemeMode.LIGHT -> "Светлая"
                                    AppThemeMode.DARK -> "Тёмная"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newVal = !dynamicColorEnabled
                                        dynamicColorEnabled = newVal
                                        ThemePreferences.setDynamicColorEnabled(context, newVal)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Динамические цвета (Monet)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Генерация палитры из обоев системы",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = dynamicColorEnabled,
                                    onCheckedChange = {
                                        dynamicColorEnabled = it
                                        ThemePreferences.setDynamicColorEnabled(context, it)
                                    }
                                )
                            }
                        }

                        if (!dynamicColorEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Акцентный цвет",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AccentColor.entries.forEach { accent ->
                                    val isSelected = currentAccentColor == accent
                                    val color = Color(accent.primaryLight)
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(color)
                                            .clickable {
                                                currentAccentColor = accent
                                                ThemePreferences.setAccentColor(context, accent)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = accent.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Обновления
            item {
                Text(
                    text = "Обновления",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newVal = !useIconUpdateButton
                                    useIconUpdateButton = newVal
                                    AppPreferences.setUseIconUpdateButton(context, newVal)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Иконка вместо надписи «Обновить»",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Компактная кнопка-иконка в карточках обновлений",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useIconUpdateButton,
                                onCheckedChange = {
                                    useIconUpdateButton = it
                                    AppPreferences.setUseIconUpdateButton(context, it)
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newVal = !showInitialVersion
                                    showInitialVersion = newVal
                                    AppPreferences.setShowInitialVersion(context, newVal)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Показывать установленную версию",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Отображать переход с текущей на новую версию («1.0 ➔ 2.0»)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showInitialVersion,
                                onCheckedChange = {
                                    showInitialVersion = it
                                    AppPreferences.setShowInitialVersion(context, it)
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newVal = !showUpdateCountBadge
                                    showUpdateCountBadge = newVal
                                    AppPreferences.setShowUpdateCountBadge(context, newVal)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Количество обновлений в кружочке",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Отображать бейдж с числом доступных обновлений на плитке",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showUpdateCountBadge,
                                onCheckedChange = {
                                    showUpdateCountBadge = it
                                    AppPreferences.setShowUpdateCountBadge(context, it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sub-screen: "Память и данные"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentNetworkMode by remember { mutableStateOf(AppPreferences.updateNetworkMode.value) }
    var currentLimitMb by remember { mutableStateOf(AppPreferences.maxCacheSizeMb.value) }
    var currentSpeedLimitKb by remember { mutableStateOf(AppPreferences.downloadSpeedLimitKb.value) }
    var storageUsage by remember { mutableStateOf<StorageUsage?>(null) }
    var isLoadingUsage by remember { mutableStateOf(true) }

    fun refreshUsage() {
        isLoadingUsage = true
        coroutineScope.launch {
            storageUsage = StorageManager.getStorageUsage(context)
            isLoadingUsage = false
        }
    }

    LaunchedEffect(Unit) {
        refreshUsage()
    }

    val cacheLimits = listOf(512, 1024, 2048, 0) // 0 = Без ограничений

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Память и данные") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Секция 1: Сеть и трафик
            item {
                Text(
                    text = "Сеть и передача данных",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Сеть для обновлений и загрузок",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Определяет, через какое интернет-соединение разрешено проверять и скачивать приложения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ConnectedButtonGroup(
                            items = listOf(UpdateNetworkMode.ANY, UpdateNetworkMode.WIFI_ONLY, UpdateNetworkMode.CELLULAR_ONLY),
                            selectedItem = currentNetworkMode,
                            onItemSelected = { mode ->
                                currentNetworkMode = mode
                                AppPreferences.setUpdateNetworkMode(context, mode)
                            },
                            labelProvider = { mode ->
                                when (mode) {
                                    UpdateNetworkMode.ANY -> "Любая"
                                    UpdateNetworkMode.WIFI_ONLY -> "Wi-Fi"
                                    UpdateNetworkMode.CELLULAR_ONLY -> "Мобильная"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Секция 2: Лимит кеша APK
            item {
                Text(
                    text = "Ограничение скорости загрузки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Максимальная скорость",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val speedLabel = when {
                                currentSpeedLimitKb <= 0 -> "Без лимита"
                                currentSpeedLimitKb >= 1024 && currentSpeedLimitKb % 1024 == 0 -> "${currentSpeedLimitKb / 1024} МБ/с"
                                currentSpeedLimitKb >= 1024 -> String.format(java.util.Locale.getDefault(), "%.1f МБ/с", currentSpeedLimitKb / 1024f)
                                else -> "$currentSpeedLimitKb КБ/с"
                            }
                            Text(
                                text = speedLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ограничивает скорость скачивания APK файлов, чтобы не перегружать канал связи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val speedSliderValue = when (currentSpeedLimitKb) {
                            512 -> 1f
                            1024 -> 2f
                            2048 -> 3f
                            5120 -> 4f
                            10240 -> 5f
                            else -> 0f // 0 -> Без ограничений
                        }

                        Slider(
                            value = speedSliderValue,
                            onValueChange = { pos ->
                                val targetKb = when (pos.toInt()) {
                                    1 -> 512
                                    2 -> 1024
                                    3 -> 2048
                                    4 -> 5120
                                    5 -> 10240
                                    else -> 0
                                }
                                currentSpeedLimitKb = targetKb
                                AppPreferences.setDownloadSpeedLimitKb(context, targetKb)
                            },
                            valueRange = 0f..5f,
                            steps = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("∞", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            Text("512 КБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("1 МБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("2 МБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("5 МБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("10 МБ/с", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Секция 3: Лимит кеша APK
            item {
                Text(
                    text = "Ограничение кеша",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Максимальный объём кеша APK",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val limitLabel = when {
                                currentLimitMb <= 0 -> "Без лимита"
                                currentLimitMb >= 1024 && currentLimitMb % 1024 == 0 -> "${currentLimitMb / 1024} ГБ"
                                currentLimitMb >= 1024 -> String.format(java.util.Locale.getDefault(), "%.1f ГБ", currentLimitMb / 1024f)
                                else -> "$currentLimitMb МБ"
                            }
                            Text(
                                text = limitLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "При превышении лимита старые загруженные файлы APK будут автоматически удаляться",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val sliderValue = when (currentLimitMb) {
                            256 -> 0f
                            512 -> 1f
                            1024 -> 2f
                            2048 -> 3f
                            4096 -> 4f
                            else -> 5f // 0 -> Unlimited
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { pos ->
                                val targetMb = when (pos.toInt()) {
                                    0 -> 256
                                    1 -> 512
                                    2 -> 1024
                                    3 -> 2048
                                    4 -> 4096
                                    else -> 0
                                }
                                currentLimitMb = targetMb
                                AppPreferences.setMaxCacheSizeMb(context, targetMb)
                            },
                            onValueChangeFinished = {
                                coroutineScope.launch {
                                    StorageManager.pruneApksIfNeeded(context)
                                    refreshUsage()
                                }
                            },
                            valueRange = 0f..5f,
                            steps = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("256 МБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("1 ГБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("2 ГБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("4 ГБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("∞", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Секция 3: Занято памяти и очистка
            item {
                Text(
                    text = "Использование хранилища",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Всего занято кешем",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val usageText = if (storageUsage != null) {
                                    val apks = StorageManager.formatBytes(storageUsage!!.apksSizeBytes)
                                    val imgs = StorageManager.formatBytes(storageUsage!!.imagesCacheSizeBytes)
                                    "APK: $apks • Изображения: $imgs"
                                } else {
                                    "Подсчёт..."
                                }
                                Text(
                                    text = usageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (storageUsage != null) StorageManager.formatBytes(storageUsage!!.totalSizeBytes) else "...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        StorageManager.clearAllApks(context)
                                        refreshUsage()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                enabled = (storageUsage?.apksSizeBytes ?: 0L) > 0
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Очистить APK", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        StorageManager.clearImageCache(context)
                                        refreshUsage()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                enabled = (storageUsage?.imagesCacheSizeBytes ?: 0L) > 0
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Очистить фото", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Секция 4: Закешированные в данный момент APK
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Загруженные APK (${storageUsage?.apkFiles?.size ?: 0})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val apkList = storageUsage?.apkFiles ?: emptyList()
            if (apkList.isEmpty() && !isLoadingUsage) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет сохранённых APK файлов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(apkList, key = { it.file.absolutePath }) { apkItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = apkItem.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = StorageManager.formatBytes(apkItem.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val dest = StorageManager.saveApkToDownloads(context, apkItem.file, apkItem.name)
                                        if (dest != null) {
                                            android.widget.Toast.makeText(context, "Файл сохранён в Загрузки:\n${dest.name}", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            StorageManager.shareApk(context, apkItem.file)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SaveAlt,
                                    contentDescription = "Сохранить в Загрузки",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    StorageManager.shareApk(context, apkItem.file)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Поделиться APK",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        StorageManager.deleteApk(apkItem.file)
                                        refreshUsage()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить APK",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Submenu: Установка и обновления
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallSettingsScreen(
    installer: com.whitestore.data.installer.AppInstaller? = null,
    onNavigateToIgnoredApps: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var openAfterInstall by remember { mutableStateOf(AppPreferences.openAfterInstall.value) }
    var deleteApkAfterInstall by remember { mutableStateOf(AppPreferences.deleteApkAfterInstall.value) }
    var currentInterval by remember { mutableStateOf(AppPreferences.autoCheckInterval.value) }
    var autoCheckNotify by remember { mutableStateOf(AppPreferences.autoCheckNotify.value) }
    val lastCheckTime by AppPreferences.lastUpdateCheckTime
    val ignoredPackages by AppPreferences.ignoredPackages
    val ignoredCount = ignoredPackages.size

    val hasInstallPermission = remember(installer) {
        installer?.canRequestPackageInstalls() ?: true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Установка и обновления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Раздел: Установка
            item {
                Text(
                    text = "Параметры установки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // Открыть после установки
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newVal = !openAfterInstall
                                openAfterInstall = newVal
                                AppPreferences.setOpenAfterInstall(context, newVal)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Открыть после установки",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Автоматически запускать приложение сразу после завершения его установки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = openAfterInstall,
                            onCheckedChange = {
                                openAfterInstall = it
                                AppPreferences.setOpenAfterInstall(context, it)
                            }
                        )
                    }
                }
            }

            // Удалять APK после установки
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newVal = !deleteApkAfterInstall
                                deleteApkAfterInstall = newVal
                                AppPreferences.setDeleteApkAfterInstall(context, newVal)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Удалять APK после установки",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Автоматически удалять загруженный установочный файл после успешной установки для экономии памяти",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = deleteApkAfterInstall,
                            onCheckedChange = {
                                deleteApkAfterInstall = it
                                AppPreferences.setDeleteApkAfterInstall(context, it)
                            }
                        )
                    }
                }
            }

            // Разрешение на установку
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            installer?.openInstallPermissionSettings()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hasInstallPermission) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasInstallPermission) Icons.Default.Security else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasInstallPermission) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Разрешение на установку",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasInstallPermission) "Разрешено в системе" else "Требуется разрешение в настройках системы",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasInstallPermission) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Раздел: Обновления
            item {
                Text(
                    text = "Фоновые обновления",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Автопроверка обновлений
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Автопроверка обновлений",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Периодическая проверка наличия новых версий в фоне",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lastCheckTime > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val relativeTime = TimeFormatUtils.formatRelativeTime(lastCheckTime)
                            val exactTime = TimeFormatUtils.formatExactDateTime(lastCheckTime)
                            Text(
                                text = "Последняя проверка: $relativeTime ($exactTime)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ConnectedButtonGroup(
                            items = listOf(AutoCheckInterval.OFF, AutoCheckInterval.HOURS_6, AutoCheckInterval.HOURS_12, AutoCheckInterval.HOURS_24),
                            selectedItem = currentInterval,
                            onItemSelected = { interval ->
                                currentInterval = interval
                                AppPreferences.setAutoCheckInterval(context, interval)
                                UpdateCheckReceiver.scheduleNextCheck(context, interval)
                            },
                            labelProvider = { interval ->
                                when (interval) {
                                    AutoCheckInterval.OFF -> "Выкл"
                                    AutoCheckInterval.HOURS_6 -> "6 ч"
                                    AutoCheckInterval.HOURS_12 -> "12 ч"
                                    AutoCheckInterval.HOURS_24 -> "24 ч"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (currentInterval != AutoCheckInterval.OFF) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newVal = !autoCheckNotify
                                        autoCheckNotify = newVal
                                        AppPreferences.setAutoCheckNotify(context, newVal)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Отправлять уведомления",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Показывать пуш при найденных обновлениях",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoCheckNotify,
                                    onCheckedChange = {
                                        autoCheckNotify = it
                                        AppPreferences.setAutoCheckNotify(context, it)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Исключения обновлений
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToIgnoredApps),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Исключения обновлений",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (ignoredCount > 0) "Игнорируются: $ignoredCount прил." else "Игнорировать обновления выбранных приложений",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
