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

    // Home screen customisation keys
    private const val KEY_HOME_SHOW_HEADER_SUBTITLE = "home_show_header_subtitle"
    private const val KEY_HOME_SHOW_SEARCH_BAR = "home_show_search_bar"
    private const val KEY_HOME_SHOW_CATEGORY_CHIPS = "home_show_category_chips"
    private const val KEY_HOME_SHOW_FEATURED_CAROUSEL = "home_show_featured_carousel"
    private const val KEY_HOME_SHOW_RECOMMENDED_SECTION = "home_show_recommended_section"
    private const val KEY_HOME_SHOW_NEW_APPS_SECTION = "home_show_new_apps_section"
    private const val KEY_HOME_SHOW_GAMES_SECTION = "home_show_games_section"
    private const val KEY_HOME_CATALOG_STYLE = "home_catalog_style" // "LIST" or "GRID"
    private const val KEY_HOME_SHOW_APP_DEVELOPER = "home_show_app_developer"
    private const val KEY_HOME_SHOW_APP_RATING = "home_show_app_rating"
    private const val KEY_HOME_SHOW_APP_SIZE = "home_show_app_size"

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

    // Home screen customisation states
    val homeShowHeaderSubtitle = mutableStateOf(true)
    val homeShowSearchBar = mutableStateOf(true)
    val homeShowCategoryChips = mutableStateOf(true)
    val homeShowFeaturedCarousel = mutableStateOf(true)
    val homeShowRecommendedSection = mutableStateOf(true)
    val homeShowNewAppsSection = mutableStateOf(false)
    val homeShowGamesSection = mutableStateOf(false)
    val homeCatalogStyle = mutableStateOf("LIST") // "LIST" or "GRID"
    val homeShowAppDeveloper = mutableStateOf(true)
    val homeShowAppRating = mutableStateOf(true)
    val homeShowAppSize = mutableStateOf(true)

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

        // Home customisation
        homeShowHeaderSubtitle.value = prefs.getBoolean(KEY_HOME_SHOW_HEADER_SUBTITLE, true)
        homeShowSearchBar.value = prefs.getBoolean(KEY_HOME_SHOW_SEARCH_BAR, true)
        homeShowCategoryChips.value = prefs.getBoolean(KEY_HOME_SHOW_CATEGORY_CHIPS, true)
        homeShowFeaturedCarousel.value = prefs.getBoolean(KEY_HOME_SHOW_FEATURED_CAROUSEL, true)
        homeShowRecommendedSection.value = prefs.getBoolean(KEY_HOME_SHOW_RECOMMENDED_SECTION, true)
        homeShowNewAppsSection.value = prefs.getBoolean(KEY_HOME_SHOW_NEW_APPS_SECTION, false)
        homeShowGamesSection.value = prefs.getBoolean(KEY_HOME_SHOW_GAMES_SECTION, false)
        homeCatalogStyle.value = prefs.getString(KEY_HOME_CATALOG_STYLE, "LIST") ?: "LIST"
        homeShowAppDeveloper.value = prefs.getBoolean(KEY_HOME_SHOW_APP_DEVELOPER, true)
        homeShowAppRating.value = prefs.getBoolean(KEY_HOME_SHOW_APP_RATING, true)
        homeShowAppSize.value = prefs.getBoolean(KEY_HOME_SHOW_APP_SIZE, true)
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

    fun setHomeShowHeaderSubtitle(context: Context, value: Boolean) {
        homeShowHeaderSubtitle.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_HEADER_SUBTITLE, value) }
    }

    fun setHomeShowSearchBar(context: Context, value: Boolean) {
        homeShowSearchBar.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_SEARCH_BAR, value) }
    }

    fun setHomeShowCategoryChips(context: Context, value: Boolean) {
        homeShowCategoryChips.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_CATEGORY_CHIPS, value) }
    }

    fun setHomeShowFeaturedCarousel(context: Context, value: Boolean) {
        homeShowFeaturedCarousel.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_FEATURED_CAROUSEL, value) }
    }

    fun setHomeShowRecommendedSection(context: Context, value: Boolean) {
        homeShowRecommendedSection.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_RECOMMENDED_SECTION, value) }
    }

    fun setHomeShowNewAppsSection(context: Context, value: Boolean) {
        homeShowNewAppsSection.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_NEW_APPS_SECTION, value) }
    }

    fun setHomeShowGamesSection(context: Context, value: Boolean) {
        homeShowGamesSection.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_GAMES_SECTION, value) }
    }

    fun setHomeCatalogStyle(context: Context, value: String) {
        homeCatalogStyle.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_HOME_CATALOG_STYLE, value) }
    }

    fun setHomeShowAppDeveloper(context: Context, value: Boolean) {
        homeShowAppDeveloper.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_APP_DEVELOPER, value) }
    }

    fun setHomeShowAppRating(context: Context, value: Boolean) {
        homeShowAppRating.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_APP_RATING, value) }
    }

    fun setHomeShowAppSize(context: Context, value: Boolean) {
        homeShowAppSize.value = value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_SHOW_APP_SIZE, value) }
    }
}
