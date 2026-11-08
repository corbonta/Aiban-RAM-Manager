package com.example.aiban_rammanager.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BodyColor = Color.White.copy(alpha = 0.82f)
private val DangerRed = Color(0xFFFF6B6B)

// ---------- помощники ----------

private fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ?: AppInfo.VERSION_FALLBACK
} catch (_: Exception) {
    AppInfo.VERSION_FALLBACK
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
    }
}

private fun sendMail(context: Context, subject: String) {
    try {
        val uri = "mailto:${AppInfo.EMAIL}?subject=${Uri.encode(subject)}".toUri()
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(context, "Не найдено приложение для почты", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
}

// ---------- общие кусочки интерфейса ----------

@Composable
private fun PageColumn(
    kicker: String,
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(kicker, title, onBack)
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text,
        color = Green,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, color = BodyColor, fontSize = 13.sp, lineHeight = 19.sp)
}

@Composable
private fun FeatureRow(emoji: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = MutedText, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun IconRow(
    title: String,
    subtitle: String,
    note: String? = null,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Green, fontSize = 12.sp)
            if (note != null) {
                Text(note, color = MutedText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TechChip(name: String, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(Modifier.height(6.dp))
        Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContactRow(emoji: String, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(value, color = MutedText, fontSize = 12.sp)
        }
        Text("ОТКРЫТЬ", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Контакты из AppInfo. Пустые значения не показываются, если все пустые, выводится заглушка. */
@Composable
private fun ContactsBlock(context: Context, mailSubject: String) {
    val any = listOf(AppInfo.TELEGRAM, AppInfo.EMAIL, AppInfo.GITHUB, AppInfo.CHANNEL_URL)
        .any { it.isNotBlank() }

    if (!any) {
        Body("Контакты появятся в ближайшем обновлении.")
        return
    }

    if (AppInfo.TELEGRAM.isNotBlank()) {
        ContactRow("✈", "Telegram", "@${AppInfo.TELEGRAM}") {
            openLink(context, "https://t.me/${AppInfo.TELEGRAM}")
        }
    }
    if (AppInfo.EMAIL.isNotBlank()) {
        ContactRow("✉", "Почта", AppInfo.EMAIL) {
            sendMail(context, mailSubject)
        }
    }
    if (AppInfo.GITHUB.isNotBlank()) {
        ContactRow("⌨", "GitHub", AppInfo.GITHUB) {
            openLink(context, "https://github.com/${AppInfo.GITHUB}")
        }
    }
    if (AppInfo.CHANNEL_URL.isNotBlank()) {
        ContactRow("📣", "Канал проекта", AppInfo.CHANNEL_URL) {
            openLink(context, AppInfo.CHANNEL_URL)
        }
    }
}

// ---------- О приложении ----------

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember { appVersion(context) }
    var taps by remember { mutableIntStateOf(0) }

    PageColumn("ПРИЛОЖЕНИЕ", "О приложении", onBack) {

        InfoCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppLogo(Modifier.size(88.dp).clickable { taps++ })

                Spacer(Modifier.height(12.dp))

                Text(
                    AppInfo.NAME,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    AppInfo.TAGLINE,
                    color = MutedText,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Green.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Версия $version",
                        color = Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (taps >= 7) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "🎉 Секрет найден! Сделано с любовью: ${AppInfo.DEVELOPER}",
                        color = Green,
                        fontSize = 12.sp
                    )
                }
            }
        }

        InfoCard {
            CardTitle("О ПРОГРАММЕ")
            Body(
                "Aiban-RM следит за оперативной памятью устройства в реальном времени, " +
                    "показывает, какие приложения расходуют больше всего RAM, и освобождает её " +
                    "одним касанием. Встроенный Game Booster готовит телефон к игре: останавливает " +
                    "лишние фоновые процессы, приглушает уведомления и запускает выбранную игру."
            )
            Spacer(Modifier.height(8.dp))
            Body("Работает полностью офлайн и не отправляет никаких данных.")
        }

        InfoCard {
            CardTitle("ВОЗМОЖНОСТИ")
            FeatureRow("⚡", "Live-монитор памяти", "RAM и список процессов обновляются в реальном времени")
            FeatureRow("🧹", "Smart Cleanup", "Останавливает только фоновые приложения, важные и системные не трогает")
            FeatureRow("🎮", "Game Booster", "Очистка, «Не беспокоить» и автозапуск игры одной кнопкой")
            FeatureRow("📊", "История и графики", "Загрузка RAM за 5 минут, час, 6 и 12 часов, максимум, минимум и среднее")
            FeatureRow("🛡", "Защита от ошибок", "Ядро Android, лаунчер и клавиатура под защитой, на опасные действия приложение предупреждает")
            FeatureRow("🌡", "Температура батареи", "Контроль нагрева во время игры")
            FeatureRow("🔒", "Полностью офлайн", "Нет доступа в интернет, данные остаются на устройстве")
        }

        InfoCard {
            CardTitle("СДЕЛАНО НА")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TechChip("Kotlin") { KotlinIcon(Modifier.size(34.dp)) }
                TechChip("Java") { JavaIcon(Modifier.size(38.dp)) }
                TechChip("Claude") { ClaudeIcon(Modifier.size(36.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Jetpack Compose · Material 3 · libsu (root)",
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        InfoCard {
            CardTitle("ЧТО НОВОГО В $version")
            listOf(
                "Первый релиз Aiban-RM",
                "Smart Cleanup по реальной важности процессов",
                "Game Booster с отдельным профилем для каждой игры",
                "Экраны статистики и настроек",
                "Режим «Не беспокоить» на время игры"
            ).forEach {
                Text("•  $it", color = BodyColor, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        Text(
            "© 2026 ${AppInfo.DEVELOPER}",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

// ---------- Разработчики и помощники ----------

@Composable
fun DevelopersScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    PageColumn("КОМАНДА", "Разработчики и помощники", onBack) {

        InfoCard {
            CardTitle("РАЗРАБОТЧИК")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Green.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "X",
                        color = Green,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        AppInfo.DEVELOPER,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        "Автор и разработчик ${AppInfo.NAME}",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        InfoCard {
            CardTitle("КОНТАКТЫ")
            ContactsBlock(context, "${AppInfo.NAME}: сообщение разработчику")
        }

        InfoCard {
            CardTitle("ПОМОЩНИКИ")
            IconRow(
                title = "Claude",
                subtitle = "Powered by Claude · Anthropic",
                note = "ИИ-помощник: код, архитектура, отладка"
            ) { ClaudeIcon(Modifier.size(36.dp)) }
        }

        InfoCard {
            CardTitle("СДЕЛАНО НА")
            IconRow(
                title = "Kotlin",
                subtitle = "Основной язык приложения",
                note = "Jetpack Compose, корутины"
            ) { KotlinIcon(Modifier.size(34.dp)) }
            IconRow(
                title = "Java",
                subtitle = "Платформа Android и JVM",
                note = "Основа, на которой работает Android-приложение"
            ) { JavaIcon(Modifier.size(38.dp)) }
        }

        Text(
            "Спасибо всем, кто тестирует и делится идеями.",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

// ---------- Поддержать нас ----------

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val wallets = AppInfo.WALLETS.filter { it.second.isNotBlank() }

    PageColumn("ПРОЕКТ", "Поддержать нас", onBack) {

        InfoCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("♥", color = Green, fontSize = 40.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Поддержать ${AppInfo.NAME}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Body(
                    "Проект делает один разработчик. Если ${AppInfo.NAME} оказался полезным, " +
                        "ты можешь помочь его развитию."
                )
            }
        }

        if (wallets.isEmpty()) {
            InfoCard {
                CardTitle("СКОРО")
                Body("Способы поддержки появятся позже: готовится криптокошелёк.")
            }
        } else {
            wallets.forEach { (network, address) ->
                InfoCard {
                    CardTitle(network.uppercase())
                    Text(
                        address,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { copyToClipboard(context, network, address) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = DarkBackground
                        )
                    ) {
                        Text("Скопировать адрес", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        InfoCard {
            CardTitle("ЕЩЁ МОЖНО ПОМОЧЬ")
            FeatureRow("📣", "Расскажи друзьям", "Лучшая поддержка для небольшого проекта")
            FeatureRow("🐞", "Сообщи об ошибке", "Через «Обратную связь» в меню")
            FeatureRow("💡", "Предложи идею", "Что добавить в следующую версию")
        }
    }
}

// ---------- Безопасность ----------

@Composable
fun SecurityScreen(vm: MainViewModel, onBack: () -> Unit) {
    var showPackages by remember { mutableStateOf(false) }

    val (statusText, statusColor, statusNote) = when (vm.hasRoot) {
        true -> Triple(
            "Root активен",
            Green,
            "Приложение может читать процессы и останавливать приложения."
        )
        false -> Triple(
            "Root не получен",
            DangerRed,
            "Выдай root-доступ в Magisk или KernelSU и перезапусти приложение. " +
                "Без него работают только монитор памяти и статистика."
        )
        null -> Triple(
            "Проверка...",
            MutedText,
            "Ждём ответа менеджера root."
        )
    }

    val protectedNow = vm.apps.filter { it.isProtected }

    PageColumn("ЗАЩИТА", "Безопасность", onBack) {

        InfoCard {
            CardTitle("СТАТУС ROOT")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    statusText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Body(statusNote)
        }

        InfoCard {
            CardTitle("ПРИВАТНОСТЬ")
            Body(
                "В приложении нет разрешения INTERNET, поэтому ${AppInfo.NAME} физически не " +
                    "может отправлять данные в сеть. История RAM и настройки хранятся только " +
                    "на этом устройстве и удаляются вместе с приложением."
            )
        }

        InfoCard {
            CardTitle("РАЗРЕШЕНИЯ")
            FeatureRow(
                "🔓", "Root (Magisk / KernelSU)",
                "Чтение процессов и остановка приложений"
            )
            FeatureRow(
                "📋", "Список приложений",
                "Нужен, чтобы показать названия и иконки процессов и игр"
            )
            FeatureRow(
                "🔕", "Режим «Не беспокоить»",
                "Только для Game Booster и только если включишь переключатель"
            )
        }

        InfoCard {
            CardTitle("РУЧНАЯ ОСТАНОВКА И ЗАМОРОЗКА")
            FeatureRow(
                "🔒", "Защищено всегда",
                "Ядро Android, SystemUI, настройки, телефон и звонки, активные лаунчер и клавиатура: их нельзя ни остановить, ни заморозить"
            )
            FeatureRow(
                "⚠", "Разрешено с предупреждением",
                "Системные и Google-приложения: перед действием приложение предупредит об опасности"
            )
            FeatureRow(
                "✅", "Без ограничений",
                "Обычные приложения, которые ты установил сам"
            )
        }

        InfoCard {
            CardTitle("АВТООЧИСТКА НЕ ТРОГАЕТ")
            FeatureRow("✔", "Системные приложения", "Они исключены из очистки полностью")
            FeatureRow("✔", "Выбранную игру", "Её процессы остаются нетронутыми")
            FeatureRow("✔", "Лаунчер и клавиатуру", "Чтобы экран и ввод не пропали")
            FeatureRow("✔", "VPN и спецвозможности", "Эти службы нельзя останавливать без последствий")
            FeatureRow(
                "✔", "Важные для Android приложения",
                "Музыка, навигация и звонок остаются, очищаются только фоновые"
            )
            FeatureRow(
                "✔", "${ProcessRepository.PROTECTED.size} критичных пакетов",
                "Ядро Android, SystemUI, настройки, телефон и звонки"
            )
        }

        InfoCard {
            CardTitle("СЕЙЧАС ПОД ЗАЩИТОЙ")
            if (protectedNow.isEmpty()) {
                Body("Список появится, когда будет получен root и загрузятся процессы.")
            } else {
                Text(
                    "${protectedNow.size} запущенных приложений",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Body(protectedNow.joinToString(", ") { it.label })
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { showPackages = !showPackages }) {
                Text(
                    if (showPackages) "Скрыть список пакетов" else "Показать защищённые пакеты",
                    color = Green
                )
            }

            if (showPackages) {
                ProcessRepository.PROTECTED.sorted().forEach {
                    Text(
                        it,
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        InfoCard {
            CardTitle("СОВЕТЫ")
            listOf(
                "Принудительная остановка может сбросить несохранённые данные в приложении.",
                "Системные и Google-приложения замораживай по одному, так проще понять, что именно перестало работать.",
                "Если телефон повёл себя странно после заморозки, открой «Заморозка приложений» и нажми «Разморозить всё».",
                "Перед запуском игры закрой то, что нужно сохранить, затем нажимай START BOOST."
            ).forEach {
                Text("•  $it", color = BodyColor, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

// ---------- Лицензии ----------

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    PageColumn("ЮРИДИЧЕСКОЕ", "Лицензии", onBack) {

        InfoCard {
            CardTitle("ИСПОЛЬЗУЕМЫЕ БИБЛИОТЕКИ")
            FeatureRow("K", "Kotlin", "JetBrains · Apache License 2.0")
            FeatureRow("🧩", "Jetpack Compose и AndroidX", "Google · Apache License 2.0")
            FeatureRow("🎨", "Material 3", "Google · Apache License 2.0")
            FeatureRow("🔓", "libsu", "John Wu (topjohnwu) · Apache License 2.0")
        }

        InfoCard {
            CardTitle("ЛОГОТИПЫ И НАЗВАНИЯ")
            Body(
                "Kotlin, Java, Claude и другие названия принадлежат их владельцам. " +
                    "Они упоминаются только для указания технологий и инструментов, " +
                    "использованных при создании приложения."
            )
        }

        Text(
            "${AppInfo.NAME} © 2026 ${AppInfo.DEVELOPER}",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

// ---------- Обратная связь ----------

@Composable
fun FeedbackScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current

    PageColumn("ПОДДЕРЖКА", "Обратная связь", onBack) {

        InfoCard {
            CardTitle("НАПИСАТЬ РАЗРАБОТЧИКУ")
            ContactsBlock(context, "${AppInfo.NAME}: обратная связь")
        }

        InfoCard {
            CardTitle("СООБЩИТЬ ОБ ОШИБКЕ")
            Body(
                "Нажми кнопку, чтобы скопировать данные об устройстве и версии приложения, " +
                    "и приложи их к сообщению. Так ошибку найти быстрее."
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val root = when (vm.hasRoot) {
                        true -> "да"
                        false -> "нет"
                        null -> "неизвестно"
                    }
                    val info = "${AppInfo.NAME} ${appVersion(context)}\n" +
                        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                        "${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "Root: $root"
                    copyToClipboard(context, "Aiban-RM", info)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = DarkBackground
                )
            ) {
                Text("Скопировать данные об устройстве", fontWeight = FontWeight.Bold)
            }
        }
    }
}
