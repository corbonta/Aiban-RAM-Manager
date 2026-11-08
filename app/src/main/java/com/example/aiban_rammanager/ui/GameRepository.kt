package com.example.aiban_rammanager.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import java.util.Locale

data class GameApp(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap?,
    val isGame: Boolean,
    val isSystem: Boolean
)

object GameRepository {

    private val PACKAGE_REGEX = Regex("^[A-Za-z0-9._]+$")

    /**
     * Возвращает приложения, у которых есть обычная иконка запуска.
     * Игры Android CATEGORY_GAME поднимаются в начало списка.
     */
    fun loadLaunchableApps(context: Context): List<GameApp> {
        val pm = context.packageManager

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { info ->
                GameApp(
                    pkg = info.packageName,
                    label = AppCache.label(pm, info),
                    icon = AppCache.icon(pm, info),
                    isGame = info.category == ApplicationInfo.CATEGORY_GAME,
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedWith(
                compareByDescending<GameApp> { it.isGame }
                    .thenBy { it.label.lowercase(Locale.getDefault()) }
            )
            .toList()
    }

    /**
     * Запускает только приложение, для которого Android выдаёт launch intent.
     */
    fun launchGame(context: Context, packageName: String): Boolean {
        if (!PACKAGE_REGEX.matches(packageName)) return false
        if (packageName == context.packageName) return false

        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)
                ?: return false

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
