package com.whitestore.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AccentColor(val title: String, val primaryLight: Long, val primaryDark: Long) {
    BLUE("Синий", 0xFF006495, 0xFF8FCDFF),
    PURPLE("Фиолетовый", 0xFF6750A4, 0xFFD0BCFF),
    GREEN("Изумрудный", 0xFF2E6C43, 0xFF85D79B),
    ORANGE("Оранжевый", 0xFF8B5000, 0xFFFFB77C),
    TEAL("Бирюзовый", 0xFF006A6A, 0xFF4CDADA),
    RED("Красный", 0xFFB3261E, 0xFFF2B8B5)
}

object ThemePreferences {
    private const val PREFS_NAME = "whitestore_theme_prefs"
    private const val KEY_THEME = "app_theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"

    val currentThemeMode = mutableStateOf(AppThemeMode.SYSTEM)
    val dynamicColorEnabled = mutableStateOf(true)
    val currentAccentColor = mutableStateOf(AccentColor.BLUE)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        currentThemeMode.value = try {
            AppThemeMode.valueOf(saved)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
        dynamicColorEnabled.value = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        val savedAccent = prefs.getString(KEY_ACCENT_COLOR, AccentColor.BLUE.name) ?: AccentColor.BLUE.name
        currentAccentColor.value = try {
            AccentColor.valueOf(savedAccent)
        } catch (e: Exception) {
            AccentColor.BLUE
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        currentThemeMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_THEME, mode.name)
        }
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        dynamicColorEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_DYNAMIC_COLOR, enabled)
        }
    }

    fun setAccentColor(context: Context, accent: AccentColor) {
        currentAccentColor.value = accent
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_ACCENT_COLOR, accent.name)
        }
    }
}
