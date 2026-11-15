package com.whitestore.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.data.prefs.RecentAppsFilter
import com.whitestore.ui.components.ConnectedButtonGroup
import com.whitestore.util.AppIcon
import com.whitestore.util.TimeFormatUtils
import com.whitestore.viewmodel.StoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentAppItem(
    val name: String,
    val packageName: String,
    val versionName: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isUpdated: Boolean,
    val timestamp: Long,
    val isInRuStore: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentAppsScreen(
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onNavigateToDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val packageTrigger by installer.packageUpdateTrigger.collectAsState()
    val ruStoreOnlyPref by AppPreferences.recentFilterRuStoreOnly
    val defaultFilterPref by AppPreferences.recentDefaultFilter

    var selectedFilter by remember { mutableStateOf(defaultFilterPref) }
    var appsList by remember { mutableStateOf<List<RecentAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Sync default filter if changed from settings
    LaunchedEffect(defaultFilterPref) {
        selectedFilter = defaultFilterPref
    }

    LaunchedEffect(packageTrigger) {
        isLoading = true
        val result = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            val candidateItems = packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
                .mapNotNull { appInfo ->
                    try {
                        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getPackageInfo(appInfo.packageName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(appInfo.packageName, 0)
                        }
                        val label = pm.getApplicationLabel(appInfo).toString()
                        val firstInstall = pInfo.firstInstallTime
                        val lastUpdate = pInfo.lastUpdateTime
                        // If lastUpdateTime is significantly larger than firstInstallTime (> 60s), consider it updated
                        val isUpdated = (lastUpdate - firstInstall) > 60_000L
                        val mostRecentTime = maxOf(firstInstall, lastUpdate)

                        RecentAppItem(
                            name = label,
                            packageName = appInfo.packageName,
                            versionName = pInfo.versionName ?: "",
                            firstInstallTime = firstInstall,
                            lastUpdateTime = lastUpdate,
                            isUpdated = isUpdated,
                            timestamp = mostRecentTime,
                            isInRuStore = false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedByDescending { it.timestamp }

            // Check RuStore status concurrently in background
            val semaphore = Semaphore(6)
            val checkedItems = candidateItems.map { item ->
                async {
                    val inStore = semaphore.withPermit {
                        viewModel.isPackageInRuStore(item.packageName)
                    }
                    item.copy(isInRuStore = inStore)
                }
            }.awaitAll()

            checkedItems
        }
        appsList = result
        isLoading = false
    }

    val filteredList = remember(appsList, selectedFilter, ruStoreOnlyPref) {
        appsList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                RecentAppsFilter.ALL -> true
                RecentAppsFilter.UPDATED -> item.isUpdated
                RecentAppsFilter.INSTALLED -> !item.isUpdated
            }
            val matchesRuStore = !ruStoreOnlyPref || item.isInRuStore
            matchesFilter && matchesRuStore
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru"))
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
                            RecentAppsFilter.ALL -> if (ruStoreOnlyPref) "Нет приложений из RuStore" else "Нет установленных приложений"
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
                                                    color = if (app.isUpdated) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }

                                        if (app.versionName.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "v${app.versionName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
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
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { installer.openApp(app.packageName) },
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Открыть",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
}
