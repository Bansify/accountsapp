package com.bansify.accounts.ui.state

import android.content.Context
import com.bansify.accounts.ui.model.NavigationLayoutMode
import com.bansify.accounts.ui.model.ThemePreference

internal object AccountAppearancePrefs {
    private const val PREFS_NAME = "gothwad_accounts_ui_prefs"
    private const val KEY_THEME = "theme_preference"
    private const val KEY_NAV_LAYOUT = "navigation_layout"

    fun loadTheme(context: Context): ThemePreference {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        return ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM
    }

    fun saveTheme(context: Context, preference: ThemePreference) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, preference.name)
            .apply()
    }

    fun loadNavigationLayout(context: Context): NavigationLayoutMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAV_LAYOUT, null)
        return NavigationLayoutMode.entries.firstOrNull { it.name == raw } ?: NavigationLayoutMode.HEADER_PILLS
    }

    fun saveNavigationLayout(context: Context, mode: NavigationLayoutMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAV_LAYOUT, mode.name)
            .apply()
    }
}
