package com.whitestore.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemePreferences {
    private const val PREFS_NAME = "whitestore_theme_prefs"
    private const val KEY_THEME = "app_theme_mode"

    val currentThemeMode = mutableStateOf(AppThemeMode.SYSTEM)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        currentThemeMode.value = try {
            AppThemeMode.valueOf(saved)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        currentThemeMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_THEME, mode.name)
        }
    }
}
