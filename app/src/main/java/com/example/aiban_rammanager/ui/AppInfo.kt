package com.example.aiban_rammanager.ui

/**
 * Все данные о приложении и контакты разработчика в одном месте.
 * Пустая строка значит «ещё не заполнено»: такая строка в интерфейсе просто не показывается.
 */
object AppInfo {

    const val NAME = "Aiban-RM"
    const val FULL_NAME = "Aiban RAM Manager"
    const val TAGLINE = "Менеджер оперативной памяти для Android с root"

    /** Запасное значение. Основная версия берётся из самой сборки (versionName в gradle). */
    const val VERSION_FALLBACK = "1.0"

    const val DEVELOPER = "cansorwy aka corbonta"

    // ---------- Контакты (заполни, и они появятся в приложении) ----------

    /** Ник в Telegram без @, например "xansarow". */
    const val TELEGRAM = "cansorwy"

    /** Почта для связи, например "name@example.com". */
    const val EMAIL = "corbonta@gmail.com"

    /** Ник на GitHub без @ (по желанию). */
    const val GITHUB = "corbonta"

    /** Ссылка на Telegram-канал проекта (по желанию), например "https://t.me/aiban_rm". */
    const val CHANNEL_URL = "https://t.me/aibanrammanager"

    // ---------- Поддержка проекта ----------

    /**
     * Крипто-кошельки для экрана «Поддержать нас»: пары «название сети» to «адрес».
     * Пример: "USDT (TRC20)" to "T....". Пока список пуст, экран показывает «скоро».
     */
    val WALLETS: List<Pair<String, String>> = emptyList()
}
