package com.whitestore.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val isInstalled = remember(packageName, packageTrigger) {
        installer.isAppInstalled(packageName)
    }
    val installState = installStates[packageName] ?: InstallState.Idle

    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.appName ?: "Приложение") },
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
        if (isDetailLoading && detail == null) {
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
                        if (isInstalled) {
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
                            Button(
                                onClick = onAction,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Установить сейчас", style = MaterialTheme.typography.titleMedium)
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
                            if (isInstalled) {
                                FilledTonalButton(
                                    onClick = onAction,
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Открыть", style = MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                Button(
                                    onClick = onAction,
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Скачать", style = MaterialTheme.typography.titleMedium)
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
