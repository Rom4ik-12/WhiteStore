package com.whitestore.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.model.SuggestItem
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
                        Text(
                            text = "Чистый каталог приложений без рекламы",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Category quick tags with connected morphing style
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

            Spacer(modifier = Modifier.height(6.dp))

            // Main Content Area (Apps List)
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            val title = if (uiState.searchQuery.isNotBlank()) {
                                "Результаты поиска"
                            } else if (uiState.selectedCategory.isNotBlank()) {
                                uiState.selectedCategory
                            } else {
                                "Популярное"
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(uiState.apps, key = { it.packageName }) { app ->
                            val isInstalled = remember(app.packageName, packageTrigger) {
                                installer.isAppInstalled(app.packageName)
                            }

                            // Clean card without giant download button, opens details
                            AppCardItem(
                                item = app,
                                isInstalled = isInstalled,
                                onClick = { onNavigateToDetail(app.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}
