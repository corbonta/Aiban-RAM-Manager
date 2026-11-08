package com.example.aiban_rammanager.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    /** Сохраняемые настройки (интервал, фильтры, интерфейс). Значения наблюдаемы из Compose. */
    val settings = AppSettings(app)
    private val stats = StatsRepository(app)

    // Очередь сообщений для Snackbar: сообщения больше не теряются, если приходят подряд
    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    val messages = messageChannel.receiveAsFlow()

    private fun say(text: String) {
        messageChannel.trySend(text)
    }

    var mem by mutableStateOf(MemInfo(0, 0))
        private set
    var apps by mutableStateOf<List<AppProc>>(emptyList())
        private set
    var hasRoot by mutableStateOf<Boolean?>(null)
        private set

    /** Измерения за последние минуты (каждое обновление) — для «живого» графика. */
    var live by mutableStateOf<List<RamSample>>(emptyList())
        private set

    /** Долгая история, сохранённая на диск (одна точка раз в 30 секунд). */
    var history by mutableStateOf<List<RamSample>>(emptyList())
        private set

    var showSystem: Boolean
        get() = settings.showSystem
        set(value) {
            settings.showSystem = value
        }

    val selected = mutableStateListOf<String>()

    // ---------- Game Booster ----------

    var isCleaning by mutableStateOf(false)
        private set

    var gameApps by mutableStateOf<List<GameApp>>(emptyList())
        private set

    var selectedGamePackage by mutableStateOf(settings.selectedGamePackage)
        private set

    /** Профиль выбранной игры. Читаем и пишем только его, а не глобальные настройки. */
    var gameProfile by mutableStateOf(settings.loadGameProfile(settings.selectedGamePackage))
        private set

    var isLoadingGames by mutableStateOf(false)
        private set

    var monitorMem by mutableStateOf(MemInfo(0, 0))
        private set

    var batteryTempC by mutableStateOf<Float?>(null)
        private set

    /** Экран Game Booster ставит true, пока показывает монитор. */
    @Volatile
    var monitorVisible: Boolean = false

    val visibleApps: List<AppProc>
        get() = apps.filter { showSystem || !it.isSystem }

    private var lastHistoryMs = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            history = stats.snapshot()
        }
        loadGames()
    }

    /**
     * Фоновые циклы обновления. Запускать внутри repeatOnLifecycle(STARTED),
     * тогда они останавливаются, когда приложение свёрнуто.
     */
    suspend fun runLoops() {
        coroutineScope {
            launch {
                while (isActive) {
                    try {
                        refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Одна неудачная проверка не должна ронять цикл
                    }

                    // Ждём короткими шагами, чтобы новый интервал из настроек применялся сразу
                    var waited = 0L
                    while (isActive && waited < settings.refreshSec * 1000L) {
                        delay(250)
                        waited += 250
                    }
                }
            }

            launch {
                while (isActive) {
                    if (monitorVisible) {
                        try {
                            refreshMonitor()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                        }
                    }
                    delay(2000)
                }
            }
        }
    }

    suspend fun refreshMonitor() {
        val values = withContext(Dispatchers.IO) {
            ProcessRepository.readMemInfo() to
                ProcessRepository.readBatteryTemperature(getApplication<Application>())
        }
        monitorMem = values.first
        batteryTempC = values.second
    }

    suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        // Память читается без root — обновляем её и пишем в статистику сразу
        mem = ProcessRepository.readMemInfo()
        record(mem)

        val root = Shell.getShell().isRoot // тут Magisk покажет запрос root
        hasRoot = root
        if (!root) return@withContext
        val list = ProcessRepository.loadApps(getApplication<Application>(), mem.totalKb)
        apps = list
        val alive = list.map { it.pkg }.toSet()
        selected.removeAll { it !in alive }
        Unit
    }

    private fun record(m: MemInfo) {
        if (m.totalKb <= 0) return
        val now = System.currentTimeMillis()
        val sample = RamSample(now, m.usedKb, m.totalKb)

        live = (live + sample).filter { now - it.timeMs <= LIVE_WINDOW_MS }

        if (settings.saveHistory && now - lastHistoryMs >= HISTORY_STEP_MS) {
            lastHistoryMs = now
            stats.add(sample)
            history = stats.snapshot()
        }
    }

    /** Загружает приложения, доступные для запуска. */
    fun loadGames(refreshIcons: Boolean = false) {
        viewModelScope.launch {
            isLoadingGames = true
            gameApps = withContext(Dispatchers.IO) {
                if (refreshIcons) AppCache.clear()
                GameRepository.loadLaunchableApps(getApplication<Application>())
            }
            isLoadingGames = false
        }
    }

    /** Сохраняет выбранную игру и подгружает её профиль. */
    fun selectGame(packageName: String) {
        val game = gameApps.firstOrNull { it.pkg == packageName } ?: return

        settings.selectedGamePackage = packageName
        selectedGamePackage = packageName
        gameProfile = settings.loadGameProfile(packageName)
        say("Игра выбрана: ${game.label}")
    }

    /** Меняет профиль выбранной игры и сохраняет его. */
    fun updateGameProfile(profile: GameProfile) {
        gameProfile = profile
        settings.saveGameProfile(selectedGamePackage, profile)
    }

    /** Запускает безопасную очистку и при необходимости выбранную игру. */
    fun startGameBooster() {
        if (isCleaning) return

        val profile = gameProfile
        val gamePkg = selectedGamePackage

        viewModelScope.launch {
            isCleaning = true
            val report = mutableListOf<String>()

            try {
                if (profile.cleanup) {
                    val rootAvailable = withContext(Dispatchers.IO) {
                        Shell.getShell().isRoot
                    }

                    if (!rootAvailable) {
                        say("Нет Root-доступа. Очистка отменена.")
                        return@launch
                    }

                    val result = withContext(Dispatchers.IO) {
                        ProcessRepository.cleanupBackgroundApps(
                            getApplication<Application>(),
                            gamePkg
                        )
                    }

                    report += when {
                        result.importanceUnavailable ->
                            "Не удалось определить важность процессов, очистка пропущена."
                        result.candidates == 0 ->
                            "Нечего чистить: подходящих фоновых приложений нет."
                        else ->
                            "Очистка: остановлено ${result.stopped}, ошибок ${result.failed}, " +
                                "освобождено ≈ ${fmt(result.freedKb)}."
                    }
                } else {
                    report += "Очистка выключена в профиле игры."
                }

                if (profile.dnd) {
                    val enabled = withContext(Dispatchers.IO) {
                        DndHelper.enable(getApplication<Application>(), settings)
                    }
                    report += if (enabled) {
                        "«Не беспокоить» включён."
                    } else {
                        "Нет доступа к режиму «Не беспокоить», выдай его в настройках Android."
                    }
                }

                if (profile.autoLaunch) {
                    val game = gameApps.firstOrNull { it.pkg == gamePkg }
                    if (game == null) {
                        report += "Игра не выбрана. Выбери приложение для запуска."
                        say(report.joinToString("\n"))
                    } else {
                        report += "Запускаю: ${game.label}"
                        say(report.joinToString("\n"))
                        delay(900)

                        val launched = GameRepository.launchGame(
                            getApplication<Application>(),
                            gamePkg
                        )
                        if (!launched) say("Не удалось запустить выбранную игру.")
                    }
                } else {
                    say(report.joinToString("\n"))
                }

                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Ошибка Game Booster: ${e.message ?: "неизвестная ошибка"}")
            } finally {
                isCleaning = false
            }
        }
    }

    /** Возвращает прежний режим «Не беспокоить», когда пользователь вернулся в приложение. */
    fun restoreDnd() {
        viewModelScope.launch(Dispatchers.IO) {
            DndHelper.restore(getApplication<Application>(), settings)
        }
    }

    // ---------- Заморозка приложений ----------

    var freezeApps by mutableStateOf<List<FreezeApp>>(emptyList())
        private set

    var isFreezeBusy by mutableStateOf(false)
        private set

    /** Загружает список приложений для заморозки. Нужен root. */
    fun loadFreezeList() {
        viewModelScope.launch {
            isFreezeBusy = true
            try {
                val root = withContext(Dispatchers.IO) { Shell.getShell().isRoot }
                if (!root) {
                    say("Для заморозки нужен root-доступ.")
                    return@launch
                }
                freezeApps = withContext(Dispatchers.IO) {
                    FreezeRepository.list(getApplication<Application>(), settings.frozenByUs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Не удалось загрузить список: ${e.message ?: "неизвестная ошибка"}")
            } finally {
                isFreezeBusy = false
            }
        }
    }

    /** Замораживает или размораживает одно приложение. */
    fun setFrozen(pkg: String, frozen: Boolean) {
        if (isFreezeBusy) return

        viewModelScope.launch {
            isFreezeBusy = true
            try {
                val root = withContext(Dispatchers.IO) { Shell.getShell().isRoot }
                if (!root) {
                    say("Для заморозки нужен root-доступ.")
                    return@launch
                }

                val label = freezeApps.firstOrNull { it.pkg == pkg }?.label ?: pkg
                val ok = withContext(Dispatchers.IO) {
                    if (frozen) {
                        FreezeRepository.freeze(getApplication<Application>(), pkg)
                    } else {
                        FreezeRepository.unfreeze(pkg)
                    }
                }

                if (ok) {
                    settings.frozenByUs = if (frozen) {
                        settings.frozenByUs + pkg
                    } else {
                        settings.frozenByUs - pkg
                    }
                }

                say(
                    when {
                        ok && frozen -> "Заморожено: $label"
                        ok -> "Разморожено: $label"
                        frozen -> "Не удалось заморозить: $label"
                        else -> "Не удалось разморозить: $label"
                    }
                )

                freezeApps = withContext(Dispatchers.IO) {
                    FreezeRepository.list(getApplication<Application>(), settings.frozenByUs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Ошибка заморозки: ${e.message ?: "неизвестная ошибка"}")
            } finally {
                isFreezeBusy = false
            }
        }
    }

    /** Размораживает все замороженные приложения. */
    fun unfreezeAll() {
        if (isFreezeBusy) return

        viewModelScope.launch {
            isFreezeBusy = true
            try {
                // Системные пакеты включаем, только если заморозили их мы
                val tracked = settings.frozenByUs
                val frozenPackages = freezeApps
                    .filter { it.frozen && (!it.isSystem || it.pkg in tracked) }
                    .map { it.pkg }

                var ok = 0
                val unfrozen = mutableSetOf<String>()
                withContext(Dispatchers.IO) {
                    for (pkg in frozenPackages) {
                        if (FreezeRepository.unfreeze(pkg)) {
                            ok++
                            unfrozen += pkg
                        }
                    }
                }
                settings.frozenByUs = settings.frozenByUs - unfrozen
                say("Разморожено: $ok из ${frozenPackages.size}")

                freezeApps = withContext(Dispatchers.IO) {
                    FreezeRepository.list(getApplication<Application>(), settings.frozenByUs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Ошибка разморозки: ${e.message ?: "неизвестная ошибка"}")
            } finally {
                isFreezeBusy = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            stats.clear()
            history = emptyList()
            say("История очищена")
        }
    }

    fun toggle(pkg: String) {
        if (pkg in selected) selected.remove(pkg) else selected.add(pkg)
    }

    fun stop(pkgs: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val allowed = apps.filter { it.pkg in pkgs && !it.isProtected }.map { it.pkg }
            var ok = 0
            for (p in allowed) if (ProcessRepository.forceStop(p)) ok++
            selected.clear()
            say("Остановлено: $ok из ${allowed.size}")
            delay(500)
            refresh()
        }
    }

    /** Замораживает выбранные приложения прямо из главного списка. Критичные пропускаются. */
    fun freezeSelected() {
        val pkgs = selected.toList()

        viewModelScope.launch(Dispatchers.IO) {
            val targets = apps.filter { it.pkg in pkgs && !it.isProtected }
            val skipped = pkgs.size - targets.size

            if (targets.isEmpty()) {
                selected.clear()
                say("Эти приложения заморозить нельзя: они критичные (ядро Android, экран, звонки).")
                return@launch
            }

            var ok = 0
            val frozenNow = mutableSetOf<String>()
            for (app in targets) {
                if (FreezeRepository.freeze(getApplication<Application>(), app.pkg)) {
                    ok++
                    frozenNow += app.pkg
                }
            }
            settings.frozenByUs = settings.frozenByUs + frozenNow

            selected.clear()
            say(
                "Заморожено: $ok из ${targets.size}" +
                    if (skipped > 0) ", пропущено $skipped (критичные)" else ""
            )
            delay(500)
            refresh()
        }
    }

    fun stopSelected() = stop(selected.toList())

    private companion object {
        const val LIVE_WINDOW_MS = 5 * 60_000L
        const val HISTORY_STEP_MS = 30_000L
    }
}
