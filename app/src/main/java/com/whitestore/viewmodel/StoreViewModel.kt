package com.whitestore.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitestore.data.api.RuStoreApiService
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.installer.InstallState
import com.whitestore.data.model.AppDetail
import com.whitestore.data.model.AppSummaryItem
import com.whitestore.data.model.AppUpdateInfo
import com.whitestore.data.model.SuggestItem
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
            _isDetailLoading.value = true
            _currentDetail.value = null
            try {
                val detail = apiService.getAppDetail(packageName)
                _currentDetail.value = detail
            } catch (e: Exception) {
                _currentDetail.value = null
            } finally {
                _isDetailLoading.value = false
            }
        }
    }

    fun startDownload(appId: Long, packageName: String, appName: String) {
        viewModelScope.launch {
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
                    installer.downloadAndInstall(packageName, downloadUrl, appName)
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
            val updatesFound = mutableListOf<AppUpdateInfo>()

            try {
                withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val installed = pm.getInstalledApplications(0).filter {
                        (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName
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
                                            localIcon = pm.getApplicationIcon(appInfo)
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
                _availableUpdates.value = updatesFound
                _isCheckingUpdates.value = false
                _hasCheckedUpdates.value = true
            }
        }
    }

    fun updateAll() {
        val list = _availableUpdates.value
        list.forEach { update ->
            startDownload(update.appId, update.packageName, update.appName)
        }
    }
}
