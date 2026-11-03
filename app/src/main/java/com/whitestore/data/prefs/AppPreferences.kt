package com.whitestore.data.prefs

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

enum class AutoCheckInterval(val title: String, val hours: Long) {
    OFF("Выключено", 0),
    HOURS_6("Каждые 6 часов", 6),
    HOURS_12("Каждые 12 часов", 12),
    HOURS_24("Раз в сутки", 24)
}

enum class UpdateNetworkMode(val title: String) {
    ANY("Любое подключение"),
    WIFI_ONLY("Только по Wi-Fi"),
    CELLULAR_ONLY("Только по мобильной сети")
}

object AppPreferences {
    private const val PREFS_NAME = "whitestore_app_prefs"
    private const val KEY_OPEN_AFTER_INSTALL = "open_after_install"
    private const val KEY_IGNORED_PACKAGES = "ignored_packages"
    private const val KEY_AUTO_CHECK_INTERVAL = "auto_check_interval"
    private const val KEY_AUTO_CHECK_NOTIFY = "auto_check_notify"
    private const val KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
    private const val KEY_SHOW_INITIAL_VERSION = "show_initial_version"
    private const val KEY_USE_ICON_UPDATE_BUTTON = "use_icon_update_button"
    private const val KEY_SHOW_UPDATE_COUNT_BADGE = "show_update_count_badge"
    private const val KEY_UPDATE_NETWORK_MODE = "update_network_mode"
    private const val KEY_MAX_CACHE_SIZE_MB = "max_cache_size_mb"
    private const val KEY_DELETE_APK_AFTER_INSTALL = "delete_apk_after_install"
    private const val KEY_DOWNLOAD_SPEED_LIMIT_KB = "download_speed_limit_kb"

    val openAfterInstall = mutableStateOf(false)
    val deleteApkAfterInstall = mutableStateOf(false)
    val ignoredPackages = mutableStateOf<Set<String>>(emptySet())
    val autoCheckInterval = mutableStateOf(AutoCheckInterval.OFF)
    val autoCheckNotify = mutableStateOf(true)
    val lastUpdateCheckTime = mutableStateOf(0L)
    val showInitialVersion = mutableStateOf(false)
    val useIconUpdateButton = mutableStateOf(true)
    val showUpdateCountBadge = mutableStateOf(true)
    val updateNetworkMode = mutableStateOf(UpdateNetworkMode.ANY)
    val maxCacheSizeMb = mutableStateOf(1024) // 1024 MB (1 GB) default. 0 = Unlimited.
    val downloadSpeedLimitKb = mutableStateOf(0) // 0 = No speed limit, otherwise KB/s (e.g. 512, 1024, 2048, 5120)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        openAfterInstall.value = prefs.getBoolean(KEY_OPEN_AFTER_INSTALL, false)
        deleteApkAfterInstall.value = prefs.getBoolean(KEY_DELETE_APK_AFTER_INSTALL, false)
        ignoredPackages.value = prefs.getStringSet(KEY_IGNORED_PACKAGES, emptySet())?.toSet() ?: emptySet()
        val savedInterval = prefs.getString(KEY_AUTO_CHECK_INTERVAL, AutoCheckInterval.OFF.name) ?: AutoCheckInterval.OFF.name
        autoCheckInterval.value = try {
            AutoCheckInterval.valueOf(savedInterval)
        } catch (e: Exception) {
            AutoCheckInterval.OFF
        }
        autoCheckNotify.value = prefs.getBoolean(KEY_AUTO_CHECK_NOTIFY, true)
        lastUpdateCheckTime.value = prefs.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0L)
        showInitialVersion.value = prefs.getBoolean(KEY_SHOW_INITIAL_VERSION, false)
        useIconUpdateButton.value = prefs.getBoolean(KEY_USE_ICON_UPDATE_BUTTON, true)
        showUpdateCountBadge.value = prefs.getBoolean(KEY_SHOW_UPDATE_COUNT_BADGE, true)
        val savedNetworkMode = prefs.getString(KEY_UPDATE_NETWORK_MODE, UpdateNetworkMode.ANY.name) ?: UpdateNetworkMode.ANY.name
        updateNetworkMode.value = try {
            UpdateNetworkMode.valueOf(savedNetworkMode)
        } catch (e: Exception) {
            UpdateNetworkMode.ANY
        }
        maxCacheSizeMb.value = prefs.getInt(KEY_MAX_CACHE_SIZE_MB, 1024)
        downloadSpeedLimitKb.value = prefs.getInt(KEY_DOWNLOAD_SPEED_LIMIT_KB, 0)
    }

    fun setOpenAfterInstall(context: Context, value: Boolean) {
        openAfterInstall.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_OPEN_AFTER_INSTALL, value) }
    }

    fun setDeleteApkAfterInstall(context: Context, value: Boolean) {
        deleteApkAfterInstall.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_DELETE_APK_AFTER_INSTALL, value) }
    }

    fun toggleIgnorePackage(context: Context, packageName: String) {
        val current = ignoredPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        ignoredPackages.value = current
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(KEY_IGNORED_PACKAGES, current) }
    }

    fun removeIgnoredPackage(context: Context, packageName: String) {
        val current = ignoredPackages.value.toMutableSet()
        if (current.remove(packageName)) {
            ignoredPackages.value = current
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putStringSet(KEY_IGNORED_PACKAGES, current) }
        }
    }

    fun setAutoCheckInterval(context: Context, interval: AutoCheckInterval) {
        autoCheckInterval.value = interval
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_AUTO_CHECK_INTERVAL, interval.name) }
    }

    fun setAutoCheckNotify(context: Context, notify: Boolean) {
        autoCheckNotify.value = notify
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_AUTO_CHECK_NOTIFY, notify) }
    }

    fun setLastUpdateCheckTime(context: Context, timeMillis: Long) {
        lastUpdateCheckTime.value = timeMillis
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putLong(KEY_LAST_UPDATE_CHECK_TIME, timeMillis) }
    }

    fun setShowInitialVersion(context: Context, value: Boolean) {
        showInitialVersion.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SHOW_INITIAL_VERSION, value) }
    }

    fun setUseIconUpdateButton(context: Context, value: Boolean) {
        useIconUpdateButton.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_USE_ICON_UPDATE_BUTTON, value) }
    }

    fun setShowUpdateCountBadge(context: Context, value: Boolean) {
        showUpdateCountBadge.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SHOW_UPDATE_COUNT_BADGE, value) }
    }

    fun setUpdateNetworkMode(context: Context, mode: UpdateNetworkMode) {
        updateNetworkMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_UPDATE_NETWORK_MODE, mode.name) }
    }

    fun setMaxCacheSizeMb(context: Context, limitMb: Int) {
        maxCacheSizeMb.value = limitMb
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_MAX_CACHE_SIZE_MB, limitMb) }
    }

    fun setDownloadSpeedLimitKb(context: Context, speedKb: Int) {
        downloadSpeedLimitKb.value = speedKb
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_DOWNLOAD_SPEED_LIMIT_KB, speedKb) }
    }
}
