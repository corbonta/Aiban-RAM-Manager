package com.example.aiban_rammanager.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import com.topjohnwu.superuser.Shell
import java.io.File

data class MemInfo(val totalKb: Long, val availKb: Long) {
    val usedKb: Long get() = totalKb - availKb
    val usedFraction: Float get() = if (totalKb > 0) usedKb.toFloat() / totalKb else 0f
}

data class AppProc(
    val pkg: String,
    val label: String,
    val rssKb: Long,
    val percent: Float,
    val isSystem: Boolean,
    /** Критичное приложение: его нельзя ни остановить, ни заморозить (ядро Android, экран, звонки). */
    val isProtected: Boolean,
    /** Системное или важное приложение (в том числе Google): действия разрешены, но с предупреждением. */
    val isRisky: Boolean,
    val icon: ImageBitmap?,
    /**
     * Важность процесса для Android (oom_score_adj): чем меньше, тем важнее.
     * У пакета берётся самый важный из его процессов. null — прочитать не удалось.
     */
    val oomAdj: Int?
)

data class CleanupResult(
    val candidates: Int,
    val stopped: Int,
    val failed: Int,
    val skipped: Int,
    val freedKb: Long,
    /** true, если не удалось определить важность процессов, тогда никого не останавливаем. */
    val importanceUnavailable: Boolean
)

object ProcessRepository {

    // Критичные пакеты: их нельзя трогать, телефон может зависнуть или уйти в перезагрузку
    internal val PROTECTED = setOf(
        "android",
        "com.android.systemui",
        "com.android.phone",
        "com.android.settings",
        "com.android.server.telecom",
        "com.android.providers.telephony"
    )

    // Эти пакеты разрешено останавливать и замораживать, но это может нарушить работу телефона
    private val RISKY = setOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.inputmethod.latin"
    )

    private val PKG_REGEX = Regex("^[A-Za-z0-9._]+$")
    private val WHITESPACE = Regex("\\s+")

    private const val ADJ_MARK = "---ADJ---"

    /**
     * Порог oom_score_adj для очистки.
     * 0 — приложение на экране, 100 — видимое, 200 — заметное (музыка, навигация, звонок).
     * От 300 начинаются фоновые службы и кеш, их и останавливаем.
     */
    private const val CLEANUP_MIN_ADJ = 300

    private const val INSTALLED_TTL_MS = 30_000L

    private class InstalledSnapshot(
        val apps: Map<String, ApplicationInfo>,
        val vpnPackages: Set<String>,
        val takenAt: Long
    )

    @Volatile
    private var installedSnapshot: InstalledSnapshot? = null

    /** Список установленных приложений обновляется раз в 30 секунд, а не при каждом обновлении. */
    @Synchronized
    private fun snapshotOf(pm: PackageManager): InstalledSnapshot {
        val now = SystemClock.elapsedRealtime()
        installedSnapshot?.let {
            if (now - it.takenAt < INSTALLED_TTL_MS) return it
        }

        // Консервативно исключаем приложения, объявляющие VPN-службу.
        val vpn = try {
            pm.queryIntentServices(
                Intent("android.net.VpnService"),
                PackageManager.GET_META_DATA
            ).mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }

        val fresh = InstalledSnapshot(
            apps = pm.getInstalledApplications(0).associateBy { it.packageName },
            vpnPackages = vpn,
            takenAt = now
        )
        installedSnapshot = fresh
        return fresh
    }

    /**
     * Критичные пакеты, которые нельзя ни останавливать, ни замораживать:
     * ядро Android, настройки, звонки, активный лаунчер, активная клавиатура и само приложение.
     */
    fun protectedPackages(context: Context): Set<String> {
        val pm = context.packageManager

        val homePkg = pm.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        )?.activityInfo?.packageName

        val inputMethod = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )?.let { ComponentName.unflattenFromString(it)?.packageName }

        return PROTECTED + listOfNotNull(homePkg, inputMethod, context.packageName)
    }

    /**
     * Важные пакеты, с которыми нужна осторожность: службы Google, Bluetooth, NFC,
     * VPN, службы спецвозможностей, приложения SMS и звонков по умолчанию.
     * Их можно остановить или заморозить, но приложение предупреждает об опасности.
     */
    fun riskyPackages(context: Context): Set<String> {
        val pm = context.packageManager

        val accessibilityPackages = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty().split(':').mapNotNull {
            ComponentName.unflattenFromString(it)?.packageName
        }

        val smsPackage = try {
            android.provider.Telephony.Sms.getDefaultSmsPackage(context)
        } catch (_: Exception) {
            null
        }

        val dialerPackage = try {
            context.getSystemService(android.telecom.TelecomManager::class.java)
                ?.defaultDialerPackage
        } catch (_: Exception) {
            null
        }

        return RISKY +
            snapshotOf(pm).vpnPackages +
            accessibilityPackages +
            listOfNotNull(smsPackage, dialerPackage)
    }

    /** Общая и доступная RAM. Root не нужен. */
    fun readMemInfo(): MemInfo {
        var total = 0L
        var avail = 0L
        File("/proc/meminfo").forEachLine { line ->
            when {
                line.startsWith("MemTotal:") -> total = parseKb(line)
                line.startsWith("MemAvailable:") -> avail = parseKb(line)
            }
        }
        return MemInfo(total, avail)
    }

    private fun parseKb(line: String): Long =
        line.trim().split(WHITESPACE).getOrNull(1)?.toLongOrNull() ?: 0L

    /** Список запущенных приложений с потреблением памяти и важностью (через root-shell). */
    fun loadApps(context: Context, totalKb: Long): List<AppProc> {
        val pm = context.packageManager
        val snapshot = snapshotOf(pm)
        val installed = snapshot.apps

        val hardProtected = protectedPackages(context)
        val risky = riskyPackages(context)

        // Одна root-команда: процессы с памятью и важность каждого процесса.
        val out = Shell.cmd(
            "ps -A -o PID,RSS,NAME",
            "echo $ADJ_MARK",
            "grep -H . /proc/[0-9]*/oom_score_adj 2>/dev/null"
        ).exec().out

        val markIndex = out.indexOf(ADJ_MARK)
        val psLines = if (markIndex >= 0) out.subList(0, markIndex) else out
        val adjLines = if (markIndex >= 0) out.subList(markIndex + 1, out.size) else emptyList()

        // Строки вида /proc/1234/oom_score_adj:700
        val adjByPid = HashMap<Int, Int>()
        for (line in adjLines) {
            val value = line.substringAfterLast(':').trim().toIntOrNull() ?: continue
            val pid = line.substringBeforeLast(':')
                .removePrefix("/proc/")
                .substringBefore('/')
                .toIntOrNull() ?: continue
            adjByPid[pid] = value
        }

        // Один пакет может иметь несколько процессов (com.app, com.app:service) - суммируем
        val rssByPkg = HashMap<String, Long>()
        val adjByPkg = HashMap<String, Int>()
        for (line in psLines.drop(1)) {
            val p = line.trim().split(WHITESPACE)
            if (p.size < 3) continue
            val pid = p[0].toIntOrNull() ?: continue
            val rss = p[1].toLongOrNull() ?: continue
            val pkg = p[2].substringBefore(':')
            if (pkg !in installed) continue

            rssByPkg[pkg] = (rssByPkg[pkg] ?: 0L) + rss
            adjByPid[pid]?.let { adj ->
                adjByPkg[pkg] = minOf(adjByPkg[pkg] ?: Int.MAX_VALUE, adj)
            }
        }

        return rssByPkg.map { (pkg, rss) ->
            val info = installed.getValue(pkg)
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppProc(
                pkg = pkg,
                label = AppCache.label(pm, info),
                rssKb = rss,
                percent = if (totalKb > 0) rss * 100f / totalKb else 0f,
                isSystem = isSystem,
                isProtected = pkg in hardProtected,
                isRisky = isSystem || pkg in risky,
                icon = AppCache.icon(pm, info),
                oomAdj = adjByPkg[pkg]
            )
        }.sortedByDescending { it.rssKb }
    }

    /** Температура аккумулятора в градусах Цельсия. */
    fun readBatteryTemperature(context: Context): Float? {
        return try {
            val battery = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val raw = battery?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                Int.MIN_VALUE
            ) ?: Int.MIN_VALUE

            if (raw == Int.MIN_VALUE || raw <= 0) null else raw / 10f
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Останавливает только сторонние приложения, которые Android считает фоновыми.
     * Системные, защищённые приложения и выбранная игра исключаются.
     * Вызывать не из главного потока.
     */
    fun cleanupBackgroundApps(
        context: Context,
        selectedGamePackage: String
    ): CleanupResult {
        val before = readMemInfo()
        val apps = loadApps(context, before.totalKb)

        // Если важность процессов прочитать не удалось, лучше никого не трогать
        if (apps.none { it.oomAdj != null }) {
            return CleanupResult(
                candidates = 0,
                stopped = 0,
                failed = 0,
                skipped = apps.size,
                freedKb = 0L,
                importanceUnavailable = true
            )
        }

        val candidates = apps.filter {
            !it.isSystem &&
                !it.isProtected &&
                !it.isRisky &&
                it.pkg != selectedGamePackage &&
                (it.oomAdj ?: 0) >= CLEANUP_MIN_ADJ
        }

        var stopped = 0
        var failed = 0

        candidates.forEach { app ->
            if (forceStop(app.pkg)) stopped++ else failed++
        }

        // Даём системе освободить память и сравниваем
        var freedKb = 0L
        if (stopped > 0) {
            Thread.sleep(700)
            freedKb = (readMemInfo().availKb - before.availKb).coerceAtLeast(0L)
        }

        return CleanupResult(
            candidates = candidates.size,
            stopped = stopped,
            failed = failed,
            skipped = apps.size - candidates.size,
            freedKb = freedKb,
            importanceUnavailable = false
        )
    }

    /** Полная остановка приложения (процессы и службы). */
    fun forceStop(pkg: String): Boolean {
        if (!PKG_REGEX.matches(pkg)) return false
        return Shell.cmd("am force-stop $pkg").exec().isSuccess
    }
}
