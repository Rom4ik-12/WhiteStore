package com.whitestore.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitestore.data.api.RuStoreApiService
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.installer.InstallState
import com.whitestore.data.model.AppDetail
import com.whitestore.data.model.AppSummaryItem
import com.whitestore.data.model.AppUpdateInfo
import com.whitestore.data.model.SuggestItem
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.data.updater.SelfUpdateManager
import com.whitestore.data.updater.SelfUpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StoreUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val suggestions: List<SuggestItem> = emptyList(),
    val apps: List<AppSummaryItem> = emptyList(),
    val error: String? = null
)

class StoreViewModel(
    private val apiService: RuStoreApiService,
    private val installer: AppInstaller,
    private val selfUpdateManager: SelfUpdateManager,
    private val context: Context
) : ViewModel() {

    private val TAG = "StoreViewModel"

    val selfUpdateState: StateFlow<SelfUpdateState> = selfUpdateManager.updateState

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _currentDetail = MutableStateFlow<AppDetail?>(null)
    val currentDetail: StateFlow<AppDetail?> = _currentDetail.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    // Pre-cache item summaries: packageName -> AppSummaryItem
    val appSummaryCache = java.util.concurrent.ConcurrentHashMap<String, AppSummaryItem>()

    // In-memory cache for app details: packageName -> AppDetail
    private val appDetailCache = java.util.concurrent.ConcurrentHashMap<String, AppDetail>()

    // Updates state
    private val _availableUpdates = MutableStateFlow<List<AppUpdateInfo>>(emptyList())
    val availableUpdates: StateFlow<List<AppUpdateInfo>> = _availableUpdates.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _hasCheckedUpdates = MutableStateFlow(false)
    val hasCheckedUpdates: StateFlow<Boolean> = _hasCheckedUpdates.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialCatalog()
        checkUpdates()
        checkSelfUpdate()

        // Automatically filter updates when ignored packages change
        viewModelScope.launch {
            snapshotFlow { AppPreferences.ignoredPackages.value }
                .collect { ignored ->
                    _availableUpdates.value = _availableUpdates.value.filter { !ignored.contains(it.packageName) }
                }
        }

        // Listen for package install/update/remove events to immediately remove updated apps from availableUpdates
        viewModelScope.launch {
            installer.packageUpdateTrigger.collect { trigger ->
                if (trigger > 0) {
                    filterUpdatedApps()
                }
            }
        }
    }

    private fun filterUpdatedApps() {
        val currentUpdates = _availableUpdates.value
        if (currentUpdates.isEmpty()) return
        val pm = context.packageManager
        val remainingUpdates = currentUpdates.filter { update ->
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(update.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(update.packageName, 0)
                }
                val localCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                update.targetVersionCode > localCode
            } catch (e: Exception) {
                false
            }
        }
        _availableUpdates.value = remainingUpdates
    }

    fun checkSelfUpdate() {
        viewModelScope.launch {
            selfUpdateManager.checkForUpdates()
        }
    }

    fun downloadSelfUpdate(downloadUrl: String) {
        viewModelScope.launch {
            selfUpdateManager.downloadAndInstallUpdate(downloadUrl)
        }
    }

    fun loadInitialCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = apiService.searchApps(query = "Банк", pageNumber = 0, pageSize = 25)
                val apps = result.content
                apps.forEach { appSummaryCache[it.packageName] = it }
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun reloadAllData() {
        appDetailCache.clear()
        if (_uiState.value.searchQuery.isNotBlank()) {
            performSearch(_uiState.value.searchQuery)
        } else if (_uiState.value.selectedCategory.isNotBlank()) {
            performSearch(_uiState.value.selectedCategory)
        } else {
            loadInitialCatalog()
        }
        checkUpdates()
        checkSelfUpdate()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = newQuery)
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                suggestions = emptyList()
            )
            loadInitialCatalog()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearching = true)
            try {
                val suggestBody = apiService.getSearchSuggestions(newQuery)
                _uiState.value = _uiState.value.copy(
                    suggestions = suggestBody.suggests
                )

                val appsBody = apiService.searchApps(newQuery, pageNumber = 0, pageSize = 20)
                if (appsBody.content.isNotEmpty()) {
                    appsBody.content.forEach { appSummaryCache[it.packageName] = it }
                    _uiState.value = _uiState.value.copy(apps = appsBody.content)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                isSearching = false,
                isLoading = true,
                suggestions = emptyList()
            )
            try {
                val result = apiService.searchApps(query, pageNumber = 0, pageSize = 30)
                result.content.forEach { appSummaryCache[it.packageName] = it }
                _uiState.value = _uiState.value.copy(
                    apps = result.content,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        if (category.isBlank()) {
            loadInitialCatalog()
        } else {
            performSearch(category)
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            isSearching = false,
            suggestions = emptyList(),
            selectedCategory = ""
        )
        loadInitialCatalog()
    }

    fun loadAppDetail(packageName: String) {
        viewModelScope.launch {
            val cached = appDetailCache[packageName]
            if (cached != null) {
                // Show instantly from cache
                _currentDetail.value = cached
                _isDetailLoading.value = false
            } else {
                _isDetailLoading.value = true
                _currentDetail.value = null
            }

            try {
                val detail = apiService.getAppDetail(packageName)
                if (detail != null) {
                    appDetailCache[packageName] = detail
                    _currentDetail.value = detail
                }
            } catch (e: Exception) {
                if (_currentDetail.value == null) {
                    _currentDetail.value = null
                }
            } finally {
                _isDetailLoading.value = false
            }
        }
    }

    fun startDownload(appId: Long, packageName: String, appName: String, saveOnly: Boolean = false, versionName: String? = null) {
        viewModelScope.launch {
            when (AppPreferences.updateNetworkMode.value) {
                com.whitestore.data.prefs.UpdateNetworkMode.WIFI_ONLY -> {
                    if (!com.whitestore.util.NetworkUtils.isWifiConnected(context)) {
                        android.widget.Toast.makeText(context, "Загрузка приостановлена: включен режим «Только по Wi-Fi»", android.widget.Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
                com.whitestore.data.prefs.UpdateNetworkMode.CELLULAR_ONLY -> {
                    if (!com.whitestore.util.NetworkUtils.isCellularConnected(context)) {
                        android.widget.Toast.makeText(context, "Загрузка приостановлена: включен режим «Только по мобильной сети»", android.widget.Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
                com.whitestore.data.prefs.UpdateNetworkMode.ANY -> {}
            }

            installer.setResolving(packageName)
            try {
                var targetAppId = appId
                if (targetAppId <= 0L) {
                    val detail = apiService.getAppDetail(packageName)
                    targetAppId = detail?.appId ?: 0L
                }

                if (targetAppId <= 0L) {
                    installer.updateState(packageName, InstallState.Error("Не удалось определить ID приложения"))
                    return@launch
                }

                val downloadUrl = apiService.getDownloadLink(targetAppId)
                if (!downloadUrl.isNullOrBlank()) {
                    installer.downloadAndInstall(packageName, downloadUrl, appName, saveOnly = saveOnly, versionName = versionName)
                } else {
                    installer.updateState(packageName, InstallState.Error("Ссылка на скачивание не найдена"))
                }
            } catch (e: Exception) {
                installer.updateState(packageName, InstallState.Error(e.message ?: "Ошибка подготовки загрузки"))
            }
        }
    }

    fun checkUpdates() {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            val startTime = System.currentTimeMillis()
            val updatesFound = mutableListOf<AppUpdateInfo>()

            try {
                withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val ignored = AppPreferences.ignoredPackages.value
                    val installed = pm.getInstalledApplications(0).filter {
                        (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                                it.packageName != context.packageName &&
                                !ignored.contains(it.packageName)
                    }

                    // Query store in parallel (chunks of 4)
                    val deferredList = installed.map { appInfo ->
                        async {
                            try {
                                val pkgName = appInfo.packageName
                                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                                } else {
                                    @Suppress("DEPRECATION")
                                    pm.getPackageInfo(pkgName, 0)
                                }

                                val localCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    packageInfo.longVersionCode
                                } else {
                                    @Suppress("DEPRECATION")
                                    packageInfo.versionCode.toLong()
                                }
                                val localName = packageInfo.versionName ?: ""

                                val storeDetail = apiService.getAppDetail(pkgName)
                                if (storeDetail != null && storeDetail.versionCode != null) {
                                    val remoteCode = storeDetail.versionCode
                                    if (remoteCode > localCode) {
                                        Log.d(TAG, "Update available for $pkgName: local=$localCode, remote=$remoteCode")
                                        val localBitmap = try {
                                            pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }
                                        AppUpdateInfo(
                                            appId = storeDetail.appId,
                                            packageName = pkgName,
                                            appName = storeDetail.appName.ifBlank { pm.getApplicationLabel(appInfo).toString() },
                                            currentVersionCode = localCode,
                                            currentVersionName = localName,
                                            targetVersionCode = remoteCode,
                                            targetVersionName = storeDetail.versionName ?: "",
                                            fileSize = storeDetail.fileSize ?: 0L,
                                            iconUrl = storeDetail.iconUrl,
                                            localIconBitmap = localBitmap
                                        )
                                    } else null
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    val results = deferredList.awaitAll().filterNotNull()
                    updatesFound.addAll(results)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking updates", e)
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 600) {
                    delay(600 - elapsed)
                }
                _availableUpdates.value = updatesFound
                _isCheckingUpdates.value = false
                _hasCheckedUpdates.value = true
                AppPreferences.setLastUpdateCheckTime(context, System.currentTimeMillis())
            }
        }
    }

    fun updateAll() {
        val list = _availableUpdates.value
        val installStates = installer.installState.value
        list.forEach { update ->
            val currentState = installStates[update.packageName]
            // Skip apps currently downloading or ready to install
            if (currentState is InstallState.Downloading || currentState is InstallState.Resolving) {
                return@forEach
            }
            if (currentState is InstallState.ReadyToInstall) {
                installer.installDownloadedApk(update.packageName)
            } else {
                startDownload(update.appId, update.packageName, update.appName)
            }
        }
    }
}
