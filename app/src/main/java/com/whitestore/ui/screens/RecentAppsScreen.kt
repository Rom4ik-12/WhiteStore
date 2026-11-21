package com.whitestore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.model.RecentAppItem
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.data.prefs.RecentAppsFilter
import com.whitestore.data.prefs.RecentHistoryDepth
import com.whitestore.ui.components.ConnectedButtonGroup
import com.whitestore.util.AppIcon
import com.whitestore.util.TimeFormatUtils
import com.whitestore.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecentSortOrder(val title: String) {
    TIME("По времени"),
    SIZE("По размеру")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentAppsScreen(
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val appsList by viewModel.recentApps.collectAsState()
    val isLoading by viewModel.isRecentLoading.collectAsState()
    val ruStoreOnlyPref by AppPreferences.recentFilterRuStoreOnly
    val defaultFilterPref by AppPreferences.recentDefaultFilter
    val historyDepthPref by AppPreferences.recentHistoryDepth

    var selectedFilter by remember { mutableStateOf(defaultFilterPref) }
    var sortOrder by remember { mutableStateOf(RecentSortOrder.TIME) }

    // Sync default filter if changed from settings
    LaunchedEffect(defaultFilterPref) {
        selectedFilter = defaultFilterPref
    }

    // Trigger load once when opened if not loaded yet; subsequent openings are instant from cache
    LaunchedEffect(Unit) {
        viewModel.loadRecentApps(force = false)
    }

    val filteredList = remember(appsList, selectedFilter, ruStoreOnlyPref, historyDepthPref, sortOrder) {
        val now = System.currentTimeMillis()
        val minTimestamp = if (historyDepthPref.durationMs > 0L) now - historyDepthPref.durationMs else 0L

        val filtered = appsList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                RecentAppsFilter.ALL -> true
                RecentAppsFilter.UPDATED -> item.isUpdated
                RecentAppsFilter.INSTALLED -> !item.isUpdated
            }
            val matchesRuStore = !ruStoreOnlyPref || item.isInRuStore
            val matchesDepth = historyDepthPref.durationMs == 0L || item.timestamp >= minTimestamp
            matchesFilter && matchesRuStore && matchesDepth
        }

        when (sortOrder) {
            RecentSortOrder.TIME -> filtered.sortedByDescending { it.timestamp }
            RecentSortOrder.SIZE -> filtered.sortedByDescending { it.appSizeBytes }
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Недавние",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = if (sortOrder == RecentSortOrder.TIME) Icons.Default.Schedule else Icons.Default.Storage,
                                contentDescription = "Сортировка"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Сначала недавние") },
                                onClick = {
                                    sortOrder = RecentSortOrder.TIME
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Сначала тяжелые (по размеру)") },
                                onClick = {
                                    sortOrder = RecentSortOrder.SIZE
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Storage, contentDescription = null)
                                }
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.loadRecentApps(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedFilter) {
                            RecentAppsFilter.ALL -> if (ruStoreOnlyPref) "Нет приложений из RuStore за этот период" else "Нет событий за этот период"
                            RecentAppsFilter.UPDATED -> "Обновленные приложения не найдены"
                            RecentAppsFilter.INSTALLED -> "Установленные приложения не найдены"
                        },
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
                    items(filteredList, key = { it.packageName }) { app ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onNavigateToDetail(app.packageName) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                                    size = 50.dp,
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

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (app.isUpdated) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (app.isUpdated) Icons.Default.SystemUpdate else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(11.dp),
                                                    tint = if (app.isUpdated) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (app.isUpdated) "Обновлено" else "Установлено",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (app.isUpdated) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        if (app.versionName.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "v${app.versionName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val formattedDate = remember(app.timestamp) {
                                        try {
                                            dateFormatter.format(Date(app.timestamp))
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (app.appSizeBytes > 0L) {
                                            val formattedSize = remember(app.appSizeBytes) {
                                                android.text.format.Formatter.formatFileSize(context, app.appSizeBytes)
                                            }
                                            Text(
                                                text = " • $formattedSize",
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
}
