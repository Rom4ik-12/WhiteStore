package com.whitestore.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.model.AppSummaryItem
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.ui.components.AppCardItem
import com.whitestore.ui.components.ExpressiveConnectedCategoryChip
import com.whitestore.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainCatalogScreen(
    viewModel: StoreViewModel,
    installer: AppInstaller,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val packageTrigger by installer.packageUpdateTrigger.collectAsState()

    // Home screen customisation preferences
    val showHeaderSubtitle by AppPreferences.homeShowHeaderSubtitle
    val showSearchBar by AppPreferences.homeShowSearchBar
    val showCategoryChips by AppPreferences.homeShowCategoryChips
    val showFeaturedCarousel by AppPreferences.homeShowFeaturedCarousel
    val showRecommendedSection by AppPreferences.homeShowRecommendedSection
    val showNewAppsSection by AppPreferences.homeShowNewAppsSection
    val showGamesSection by AppPreferences.homeShowGamesSection
    val catalogStyle by AppPreferences.homeCatalogStyle
    val showAppDeveloper by AppPreferences.homeShowAppDeveloper
    val showAppRating by AppPreferences.homeShowAppRating
    val showAppSize by AppPreferences.homeShowAppSize

    val popularTags = listOf("Банки", "Мессенджеры", "Госуслуги", "Доставка", "Музыка", "Маркетплейсы", "Игры", "Карты")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "WhiteStore",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (showHeaderSubtitle) {
                            Text(
                                text = "Чистый каталог приложений без рекламы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.reloadAllData() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить данные",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
            // Material 3 Expressive Search Bar
            if (showSearchBar) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Поиск приложений и игр...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            // Category quick tags with connected morphing style
            if (showCategoryChips) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(popularTags) { tag ->
                        val isSelected = uiState.selectedCategory == tag
                        ExpressiveConnectedCategoryChip(
                            text = tag,
                            isSelected = isSelected,
                            onClick = { viewModel.selectCategory(if (isSelected) "" else tag) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading && uiState.apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                } else if (uiState.apps.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val isDefaultCatalog = uiState.searchQuery.isBlank() && uiState.selectedCategory.isBlank()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // If standard catalog without search/filter, show Featured Carousel & Editorial Cards
                        if (isDefaultCatalog && uiState.apps.isNotEmpty()) {
                            val topFeatured = uiState.apps.take(4)
                            val remainingApps = uiState.apps.drop(if (showFeaturedCarousel) 4 else 0)

                            // 1. "В центре внимания" Horizontal Carousel
                            if (showFeaturedCarousel && topFeatured.isNotEmpty()) {
                                item {
                                    Column {
                                        Text(
                                            text = "В центре внимания",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            items(topFeatured) { app ->
                                                FeaturedAppCard(
                                                    item = app,
                                                    onClick = { onNavigateToDetail(app.packageName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Extra section: "Рекомендуемое"
                            if (showRecommendedSection && uiState.recommendedApps.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column {
                                        Text(
                                            text = "Рекомендуемое для вас",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            items(uiState.recommendedApps) { app ->
                                                CompactAppCard(
                                                    item = app,
                                                    onClick = { onNavigateToDetail(app.packageName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Extra section: "Новинки"
                            if (showNewAppsSection && uiState.newApps.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column {
                                        Text(
                                            text = "Новинки каталога",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            items(uiState.newApps) { app ->
                                                CompactAppCard(
                                                    item = app,
                                                    onClick = { onNavigateToDetail(app.packageName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Extra section: "Игры"
                            if (showGamesSection && uiState.gamesApps.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column {
                                        Text(
                                            text = "Популярные игры",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            items(uiState.gamesApps) { app ->
                                                CompactAppCard(
                                                    item = app,
                                                    onClick = { onNavigateToDetail(app.packageName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Section Title
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Каталог приложений",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${remainingApps.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 6. Regular catalog items (List or Grid)
                            if (catalogStyle == "GRID") {
                                val chunkedApps = remainingApps.chunked(2)
                                items(chunkedApps) { rowPair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        for (app in rowPair) {
                                            val isInstalled = remember(app.packageName, packageTrigger) {
                                                installer.isAppInstalled(app.packageName)
                                            }
                                            GridAppCardItem(
                                                item = app,
                                                isInstalled = isInstalled,
                                                showRating = showAppRating,
                                                onClick = { onNavigateToDetail(app.packageName) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowPair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(remainingApps, key = { it.packageName }) { app ->
                                    val isInstalled = remember(app.packageName, packageTrigger) {
                                        installer.isAppInstalled(app.packageName)
                                    }
                                    AppCardItem(
                                        item = app,
                                        isInstalled = isInstalled,
                                        showSubtitle = showAppDeveloper,
                                        showRating = showAppRating,
                                        showExtraInfo = showAppSize,
                                        onClick = { onNavigateToDetail(app.packageName) }
                                    )
                                }
                            }
                        } else {
                            // Search or filtered category view
                            item {
                                val title = if (uiState.searchQuery.isNotBlank()) {
                                    "Результаты поиска"
                                } else {
                                    uiState.selectedCategory
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            if (catalogStyle == "GRID") {
                                val chunkedApps = uiState.apps.chunked(2)
                                items(chunkedApps) { rowPair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        for (app in rowPair) {
                                            val isInstalled = remember(app.packageName, packageTrigger) {
                                                installer.isAppInstalled(app.packageName)
                                            }
                                            GridAppCardItem(
                                                item = app,
                                                isInstalled = isInstalled,
                                                showRating = showAppRating,
                                                onClick = { onNavigateToDetail(app.packageName) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowPair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(uiState.apps, key = { it.packageName }) { app ->
                                    val isInstalled = remember(app.packageName, packageTrigger) {
                                        installer.isAppInstalled(app.packageName)
                                    }
                                    AppCardItem(
                                        item = app,
                                        isInstalled = isInstalled,
                                        showSubtitle = showAppDeveloper,
                                        showRating = showAppRating,
                                        showExtraInfo = showAppSize,
                                        onClick = { onNavigateToDetail(app.packageName) }
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

/**
 * Editorial Featured Card for the top carousel on the main screen.
 */
@Composable
private fun FeaturedAppCard(
    item: AppSummaryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle expressive background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.iconUrl,
                    contentDescription = item.appName,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.companyName.isNullOrBlank()) {
                        Text(
                            text = item.companyName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (item.averageUserRating != null && item.averageUserRating > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", item.averageUserRating),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
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
 * Compact App Card for horizontal recommendation/category rows.
 */
@Composable
private fun CompactAppCard(
    item: AppSummaryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = item.iconUrl,
                contentDescription = item.appName,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.appName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.averageUserRating != null && item.averageUserRating > 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", item.averageUserRating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 2-Column Grid item card for alternative main catalog layout.
 */
@Composable
private fun GridAppCardItem(
    item: AppSummaryItem,
    isInstalled: Boolean,
    showRating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = item.iconUrl,
                    contentDescription = item.appName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .offset(x = 3.dp, y = 3.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Установлено",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.companyName.isNullOrBlank()) {
                Text(
                    text = item.companyName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showRating && item.averageUserRating != null && item.averageUserRating > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", item.averageUserRating),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
