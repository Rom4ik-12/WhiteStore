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
import com.whitestore.data.model.RecentAppItem
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StoreUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val suggestions: List<SuggestItem> = emptyList(),
    val apps: List<AppSummaryItem> = emptyList(),
    val recommendedApps: List<AppSummaryItem> = emptyList(),
    val newApps: List<AppSummaryItem> = emptyList(),
    val gamesApps: List<AppSummaryItem> = emptyList(),
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

    private val _developerApps = MutableStateFlow<List<AppSummaryItem>>(emptyList())
    val developerApps: StateFlow<List<AppSummaryItem>> = _developerApps.asStateFlow()

    private val _isDeveloperAppsLoading = MutableStateFlow(false)
    val isDeveloperAppsLoading: StateFlow<Boolean> = _isDeveloperAppsLoading.asStateFlow()

    private val _developerAppsError = MutableStateFlow<String?>(null)
    val developerAppsError: StateFlow<String?> = _developerAppsError.asStateFlow()

    private var currentDeveloperName: String = ""

    // Pre-cache item summaries: packageName -> AppSummaryItem
    val appSummaryCache = java.util.concurrent.ConcurrentHashMap<String, AppSummaryItem>()

    // In-memory cache for app details: packageName -> AppDetail
    private val appDetailCache = java.util.concurrent.ConcurrentHashMap<String, AppDetail>()

    // Cache for checking whether package is known in RuStore: packageName -> Boolean
    private val ruStorePackagesCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    suspend fun isPackageInRuStore(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val cached = ruStorePackagesCache[packageName]
        if (cached != null) return@withContext cached
        if (appDetailCache.containsKey(packageName) || appSummaryCache.containsKey(packageName)) {
            ruStorePackagesCache[packageName] = true
            return@withContext true
        }
        try {
            val detail = apiService.getAppDetail(packageName)
            val isInStore = detail != null
            if (detail != null) {
                appDetailCache[packageName] = detail
            }
            ruStorePackagesCache[packageName] = isInStore
            isInStore
        } catch (e: Exception) {
            false
        }
    }

    // Updates state
    private val _availableUpdates = MutableStateFlow<List<AppUpdateInfo>>(emptyList())
    val availableUpdates: StateFlow<List<AppUpdateInfo>> = _availableUpdates.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _hasCheckedUpdates = MutableStateFlow(false)
    val hasCheckedUpdates: StateFlow<Boolean> = _hasCheckedUpdates.asStateFlow()

    // Recent apps state & persistent disk cache
    private val recentCacheFile by lazy { File(context.filesDir, "recent_apps_cache.json") }
    private val jsonSerializer = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private fun loadRecentAppsFromDisk(): List<RecentAppItem> {
        return try {
            if (recentCacheFile.exists()) {
                val content = recentCacheFile.readText()
                if (content.isNotBlank()) {
                    jsonSerializer.decodeFromString<List<RecentAppItem>>(content)
                } else emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.w("StoreViewModel", "Failed to load recent apps cache from disk", e)
            emptyList()
        }
    }

    private fun saveRecentAppsToDisk(items: List<RecentAppItem>) {
        try {
            val text = jsonSerializer.encodeToString(items)
            recentCacheFile.writeText(text)
        } catch (e: Exception) {
            Log.w("StoreViewModel", "Failed to save recent apps cache to disk", e)
        }
    }

    private val _recentApps = MutableStateFlow<List<RecentAppItem>>(loadRecentAppsFromDisk())
    val recentApps: StateFlow<List<RecentAppItem>> = _recentApps.asStateFlow()

    private val _isRecentLoading = MutableStateFlow(false)
    val isRecentLoading: StateFlow<Boolean> = _isRecentLoading.asStateFlow()
    val isRecentSyncing: StateFlow<Boolean> = _isRecentLoading.asStateFlow()

    private val activeDownloadJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

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

        // Listen for package install/update/remove events to immediately remove updated apps from availableUpdates and refresh recent apps
        viewModelScope.launch {
            installer.packageUpdateTrigger.collect { trigger ->
                if (trigger > 0) {
                    filterUpdatedApps()
                    loadRecentApps(force = false)
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

    fun loadRecentApps(force: Boolean = false) {
        if (_isRecentLoading.value) return
        viewModelScope.launch {
            _isRecentLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val packages = pm.getInstalledApplications(0)
                    val installedUserApps = packages.filter {
                        (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName
                    }
                    val installedPkgMap = installedUserApps.associateBy { it.packageName }

                    val cachedItems = _recentApps.value
                    val validCachedItems = cachedItems.filter { installedPkgMap.containsKey(it.packageName) }
                    val existingByPkg = validCachedItems.associateBy { it.packageName }.toMutableMap()

                    var hasChanges = validCachedItems.size != cachedItems.size
                    val newItemsToNetworkCheck = mutableListOf<String>()

                    for (appInfo in installedUserApps) {
                        val pkg = appInfo.packageName
                        val pInfo = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                            } else {
                                @Suppress("DEPRECATION")
                                pm.getPackageInfo(pkg, 0)
                            }
                        } catch (e: Exception) {
                            null
                        } ?: continue

                        val firstInstall = pInfo.firstInstallTime
                        val lastUpdate = pInfo.lastUpdateTime
                        val isUpdated = (lastUpdate - firstInstall) > 60_000L
                        val mostRecentTime = maxOf(firstInstall, lastUpdate)
                        val currentVersion = pInfo.versionName ?: ""

                        val existing = existingByPkg[pkg]
                        if (existing != null) {
                            if (existing.timestamp != mostRecentTime || existing.versionName != currentVersion) {
                                val appSize = try {
                                    File(appInfo.sourceDir).length()
                                } catch (e: Exception) {
                                    existing.appSizeBytes
                                }
                                val label = pm.getApplicationLabel(appInfo).toString()
                                val updatedItem = existing.copy(
                                    name = label,
                                    versionName = currentVersion,
                                    firstInstallTime = firstInstall,
                                    lastUpdateTime = lastUpdate,
                                    isUpdated = isUpdated,
                                    timestamp = mostRecentTime,
                                    appSizeBytes = appSize
                                )
                                existingByPkg[pkg] = updatedItem
                                hasChanges = true
                            }
                        } else {
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val appSize = try {
                                File(appInfo.sourceDir).length()
                            } catch (e: Exception) {
                                0L
                            }
                            val newItem = RecentAppItem(
                                name = label,
                                packageName = pkg,
                                versionName = currentVersion,
                                firstInstallTime = firstInstall,
                                lastUpdateTime = lastUpdate,
                                isUpdated = isUpdated,
                                timestamp = mostRecentTime,
                                isInRuStore = false,
                                appSizeBytes = appSize
                            )
                            existingByPkg[pkg] = newItem
                            newItemsToNetworkCheck.add(pkg)
                            hasChanges = true
                        }
                    }

                    var updatedList = existingByPkg.values.sortedByDescending { it.timestamp }
                    if (hasChanges || cachedItems.isEmpty()) {
                        _recentApps.value = updatedList
                        saveRecentAppsToDisk(updatedList)
                    }

                    // Check RuStore status only for new items or items that haven't been checked yet
                    val needRuStoreCheck = if (force) {
                        updatedList.filter { !it.isInRuStore }
                    } else {
                        updatedList.filter { newItemsToNetworkCheck.contains(it.packageName) }
                    }

                    if (needRuStoreCheck.isNotEmpty()) {
                        val semaphore = Semaphore(6)
                        val checkedMap = needRuStoreCheck.map { item ->
                            async {
                                val inStore = semaphore.withPermit {
                                    isPackageInRuStore(item.packageName)
                                }
                                item.packageName to inStore
                            }
                        }.awaitAll().toMap()

                        updatedList = updatedList.map { item ->
                            val inStore = checkedMap[item.packageName]
                            if (inStore != null) item.copy(isInRuStore = inStore) else item
                        }

                        _recentApps.value = updatedList
                        saveRecentAppsToDisk(updatedList)
                    }
                }
            } catch (e: Exception) {
                Log.e("StoreViewModel", "Error syncing recent apps", e)
            } finally {
                _isRecentLoading.value = false
            }
        }
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
                val mainResultDeferred = async { apiService.searchApps(query = "Банк", pageNumber = 0, pageSize = 25) }
                val recResultDeferred = async { apiService.searchApps(query = "Популярное", pageNumber = 0, pageSize = 12) }
                val newResultDeferred = async { apiService.searchApps(query = "Новинки", pageNumber = 0, pageSize = 12) }
                val gamesResultDeferred = async { apiService.searchApps(query = "Игры", pageNumber = 0, pageSize = 12) }

                val mainApps = mainResultDeferred.await().content
                val recApps = recResultDeferred.await().content
                val newApps = newResultDeferred.await().content
                val gamesApps = gamesResultDeferred.await().content

                (mainApps + recApps + newApps + gamesApps).forEach { appSummaryCache[it.packageName] = it }

                _uiState.value = _uiState.value.copy(
                    apps = mainApps,
                    recommendedApps = recApps,
                    newApps = newApps,
                    gamesApps = gamesApps,
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

    fun loadDeveloperApps(developerName: String, companyId: String? = null) {
        val trimmed = developerName.trim()
        if (trimmed.isEmpty()) return
        val cacheKey = "$trimmed|$companyId"
        if (currentDeveloperName == cacheKey && _developerApps.value.isNotEmpty()) {
            return
        }
        currentDeveloperName = cacheKey
        viewModelScope.launch {
            _isDeveloperAppsLoading.value = true
            _developerAppsError.value = null
            try {
                // Fetch candidate apps with search
                val searchResult = apiService.searchApps(trimmed, pageNumber = 0, pageSize = 50)
                searchResult.content.forEach { appSummaryCache[it.packageName] = it }

                // RuStore's searchApps query returns apps matching keywords, not strictly by company.
                // We verify each candidate's companyName / publicCompanyId in parallel to ensure 100% accuracy.
                val semaphore = Semaphore(6)
                val cleanTargetName = trimmed
                    .replace("«", "")
                    .replace("»", "")
                    .replace("\"", "")
                    .trim()

                val verifiedApps = searchResult.content.map { appItem ->
                    async {
                        // Check if in cache first
                        val cachedDetail = appDetailCache[appItem.packageName]
                        if (cachedDetail != null) {
                            val matchesId = !companyId.isNullOrBlank() && cachedDetail.publicCompanyId == companyId
                            val matchesName = cachedDetail.companyName?.let { cName ->
                                val cleanC = cName.replace("«", "").replace("»", "").replace("\"", "").trim()
                                cleanC.equals(cleanTargetName, ignoreCase = true) ||
                                (cleanTargetName.length >= 3 && cleanC.contains(cleanTargetName, ignoreCase = true)) ||
                                (cleanC.length >= 3 && cleanTargetName.contains(cleanC, ignoreCase = true))
                            } == true
                            if (matchesId || matchesName) appItem else null
                        } else {
                            semaphore.withPermit {
                                try {
                                    val detail = apiService.getAppDetail(appItem.packageName)
                                    if (detail != null) {
                                        appDetailCache[appItem.packageName] = detail
                                        val matchesId = !companyId.isNullOrBlank() && detail.publicCompanyId == companyId
                                        val matchesName = detail.companyName?.let { cName ->
                                            val cleanC = cName.replace("«", "").replace("»", "").replace("\"", "").trim()
                                            cleanC.equals(cleanTargetName, ignoreCase = true) ||
                                            (cleanTargetName.length >= 3 && cleanC.contains(cleanTargetName, ignoreCase = true)) ||
                                            (cleanC.length >= 3 && cleanTargetName.contains(cleanC, ignoreCase = true))
                                        } == true
                                        if (matchesId || matchesName) appItem else null
                                    } else null
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                    }
                }.awaitAll().filterNotNull()

                // If strict filtering matched items, use them; if empty (e.g. network timeout), fallback to search result
                _developerApps.value = if (verifiedApps.isNotEmpty()) verifiedApps else searchResult.content
            } catch (e: Exception) {
                _developerAppsError.value = e.message
            } finally {
                _isDeveloperAppsLoading.value = false
            }
        }
    }

    fun startDownload(appId: Long, packageName: String, appName: String, saveOnly: Boolean = false, versionName: String? = null) {
        val existingJob = activeDownloadJobs[packageName]
        if (existingJob != null && existingJob.isActive) {
            return
        }

        val job = viewModelScope.launch {
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

            val prevPausedState = installer.installState.value[packageName] as? InstallState.Paused
            installer.setResolving(packageName)
            try {
                var targetAppId = appId
                if (targetAppId <= 0L) {
                    val detail = apiService.getAppDetail(packageName)
                    targetAppId = detail?.appId ?: 0L
                }

                if (targetAppId <= 0L) {
                    installer.updateState(packageName, prevPausedState ?: InstallState.Error("Не удалось определить ID приложения"))
                    return@launch
                }

                val downloadUrl = apiService.getDownloadLink(targetAppId)
                if (!downloadUrl.isNullOrBlank()) {
                    installer.downloadAndInstall(packageName, downloadUrl, appName, saveOnly = saveOnly, versionName = versionName)
                } else {
                    installer.updateState(packageName, prevPausedState ?: InstallState.Error("Ссылка на скачивание не найдена"))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("StoreViewModel", "startDownload job cancelled for $packageName")
                if (installer.installState.value[packageName] !is InstallState.Paused) {
                    installer.updateState(packageName, InstallState.Idle)
                }
            } catch (e: Exception) {
                installer.updateState(packageName, prevPausedState ?: InstallState.Error(e.message ?: "Ошибка подготовки загрузки"))
            } finally {
                activeDownloadJobs.remove(packageName)
            }
        }
        activeDownloadJobs[packageName] = job
    }

    fun pauseDownload(packageName: String) {
        installer.pauseDownload(packageName)
        val job = activeDownloadJobs.remove(packageName)
        job?.cancel()
    }

    fun cancelDownload(packageName: String) {
        installer.cancelDownload(packageName)
        val job = activeDownloadJobs.remove(packageName)
        job?.cancel()
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
                    // Query applications that are not system apps or are updated system apps
                    val installed = pm.getInstalledApplications(0).filter {
                        val isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystem = (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        (!isSystem || isUpdatedSystem) &&
                                it.packageName != context.packageName &&
                                !ignored.contains(it.packageName)
                    }

                    // Query store with controlled concurrency (semaphore) to prevent rate limiting / timeouts
                    val semaphore = Semaphore(3)
                    val deferredList: List<kotlinx.coroutines.Deferred<AppUpdateInfo?>> = installed.map { appInfo: ApplicationInfo ->
                        async {
                            semaphore.withPermit {
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
