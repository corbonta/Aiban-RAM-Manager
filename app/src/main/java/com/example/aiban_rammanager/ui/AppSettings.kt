package com.example.aiban_rammanager.ui

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** Индивидуальный профиль игры. */
data class GameProfile(
    val cleanup: Boolean = true,
    val autoLaunch: Boolean = true,
    val monitor: Boolean = true,
    val dnd: Boolean = false
)

/**
 * Сохраняемые настройки.
 * Значения, которые видит интерфейс, хранятся в состоянии Compose,
 * поэтому экран и фоновые циклы реагируют на изменения сразу.
 */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(
        "aiban_settings",
        Context.MODE_PRIVATE
    )

    private val refreshSecState = mutableIntStateOf(
        prefs.getInt("refresh_sec", 3).coerceIn(1, 60)
    )
    private val keepScreenOnState = mutableStateOf(prefs.getBoolean("keep_screen_on", false))
    private val showSystemState = mutableStateOf(prefs.getBoolean("show_system", false))
    private val saveHistoryState = mutableStateOf(prefs.getBoolean("save_history", true))

    var refreshSec: Int
        get() = refreshSecState.intValue
        set(value) {
            val safe = value.coerceIn(1, 60)
            refreshSecState.intValue = safe
            prefs.edit().putInt("refresh_sec", safe).apply()
        }

    var keepScreenOn: Boolean
        get() = keepScreenOnState.value
        set(value) {
            setBool(keepScreenOnState, "keep_screen_on", value)
        }

    var showSystem: Boolean
        get() = showSystemState.value
        set(value) {
            setBool(showSystemState, "show_system", value)
        }

    var saveHistory: Boolean
        get() = saveHistoryState.value
        set(value) {
            setBool(saveHistoryState, "save_history", value)
        }

    private fun setBool(state: MutableState<Boolean>, key: String, value: Boolean) {
        state.value = value
        prefs.edit().putBoolean(key, value).apply()
    }

    /**
     * Приложения, которые заморозили мы. Нужны, чтобы «Разморозить всё» не включило
     * системные пакеты, которые производитель телефона отключил сам.
     */
    var frozenByUs: Set<String>
        get() = prefs.getStringSet("frozen_by_us", emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet("frozen_by_us", HashSet(value)).apply()
        }

    // ---------- Game Booster ----------

    var selectedGamePackage: String
        get() = prefs.getString("selected_game_package", "") ?: ""
        set(value) {
            prefs.edit().putString("selected_game_package", value).apply()
        }

    /** Режим «Не беспокоить», который был до запуска игры (DND_NOT_SET, если мы его не меняли). */
    var dndPrevFilter: Int
        get() = prefs.getInt("dnd_prev_filter", DND_NOT_SET)
        set(value) {
            prefs.edit().putInt("dnd_prev_filter", value).apply()
        }

    private fun profileKey(packageName: String, name: String): String {
        // Имя пакета содержит только буквы, цифры, точки и подчёркивания
        val id = packageName.ifBlank { "default" }
        return "profile_${id}_$name"
    }

    /** Профиль выбранной игры. Новая игра получает значения по умолчанию, а не настройки прошлой. */
    fun loadGameProfile(packageName: String): GameProfile {
        val defaults = GameProfile()
        return GameProfile(
            cleanup = prefs.getBoolean(profileKey(packageName, "cleanup"), defaults.cleanup),
            autoLaunch = prefs.getBoolean(profileKey(packageName, "auto"), defaults.autoLaunch),
            monitor = prefs.getBoolean(profileKey(packageName, "monitor"), defaults.monitor),
            dnd = prefs.getBoolean(profileKey(packageName, "dnd"), defaults.dnd)
        )
    }

    fun saveGameProfile(packageName: String, profile: GameProfile) {
        prefs.edit()
            .putBoolean(profileKey(packageName, "cleanup"), profile.cleanup)
            .putBoolean(profileKey(packageName, "auto"), profile.autoLaunch)
            .putBoolean(profileKey(packageName, "monitor"), profile.monitor)
            .putBoolean(profileKey(packageName, "dnd"), profile.dnd)
            .apply()
    }

    companion object {
        const val DND_NOT_SET = -1
    }
}
