package com.example.aiban_rammanager.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import com.topjohnwu.superuser.Shell
import java.util.Locale

data class FreezeApp(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap?,
    val frozen: Boolean,
    /** Системное приложение (в том числе Google): замораживать можно, но с предупреждением. */
    val isSystem: Boolean
)

/**
 * Заморозка приложений.
 *
 * Замороженное приложение полностью выключено: оно остановлено, не запускается,
 * не работает в фоне, не присылает уведомления и пропадает с рабочего стола.
 * Данные приложения остаются на месте, поэтому после разморозки оно работает как раньше.
 * Удалять и скачивать заново не нужно.
 *
 * Под капотом: `am force-stop` и `pm disable-user` для заморозки, `pm enable` для разморозки.
 */
object FreezeRepository {

    private val PKG_REGEX = Regex("^[A-Za-z0-9._]+$")

    /** Имена всех отключённых приложений, включая системные (через root). */
    private fun disabledPackages(): Set<String> =
        Shell.cmd("pm list packages -d").exec().out
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .filter { PKG_REGEX.matches(it) }
            .toSet()

    /**
     * Приложения, которые можно заморозить: сторонние и системные (в том числе Google).
     * Критичные (ядро Android, экран, звонки, лаунчер, клавиатура) в список не попадают.
     * Системные пакеты, отключённые самим производителем, скрыты, чтобы не засорять список.
     *
     * @param frozenByUs пакеты, которые заморозило это приложение
     */
    fun list(context: Context, frozenByUs: Set<String>): List<FreezeApp> {
        val pm = context.packageManager
        val protectedSet = ProcessRepository.protectedPackages(context)
        val disabled = disabledPackages()

        val infos = HashMap<String, ApplicationInfo>()
        pm.getInstalledApplications(0).forEach { infos[it.packageName] = it }

        // Отключённые приложения могут не попасть в общий список, добавляем их отдельно
        disabled.forEach { pkg ->
            if (pkg !in infos) {
                try {
                    infos[pkg] = pm.getApplicationInfo(pkg, 0)
                } catch (_: Exception) {
                    // Приложение пропало, пропускаем
                }
            }
        }

        return infos.values
            .asSequence()
            .filter { it.packageName !in protectedSet }
            .map { info ->
                val system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val frozen = !info.enabled || info.packageName in disabled
                Triple(info, system, frozen)
            }
            // Отключённые системные пакеты показываем, только если заморозили их мы
            .filter { (info, system, frozen) ->
                !system || !frozen || info.packageName in frozenByUs
            }
            .map { (info, system, frozen) ->
                FreezeApp(
                    pkg = info.packageName,
                    label = AppCache.label(pm, info),
                    icon = AppCache.icon(pm, info),
                    frozen = frozen,
                    isSystem = system
                )
            }
            .sortedWith(
                compareByDescending<FreezeApp> { it.frozen }
                    .thenBy { it.label.lowercase(Locale.getDefault()) }
            )
            .toList()
    }

    /** Замораживает приложение. Критичные пакеты отклоняются, даже если вызвать напрямую. */
    fun freeze(context: Context, pkg: String): Boolean {
        if (!PKG_REGEX.matches(pkg)) return false
        if (pkg in ProcessRepository.protectedPackages(context)) return false

        return Shell.cmd(
            "am force-stop $pkg",
            "pm disable-user --user 0 $pkg"
        ).exec().isSuccess
    }

    /** Размораживает приложение: оно снова работает со всеми данными. */
    fun unfreeze(pkg: String): Boolean {
        if (!PKG_REGEX.matches(pkg)) return false
        return Shell.cmd("pm enable --user 0 $pkg").exec().isSuccess
    }
}
