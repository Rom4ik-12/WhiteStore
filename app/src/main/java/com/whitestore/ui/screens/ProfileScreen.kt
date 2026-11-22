package com.whitestore.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.installer.InstallState
import com.whitestore.data.model.AppUpdateInfo
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.data.updater.SelfUpdateState
import com.whitestore.ui.components.ConnectedButtonGroup
import com.whitestore.ui.theme.AppThemeMode
import com.whitestore.ui.theme.ThemePreferences
import com.whitestore.util.AppIcon
import com.whitestore.util.StorageManager
import com.whitestore.util.TimeFormatUtils
import com.whitestore.viewmodel.StoreViewModel

data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val appSizeBytes: Long = 0L,
    val installTime: Long = 0L
)

enum class MyAppsSortOrder(val title: String) {
    NAME("По имени"),
    SIZE("По размеру"),
    DATE("По дате")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val hasCheckedUpdates by viewModel.hasCheckedUpdates.collectAsState()
    val selfUpdateState by viewModel.selfUpdateState.collectAsState()
    val lastCheckTime by AppPreferences.lastUpdateCheckTime
    val showUpdateCountBadge by AppPreferences.showUpdateCountBadge
    val updateBadgeShape by AppPreferences.updateBadgeShape
    val updateBadgeColor by AppPreferences.updateBadgeColor
    val updateBadgeMatchTheme by AppPreferences.updateBadgeMatchTheme

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
                            // Row of app icons overlapping by 1/3 or badge
                            if (availableUpdates.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val previewUpdates = availableUpdates.take(4)
                                    val itemShape = RoundedCornerShape(9.dp)
                                    val borderColor = MaterialTheme.colorScheme.surfaceContainer
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-11).dp)
                                    ) {
                                        previewUpdates.forEachIndexed { index, update ->
                                            Box(
                                                modifier = Modifier
                                                    .zIndex((index + 1).toFloat())
                                                    .size(34.dp)
                                                    .clip(itemShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(2.dp, borderColor, itemShape)
                                            ) {
                                                if (!update.iconUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = update.iconUrl,
                                                        contentDescription = update.appName,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(itemShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else if (update.localIconBitmap != null) {
                                                    Image(
                                                        bitmap = update.localIconBitmap,
                                                        contentDescription = update.appName,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(itemShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (availableUpdates.size > 4) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+${availableUpdates.size - 4}",
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
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Обновления",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (showUpdateCountBadge && availableUpdates.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val badgeShape = com.whitestore.ui.theme.M3Shapes.getBadgeShape(updateBadgeShape)
                                        val badgeBackground = if (updateBadgeMatchTheme) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color(updateBadgeColor.colorValue)
                                        }
                                        val badgeTextColor = if (updateBadgeMatchTheme) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            Color.White
                                        }
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                                                .clip(badgeShape)
                                                .background(badgeBackground)
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${availableUpdates.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextColor
                                            )
                                        }
                                    }
                                }

                                if (lastCheckTime > 0L) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = TimeFormatUtils.formatRelativeTime(lastCheckTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Обновить всё",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
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
                                    colors = if (hasCheckedUpdates) {
                                        ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    } else ButtonDefaults.filledTonalButtonColors(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    if (isCheckingUpdates) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (hasCheckedUpdates) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Обновлений нет",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
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
                WhiteStoreUpdateCard(
                    selfUpdateState = selfUpdateState,
                    appVersionName = appVersionName,
                    lastCheckTime = lastCheckTime,
                    onDownload = { viewModel.downloadSelfUpdate(it) },
                    onCheck = { viewModel.checkSelfUpdate() }
                )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WhiteStoreUpdateCard(
    selfUpdateState: SelfUpdateState,
    appVersionName: String,
    lastCheckTime: Long,
    onDownload: (String) -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChangelogExpanded by rememberSaveable { mutableStateOf(false) }
    val isUpdate = selfUpdateState is SelfUpdateState.UpdateAvailable

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUpdate) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isUpdate) Modifier.clickable { isChangelogExpanded = !isChangelogExpanded }
                        else Modifier
                    )
                    .padding(16.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WhiteStore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isUpdate) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isChangelogExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isChangelogExpanded) "Свернуть" else "Что нового",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    when (val state = selfUpdateState) {
                        is SelfUpdateState.UpdateAvailable -> {
                            Text(
                                text = "Доступна v${state.latestVersion} • " + (if (isChangelogExpanded) "Свернуть ченджлог" else "Что нового"),
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
                            val lastCheckInfo = if (lastCheckTime > 0L) " • ${TimeFormatUtils.formatRelativeTime(lastCheckTime)}" else ""
                            Text(
                                text = "Версия $appVersionName$lastCheckInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                when (val state = selfUpdateState) {
                    is SelfUpdateState.UpdateAvailable -> {
                        FilledIconButton(
                            onClick = { onDownload(state.downloadUrl) },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Обновить WhiteStore",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    is SelfUpdateState.Downloading -> {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator(modifier = Modifier.size(36.dp))
                        }
                    }
                    is SelfUpdateState.Checking -> {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    is SelfUpdateState.UpToDate -> {
                        FilledTonalIconButton(
                            onClick = onCheck,
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Версия актуальна",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    else -> {
                        FilledTonalIconButton(
                            onClick = onCheck,
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Проверить обновления",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (isUpdate && isChangelogExpanded) {
                val state = selfUpdateState as SelfUpdateState.UpdateAvailable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Что нового в v${state.latestVersion}:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            FormattedMarkdownChangelog(markdownText = state.releaseNotes.trim())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMarkdownChangelog(
    markdownText: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(markdownText) {
        markdownText.lines()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
            } else if (line.startsWith("###") || line.startsWith("##") || line.startsWith("#")) {
                val header = line.trimStart('#').trim()
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            } else {
                val isBullet = line.startsWith("- ") || line.startsWith("* ")
                val cleanContent = if (isBullet) line.substring(2).trim() else line

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (isBullet) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    val annotated = remember(cleanContent) {
                        buildMarkdownAnnotatedString(cleanContent)
                    }

                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

fun buildMarkdownAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("""(\*\*(.*?)\*\*|`([^`]+)`)""")
        val matches = regex.findAll(text)

        for (match in matches) {
            val range = match.range
            if (range.first > cursor) {
                append(text.substring(cursor, range.first))
            }

            val boldContent = match.groups[2]?.value
            val codeContent = match.groups[3]?.value

            if (boldContent != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldContent)
                }
            } else if (codeContent != null) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append(codeContent)
                }
            }
            cursor = range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdatesScreen(
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onNavigateToIgnoredApps: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val packageTrigger by installer.packageUpdateTrigger.collectAsState()
    val availableUpdates by viewModel.availableUpdates.collectAsState()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
    val installStates by installer.installState.collectAsState()
    val lastCheckTime by AppPreferences.lastUpdateCheckTime
    val showInitialVersion by AppPreferences.showInitialVersion
    val useIconUpdateButton by AppPreferences.useIconUpdateButton
    val selfUpdateState by viewModel.selfUpdateState.collectAsState()
    val context = LocalContext.current
    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.3"
        } catch (e: Exception) {
            "1.0.3"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Обновления (${availableUpdates.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (lastCheckTime > 0L) {
                            Text(
                                text = "Проверено: ${TimeFormatUtils.formatRelativeTime(lastCheckTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkUpdates() },
                        enabled = !isCheckingUpdates
                    ) {
                        if (isCheckingUpdates) {
                            LoadingIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Проверить обновления"
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToIgnoredApps
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Исключения обновлений"
                        )
                    }
                    if (availableUpdates.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateAll() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Обновить всё",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val isSelfUpdateAvailable = selfUpdateState is SelfUpdateState.UpdateAvailable

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSelfUpdateAvailable) {
                item {
                    WhiteStoreUpdateCard(
                        selfUpdateState = selfUpdateState,
                        appVersionName = appVersionName,
                        lastCheckTime = lastCheckTime,
                        onDownload = { viewModel.downloadSelfUpdate(it) },
                        onCheck = { viewModel.checkSelfUpdate() }
                    )
                }
            }

            if (availableUpdates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isSelfUpdateAvailable) 32.dp else 120.dp),
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
                                text = "Все установленные приложения обновлены",
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
                }
            } else {
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
                            } else if (update.localIconBitmap != null) {
                                Image(
                                    bitmap = update.localIconBitmap,
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
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sizeText = if (update.fileSize > 0) String.format("%.1f МБ", update.fileSize / (1024.0 * 1024.0)) else ""
                                if (showInitialVersion) {
                                    Text(
                                        text = "${update.currentVersionName} ➔ ${update.targetVersionName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (sizeText.isNotEmpty()) {
                                        Text(
                                            text = sizeText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    val sizeSuffix = if (sizeText.isNotEmpty()) " • $sizeText" else ""
                                    Text(
                                        text = "v${update.targetVersionName}$sizeSuffix",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    AppPreferences.toggleIgnorePackage(context, update.packageName)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Скрыть обновление",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            when (installState) {
                                is InstallState.Downloading -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (useIconUpdateButton) {
                                            FilledTonalIconButton(
                                                onClick = { viewModel.pauseDownload(update.packageName) },
                                                modifier = Modifier.size(40.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Pause,
                                                    contentDescription = "Приостановить загрузку",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = { viewModel.pauseDownload(update.packageName) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                val progressText = StorageManager.formatDownloadProgress(
                                                    progress = installState.progress,
                                                    downloadedBytes = installState.downloadedBytes,
                                                    totalBytes = installState.totalBytes,
                                                    speedBytesPerSec = installState.speedBytesPerSec
                                                )
                                                Text("Пауза $progressText", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }

                                        FilledTonalIconButton(
                                            onClick = { viewModel.cancelDownload(update.packageName) },
                                            modifier = Modifier.size(if (useIconUpdateButton) 40.dp else 36.dp),
                                            shape = RoundedCornerShape(if (useIconUpdateButton) 12.dp else 10.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Отменить загрузку",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                is InstallState.Paused -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (useIconUpdateButton) {
                                            FilledIconButton(
                                                onClick = { viewModel.startDownload(update.appId, update.packageName, update.appName) },
                                                modifier = Modifier.size(40.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Продолжить загрузку",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.startDownload(update.appId, update.packageName, update.appName) },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                val progressText = StorageManager.formatDownloadProgress(
                                                    progress = installState.progress,
                                                    downloadedBytes = installState.downloadedBytes,
                                                    totalBytes = installState.totalBytes,
                                                    speedBytesPerSec = 0L,
                                                    showSpeed = false
                                                )
                                                Text("Далее $progressText", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }

                                        FilledTonalIconButton(
                                            onClick = { viewModel.cancelDownload(update.packageName) },
                                            modifier = Modifier.size(if (useIconUpdateButton) 40.dp else 36.dp),
                                            shape = RoundedCornerShape(if (useIconUpdateButton) 12.dp else 10.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Отменить загрузку",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                is InstallState.Resolving -> {
                                    if (useIconUpdateButton) {
                                        FilledTonalIconButton(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                is InstallState.ReadyToInstall -> {
                                    if (useIconUpdateButton) {
                                        FilledIconButton(
                                            onClick = {
                                                installer.installDownloadedApk(update.packageName)
                                            },
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Установить",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                installer.installDownloadedApk(update.packageName)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Установить", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                else -> {
                                    if (useIconUpdateButton) {
                                        FilledIconButton(
                                            onClick = {
                                                viewModel.startDownload(update.appId, update.packageName, update.appName)
                                            },
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Обновить",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyAppsScreen(
    installer: AppInstaller,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var appsList by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(MyAppsSortOrder.NAME) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAppForBackup by remember { mutableStateOf<InstalledAppInfo?>(null) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            packages.filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val pInfo = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getPackageInfo(appInfo.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(appInfo.packageName, 0)
                        }
                    } catch (e: Exception) {
                        null
                    }
                    val size = try {
                        java.io.File(appInfo.sourceDir).length()
                    } catch (e: Exception) {
                        0L
                    }
                    val time = maxOf(pInfo?.firstInstallTime ?: 0L, pInfo?.lastUpdateTime ?: 0L)
                    InstalledAppInfo(
                        name = label,
                        packageName = appInfo.packageName,
                        versionName = pInfo?.versionName ?: "",
                        appSizeBytes = size,
                        installTime = time
                    )
                }
        }
        appsList = result
        isLoading = false
    }

    val displayList = remember(appsList, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            appsList
        } else {
            appsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortOrder) {
            MyAppsSortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
            MyAppsSortOrder.SIZE -> filtered.sortedByDescending { it.appSizeBytes }
            MyAppsSortOrder.DATE -> filtered.sortedByDescending { it.installTime }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои приложения (${displayList.size})") },
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
        ) {
            // Search and sort bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск среди установленных...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                ConnectedButtonGroup(
                    items = MyAppsSortOrder.entries,
                    selectedItem = sortOrder,
                    onItemSelected = { sortOrder = it },
                    labelProvider = { it.title },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            } else if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Нет установленных приложений",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayList, key = { it.packageName }) { app ->
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
                                AppIcon(
                                    packageName = app.packageName,
                                    size = 48.dp,
                                    shapeRadius = 14.dp
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (app.versionName.isNotBlank()) {
                                            Text(
                                                text = "v${app.versionName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (app.appSizeBytes > 0L) {
                                            val formattedSize = remember(app.appSizeBytes) {
                                                android.text.format.Formatter.formatFileSize(context, app.appSizeBytes)
                                            }
                                            if (app.versionName.isNotBlank()) {
                                                Text(
                                                    text = " • ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            Text(
                                                text = formattedSize,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { selectedAppForBackup = app },
                                        modifier = Modifier.size(38.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SaveAlt,
                                            contentDescription = "Бэкап / Поделиться APK",
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = { installer.openAppDetailsSettings(app.packageName) },
                                        modifier = Modifier.size(38.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "О приложении",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = { installer.openApp(app.packageName) },
                                        modifier = Modifier.size(38.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Открыть",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedAppForBackup != null) {
        val app = selectedAppForBackup!!
        ModalBottomSheet(
            onDismissRequest = { selectedAppForBackup = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        packageName = app.packageName,
                        size = 52.dp,
                        shapeRadius = 14.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${app.packageName} • v${app.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.appSizeBytes > 0L) {
                            val formattedSize = remember(app.appSizeBytes) {
                                android.text.format.Formatter.formatFileSize(context, app.appSizeBytes)
                            }
                            Text(
                                text = "Размер APK: $formattedSize",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Button(
                    onClick = {
                        val targetApp = app
                        selectedAppForBackup = null
                        coroutineScope.launch {
                            val saved = StorageManager.backupAppApk(context, targetApp.packageName, targetApp.name, targetApp.versionName)
                            if (saved != null) {
                                Toast.makeText(context, "APK сохранён в Загрузки:\n${saved.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Не удалось извлечь APK", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Сохранить APK в Загрузки")
                }

                FilledTonalButton(
                    onClick = {
                        val targetApp = app
                        selectedAppForBackup = null
                        StorageManager.shareInstalledAppApk(context, targetApp.packageName)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Поделиться файлом APK")
                }

                OutlinedButton(
                    onClick = {
                        val targetApp = app
                        selectedAppForBackup = null
                        installer.openAppDetailsSettings(targetApp.packageName)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Настройки приложения")
                }
            }
        }
    }
}
