package com.example.aiban_rammanager.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Кеш иконок и названий приложений.
 * Раньше иконки перерисовывались в bitmap при каждом обновлении (каждые несколько секунд).
 */
object AppCache {

    private val icons = ConcurrentHashMap<String, ImageBitmap>()
    private val labels = ConcurrentHashMap<String, String>()

    fun icon(pm: PackageManager, info: ApplicationInfo): ImageBitmap? {
        icons[info.packageName]?.let { return it }

        return try {
            val bitmap = pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
            icons[info.packageName] = bitmap
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun label(pm: PackageManager, info: ApplicationInfo): String =
        labels.getOrPut(info.packageName) {
            pm.getApplicationLabel(info).toString()
        }

    /** Сброс кеша, например после установки или обновления приложений. */
    fun clear() {
        icons.clear()
        labels.clear()
    }
}
