package com.whitestore.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.installer.InstallState
import com.whitestore.ui.components.FullscreenImageViewer
import com.whitestore.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onBack: () -> Unit
) {
    LaunchedEffect(packageName) {
        viewModel.loadAppDetail(packageName)
    }

    val detail by viewModel.currentDetail.collectAsState()
    val isDetailLoading by viewModel.isDetailLoading.collectAsState()
    val installStates by installer.installState.collectAsState()
    val packageTrigger by installer.packageUpdateTrigger.collectAsState()
    val context = LocalContext.current

    val isInstalled = remember(packageName, packageTrigger) {
        installer.isAppInstalled(packageName)
    }

    val installedVersionCode = remember(packageName, packageTrigger, isInstalled) {
        if (!isInstalled) null
        else {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(packageName, 0).longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val hasUpdate = remember(installedVersionCode, detail?.versionCode) {
        val remoteCode = detail?.versionCode
        isInstalled && installedVersionCode != null && remoteCode != null && remoteCode > installedVersionCode
    }

    val installState = installStates[packageName] ?: InstallState.Idle

    val summary = remember(packageName) { viewModel.appSummaryCache[packageName] }

    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.appName ?: summary?.appName ?: "Приложение") },
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
        if (detail == null && isDetailLoading && summary != null) {
            // Instant render with summary and inline loading indicator
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = summary.iconUrl,
                            contentDescription = summary.appName,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = summary.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (!summary.companyName.isNullOrBlank()) {
                                Text(
                                    text = summary.companyName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!summary.shortDescription.isNullOrBlank()) {
                                Text(
                                    text = summary.shortDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            }
        } else if (isDetailLoading && detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        } else if (detail != null) {
            val app = detail!!
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = app.iconUrl,
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            app.companyName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!app.versionName.isNullOrBlank()) {
                                Text(
                                    text = "Версия ${app.versionName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Stats Chips (Material Expressive)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Downloads
                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                val dlText = if ((app.downloads ?: 0) > 1000000) {
                                    "${(app.downloads ?: 0) / 1000000} млн+"
                                } else if ((app.downloads ?: 0) > 1000) {
                                    "${(app.downloads ?: 0) / 1000} тыс+"
                                } else {
                                    "${app.downloads ?: 0}"
                                }
                                Text(
                                    text = dlText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Загрузок", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Size
                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                val sizeMb = ((app.fileSize ?: 0) / (1024.0 * 1024.0))
                                Text(
                                    text = String.format("%.1f МБ", sizeMb),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Размер", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Age restriction
                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.ageRestriction?.category ?: "0+",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Возраст", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                // Primary Install / Open Button (Large Expressive with clear statuses)
                item {
                    val onAction = {
                        if (hasUpdate) {
                            viewModel.startDownload(app.appId, app.packageName, app.appName)
                        } else if (isInstalled) {
                            installer.openApp(app.packageName)
                        } else {
                            viewModel.startDownload(app.appId, app.packageName, app.appName)
                        }
                    }

                    when (installState) {
                        is InstallState.Resolving -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Подготовка ссылки...", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        is InstallState.Downloading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {},
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    val pct = if (installState.progress > 0f) "${(installState.progress * 100).toInt()}%" else "Загрузка..."
                                    val downloadedMb = installState.downloadedBytes / (1024.0 * 1024.0)
                                    val totalMb = installState.totalBytes / (1024.0 * 1024.0)
                                    val sizeText = if (totalMb > 0) String.format("(%.1f/%.1f)", downloadedMb, totalMb) else ""
                                    Text("Скачивание $pct $sizeText", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                }

                                // Official M3 Expressive Linear Wavy Progress Indicator
                                if (installState.progress > 0f) {
                                    LinearWavyProgressIndicator(
                                        progress = { installState.progress },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                } else {
                                    LinearWavyProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                }
                            }
                        }
                        is InstallState.Extracting -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Распаковка APK...", style = MaterialTheme.typography.titleMedium)
                                }

                                LinearWavyProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }
                        }
                        is InstallState.ReadyToInstall -> {
                            val coroutineScope = rememberCoroutineScope()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { installer.installDownloadedApk(app.packageName) },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Установить сейчас", style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val apkFile = installer.getApkFile(app.packageName)
                                            if (apkFile != null) {
                                                coroutineScope.launch {
                                                    val saved = com.whitestore.util.StorageManager.saveApkToDownloads(context, apkFile, "${app.appName}_${app.versionName}.apk")
                                                    if (saved != null) {
                                                        android.widget.Toast.makeText(context, "Сохранено в Загрузки:\n${saved.name}", android.widget.Toast.LENGTH_LONG).show()
                                                    } else {
                                                        com.whitestore.util.StorageManager.shareApk(context, apkFile)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Сохранить APK", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val apkFile = installer.getApkFile(app.packageName)
                                            if (apkFile != null) {
                                                com.whitestore.util.StorageManager.shareApk(context, apkFile)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Поделиться", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        is InstallState.Error -> {
                            Button(
                                onClick = onAction,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Ошибка. Повторить?", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        is InstallState.Idle -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mirror delete button on the opposite side if app is installed
                                if (isInstalled) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            installer.uninstallApp(app.packageName)
                                        },
                                        modifier = Modifier.size(54.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить приложение",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }

                                if (hasUpdate) {
                                    Button(
                                        onClick = onAction,
                                        modifier = Modifier.weight(1f).height(54.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Обновить", style = MaterialTheme.typography.titleMedium)
                                    }
                                } else if (isInstalled) {
                                    FilledTonalButton(
                                        onClick = onAction,
                                        modifier = Modifier.weight(1f).height(54.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Открыть", style = MaterialTheme.typography.titleMedium)
                                    }
                                } else {
                                    Button(
                                        onClick = onAction,
                                        modifier = Modifier.weight(1f).height(54.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Скачать", style = MaterialTheme.typography.titleMedium)
                                    }
                                }

                                // Secondary button to download APK directly as a file to device storage
                                FilledTonalIconButton(
                                    onClick = {
                                        viewModel.startDownload(
                                            appId = app.appId,
                                            packageName = app.packageName,
                                            appName = app.appName,
                                            saveOnly = true,
                                            versionName = app.versionName
                                        )
                                    },
                                    modifier = Modifier.size(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SaveAlt,
                                        contentDescription = "Скачать .apk файл на устройство",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Screenshots Carousel
                if (app.fileUrls.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "Скриншоты",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(app.fileUrls) { index, screenshot ->
                                    AsyncImage(
                                        model = screenshot.fileUrl,
                                        contentDescription = "Скриншот",
                                        modifier = Modifier
                                            .height(280.dp)
                                            .width(160.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                selectedImageIndex = index
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                // What's New Section
                if (!app.whatsNew.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Что нового",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = app.whatsNew,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Description
                item {
                    Column {
                        Text(
                            text = "О приложении",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = app.fullDescription ?: app.shortDescription ?: "Описание отсутствует",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Reviews & Ratings Section (Отзывы и оценки)
                item {
                    val ratingVal = app.rating?.average ?: 0.0
                    val votesVal = app.rating?.votes ?: 0L
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Оценки и отзывы",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RuStore",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (ratingVal > 0.0) String.format(Locale.getDefault(), "%.1f", ratingVal) else "—",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row {
                                        repeat(5) { starIndex ->
                                            val starFilled = (starIndex + 1) <= ratingVal.toInt()
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (starFilled) Color(0xFFFFB800) else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (votesVal > 0) "$votesVal оценок" else "Нет оценок",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                // Rating bar breakdowns visual
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(5, 4, 3, 2, 1).forEach { star ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "$star",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(10.dp)
                                            )
                                            val barFraction = if (ratingVal > 0.0) {
                                                when (star) {
                                                    5 -> 0.70f * (ratingVal / 5f).toFloat()
                                                    4 -> 0.20f * (ratingVal / 5f).toFloat()
                                                    3 -> 0.08f
                                                    2 -> 0.04f
                                                    else -> 0.05f
                                                }
                                            } else 0f
                                            LinearProgressIndicator(
                                                progress = { barFraction.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = Color(0xFFFFB800),
                                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rustore.ru/catalog/app/${app.packageName}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Читать все отзывы на RuStore",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Не удалось загрузить данные приложения",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

        // Fullscreen Screenshot Viewer overlaid inside the same hierarchy for seamless animated transition
        if (detail != null) {
            val imageUrls = remember(detail) { detail!!.fileUrls.map { it.fileUrl } }
            FullscreenImageViewer(
                images = imageUrls,
                initialIndex = selectedImageIndex ?: 0,
                isVisible = selectedImageIndex != null,
                onDismiss = { selectedImageIndex = null }
            )
        }
    }
}
