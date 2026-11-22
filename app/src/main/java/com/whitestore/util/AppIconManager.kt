package com.whitestore.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.whitestore.R
import com.whitestore.data.prefs.AppPreferences

enum class AppIconOption(
    val title: String,
    val description: String,
    val aliasName: String,
    val previewDrawableRes: Int
) {
    CLASSIC(
        title = "Классическая",
        description = "Фирменный синий стиль",
        aliasName = "com.whitestore.MainActivityDefault",
        previewDrawableRes = R.drawable.ic_launcher
    ),
    DARK(
        title = "Тёмная / AMOLED",
        description = "Глубокий чёрный с неоном",
        aliasName = "com.whitestore.MainActivityDark",
        previewDrawableRes = R.drawable.ic_launcher_dark
    ),
    MONOCHROME(
        title = "Монохром",
        description = "Минималистичный светлый",
        aliasName = "com.whitestore.MainActivityMonochrome",
        previewDrawableRes = R.drawable.ic_launcher_mono
    ),
    NEON(
        title = "Неоновая",
        description = "Фиолетовый киберпанк",
        aliasName = "com.whitestore.MainActivityNeon",
        previewDrawableRes = R.drawable.ic_launcher_neon
    )
}

object AppIconManager {
    private const val TAG = "AppIconManager"

    fun setAppIcon(context: Context, option: AppIconOption) {
        val pm = context.packageManager
        // Ensure MainActivity itself is ALWAYS enabled so shortcuts and internal intents never fail
        ensureMainActivityEnabled(context)

        for (item in AppIconOption.values()) {
            val componentName = ComponentName(context, item.aliasName)
            val newState = if (item == option) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error setting component state for ${item.aliasName}", e)
            }
        }
        AppPreferences.setAppIcon(context, option.name)
    }

    fun ensureMainActivityEnabled(context: Context) {
        try {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                ComponentName(context, "com.whitestore.MainActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring MainActivity enabled", e)
        }
    }

    fun syncCurrentAppIcon(context: Context) {
        val current = getCurrentAppIcon()
        setAppIcon(context, current)
    }

    fun getCurrentAppIcon(): AppIconOption {
        val saved = AppPreferences.appIcon.value
        return try {
            AppIconOption.valueOf(saved)
        } catch (e: Exception) {
            AppIconOption.CLASSIC
        }
    }
}
