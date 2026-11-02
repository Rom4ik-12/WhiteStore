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
import kotlinx.coroutines.withContext
import com.whitestore.data.prefs.AutoCheckInterval
import com.whitestore.data.updater.UpdateCheckReceiver
import com.whitestore.ui.components.ConnectedButtonGroup
import com.whitestore.ui.theme.AccentColor
import com.whitestore.ui.theme.AppThemeMode
import com.whitestore.ui.theme.ThemePreferences
import com.whitestore.util.AppIcon
import com.whitestore.util.TimeFormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToIgnoredApps: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(ThemePreferences.currentThemeMode.value) }
    var dynamicColorEnabled by remember { mutableStateOf(ThemePreferences.dynamicColorEnabled.value) }
    var currentAccentColor by remember { mutableStateOf(ThemePreferences.currentAccentColor.value) }
    var openAfterInstall by remember { mutableStateOf(AppPreferences.openAfterInstall.value) }
    var currentInterval by remember { mutableStateOf(AppPreferences.autoCheckInterval.value) }
    var autoCheckNotify by remember { mutableStateOf(AppPreferences.autoCheckNotify.value) }
    val lastCheckTime by AppPreferences.lastUpdateCheckTime
    val ignoredPackages by AppPreferences.ignoredPackages
    val ignoredCount = ignoredPackages.size

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
            // 1. Тема оформления
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

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

                        // Если динамические цвета выключены (или на Android < 12) - показываем выбор акцентного цвета
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

            // 2. Установка приложений (Открыть сразу после установки)
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

            // 3. Автопроверка обновлений
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

            // 4. Исключения обновлений
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

            // 5. Поддержать проект донатом (CloudTips)
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
