package com.whitestore.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.installer.InstallState
import com.whitestore.data.model.AppUpdateInfo
import com.whitestore.data.updater.SelfUpdateState
import com.whitestore.ui.theme.AppThemeMode
import com.whitestore.ui.theme.ThemePreferences
import com.whitestore.viewmodel.StoreViewModel

data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onNavigateToSettings: () -> Unit,
    onNavigateToMyApps: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.3"
        } catch (e: Exception) {
            "1.0.3"
        }
    }
    val packageTrigger by installer.packageUpdateTrigger.collectAsState()
    val availableUpdates by viewModel.availableUpdates.collectAsState()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
    val selfUpdateState by viewModel.selfUpdateState.collectAsState()
    var showSelfUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selfUpdateState) {
        if (selfUpdateState is SelfUpdateState.UpdateAvailable) {
            showSelfUpdateDialog = true
        }
    }

    if (showSelfUpdateDialog && selfUpdateState is SelfUpdateState.UpdateAvailable) {
        val update = selfUpdateState as SelfUpdateState.UpdateAvailable
        AlertDialog(
            onDismissRequest = { showSelfUpdateDialog = false },
            title = {
                Text(
                    text = "Доступно обновление WhiteStore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Новая версия: v${update.latestVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = update.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSelfUpdateDialog = false
                        viewModel.downloadSelfUpdate(update.downloadUrl)
                    }
                ) {
                    Text("Обновить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelfUpdateDialog = false }) {
                    Text("Позже")
                }
            }
        )
    }

    // Count user-installed apps
    val installedAppsCount = remember(packageTrigger) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            packages.count { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
        } catch (e: Exception) {
            0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Моё",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
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
            // Top Grid: [Мои приложения] & [Обновления плитка]
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Мои приложения
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(175.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(onClick = onNavigateToMyApps),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "$installedAppsCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text(
                                    text = "Мои приложения",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Показать список",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // 2. Плиточка [Обновления] в точности как на скриншоте (иконки в ряд + бейдж + кнопка Обновить всё)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(175.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(onClick = onNavigateToUpdates),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Row of app icons overlapping or badge
                            if (availableUpdates.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val previewUpdates = availableUpdates.take(3)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-6).dp)
                                    ) {
                                        previewUpdates.forEach { update ->
                                            if (!update.iconUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = update.iconUrl,
                                                    contentDescription = update.appName,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else if (update.localIcon != null) {
                                                Image(
                                                    bitmap = update.localIcon.toBitmap().asImageBitmap(),
                                                    contentDescription = update.appName,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                )
                                            }
                                        }
                                    }

                                    if (availableUpdates.size > 3) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+${availableUpdates.size - 3}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Title & Count badge (e.g. "Обновления 1")
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Обновления",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (availableUpdates.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE53935)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${availableUpdates.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Action button inside the tile: "Обновить всё" or "Проверить"
                            if (availableUpdates.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.updateAll() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Обновить всё",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { viewModel.checkUpdates() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    enabled = !isCheckingUpdates,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    if (isCheckingUpdates) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "Проверить",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WhiteStore app update card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "WhiteStore",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhiteStore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            when (val state = selfUpdateState) {
                                is SelfUpdateState.UpdateAvailable -> {
                                    Text(
                                        text = "Доступна версия v${state.latestVersion}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                is SelfUpdateState.Downloading -> {
                                    Text(
                                        text = "Загрузка: ${(state.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "Версия $appVersionName • Актуальная",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        when (val state = selfUpdateState) {
                            is SelfUpdateState.UpdateAvailable -> {
                                Button(
                                    onClick = {
                                        viewModel.downloadSelfUpdate(state.downloadUrl)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Обновить", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            is SelfUpdateState.Downloading -> {
                                CircularProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                            is SelfUpdateState.Checking -> {
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }
                            else -> {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.checkSelfUpdate()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Проверить", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Settings row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToSettings),
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
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Настройки",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Тема оформления, о разработчике",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val packageTrigger by installer.packageUpdateTrigger.collectAsState()
    val availableUpdates by viewModel.availableUpdates.collectAsState()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
    val installStates by installer.installState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обновления (${availableUpdates.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (availableUpdates.isNotEmpty()) {
                        TextButton(onClick = { viewModel.updateAll() }) {
                            Text("Обновить всё")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (availableUpdates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Все приложения обновлены",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Новых версий пока нет",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(availableUpdates) { update ->
                    val installState = installStates[update.packageName] ?: InstallState.Idle
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigateToDetail(update.packageName) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!update.iconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = update.iconUrl,
                                    contentDescription = update.appName,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (update.localIcon != null) {
                                Image(
                                    bitmap = update.localIcon.toBitmap().asImageBitmap(),
                                    contentDescription = update.appName,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = update.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val sizeMb = if (update.fileSize > 0) String.format(" • %.1f МБ", update.fileSize / (1024.0 * 1024.0)) else ""
                                Text(
                                    text = "${update.currentVersionName} ➔ ${update.targetVersionName}$sizeMb",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            when (installState) {
                                is InstallState.Downloading -> {
                                    FilledTonalButton(
                                        onClick = {},
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { installState.progress },
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val pct = if (installState.progress > 0f) "${(installState.progress * 100).toInt()}%" else "..."
                                        Text(pct, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                is InstallState.Resolving -> {
                                    FilledTonalButton(
                                        onClick = {},
                                        enabled = false,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    }
                                }
                                is InstallState.ReadyToInstall -> {
                                    Button(
                                        onClick = {
                                            viewModel.startDownload(update.appId, update.packageName, update.appName)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Установить", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = {
                                            viewModel.startDownload(update.appId, update.packageName, update.appName)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Обновить", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(ThemePreferences.currentThemeMode.value) }

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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme selection card
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

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = currentMode == AppThemeMode.SYSTEM,
                            onClick = {
                                currentMode = AppThemeMode.SYSTEM
                                ThemePreferences.setThemeMode(context, AppThemeMode.SYSTEM)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text("Система", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = currentMode == AppThemeMode.LIGHT,
                            onClick = {
                                currentMode = AppThemeMode.LIGHT
                                ThemePreferences.setThemeMode(context, AppThemeMode.LIGHT)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text("Светлая", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = currentMode == AppThemeMode.DARK,
                            onClick = {
                                currentMode = AppThemeMode.DARK
                                ThemePreferences.setThemeMode(context, AppThemeMode.DARK)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text("Тёмная", maxLines = 1)
                        }
                    }
                }
            }

            // Developer / GitHub Card
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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

            // App Version Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WhiteStore v1.0.3 • Material Expressive",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppsScreen(
    installer: AppInstaller,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appsList = remember {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        packages.filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
            .map { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(appInfo.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(appInfo.packageName, 0)
                }
                InstalledAppInfo(
                    name = label,
                    packageName = appInfo.packageName,
                    versionName = pInfo.versionName ?: "",
                    icon = pm.getApplicationIcon(appInfo)
                )
            }.sortedBy { it.name.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои приложения (${appsList.size})") },
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
            items(appsList) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { installer.openApp(app.packageName) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (app.versionName.isNotBlank()) {
                                Text(
                                    text = "Версия ${app.versionName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { installer.openApp(app.packageName) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Открыть", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
