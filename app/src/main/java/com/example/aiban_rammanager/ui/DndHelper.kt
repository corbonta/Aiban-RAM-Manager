package com.example.aiban_rammanager.ui

import android.app.NotificationManager
import android.content.Context
import com.topjohnwu.superuser.Shell

/**
 * Включает «Не беспокоить» на время игры и возвращает прежний режим,
 * когда пользователь снова открывает приложение.
 */
object DndHelper {

    private fun manager(context: Context): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    /** Есть ли у приложения доступ к управлению режимом «Не беспокоить». */
    fun hasAccess(context: Context): Boolean =
        manager(context)?.isNotificationPolicyAccessGranted == true

    /**
     * Включает приоритетный режим. Если доступа нет, пробует выдать его через Root.
     * Вызывать не из главного потока.
     */
    fun enable(context: Context, settings: AppSettings): Boolean {
        val nm = manager(context) ?: return false

        if (!nm.isNotificationPolicyAccessGranted) {
            // Имя пакета нашего приложения, внешнего ввода здесь нет
            Shell.cmd("cmd notification allow_dnd ${context.packageName}").exec()
        }
        if (!nm.isNotificationPolicyAccessGranted) return false

        if (settings.dndPrevFilter == AppSettings.DND_NOT_SET) {
            settings.dndPrevFilter = nm.currentInterruptionFilter
        }
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        return true
    }

    /** Возвращает режим, который был до игры. Ничего не делает, если мы его не меняли. */
    fun restore(context: Context, settings: AppSettings) {
        val previous = settings.dndPrevFilter
        if (previous == AppSettings.DND_NOT_SET) return

        val nm = manager(context)
        if (nm != null &&
            nm.isNotificationPolicyAccessGranted &&
            previous in NotificationManager.INTERRUPTION_FILTER_ALL..NotificationManager.INTERRUPTION_FILTER_ALARMS
        ) {
            nm.setInterruptionFilter(previous)
        }
        settings.dndPrevFilter = AppSettings.DND_NOT_SET
    }
}
