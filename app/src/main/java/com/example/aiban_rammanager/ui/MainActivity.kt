package com.example.aiban_rammanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiban_rammanager.ui.theme.Aiban_RamManagerTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.util.Locale

val Green = Color(0xFF39D98A)
val DarkBackground = Color(0xFF101410)
val CardBackground = Color(0xFF191F1A)
val SecondaryBackground = Color(0xFF222B24)
val MutedText = Color(0xFFA6B2A9)

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Системную заставку убираем сразу: дальше играет наша анимация запуска
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { provider -> provider.remove() }
        }

        // Обновление идёт только пока приложение на экране
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.runLoops()
            }
        }

        setContent {
            Aiban_RamManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Анимация запуска проигрывается один раз, пока под ней уже грузятся данные
                    var showIntro by rememberSaveable { mutableStateOf(true) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        RamScreen(vm)

                        AnimatedVisibility(
                            visible = showIntro,
                            exit = fadeOut(tween(420)) + scaleOut(tween(420), targetScale = 1.08f)
                        ) {
                            IntroScreen(onFinished = { showIntro = false })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Возвращаем прежний режим «Не беспокоить», если Game Booster его менял
        vm.restoreDnd()
    }
}

internal fun fmt(kb: Long): String {
    return if (kb >= 1_048_576) {
        String.format(Locale.US, "%.2f ГБ", kb / 1_048_576.0)
    } else {
        "${kb / 1024} МБ"
    }
}

@Composable
fun RamScreen(vm: MainViewModel) {

    val snack = remember { SnackbarHostState() }
    var currentPage by remember { mutableStateOf("home") }
    var confirmFreeze by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }

    // Сколько выбранных приложений системные или важные: для них показываем предупреждение
    val riskySelected = vm.apps.count { it.pkg in vm.selected && it.isRisky }

    // Настройка «Не выключать экран»
    val view = LocalView.current
    DisposableEffect(vm.settings.keepScreenOn) {
        view.keepScreenOn = vm.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = currentPage != "home") {
        currentPage = if (currentPage == "menu") "home" else "menu"
    }

    // Новое сообщение заменяет старое, очередь не теряется
    LaunchedEffect(Unit) {
        vm.messages.collect { text ->
            snack.currentSnackbarData?.dismiss()
            launch { snack.showSnackbar(text) }
        }
    }

    // Подтверждение заморозки выбранных приложений
    if (confirmFreeze) {
        AlertDialog(
            onDismissRequest = { confirmFreeze = false },
            containerColor = CardBackground,
            titleContentColor = Color.White,
            textContentColor = MutedText,
            title = { Text("Заморозить выбранные (${vm.selected.size})?") },
            text = {
                Column {
                    Text(
                        "Приложения будут остановлены и выключены: они пропадут с рабочего стола, " +
                            "не смогут работать в фоне и присылать уведомления. Данные сохранятся. " +
                            "Вернуть их можно в меню: «Заморозка приложений»."
                    )
                    if (riskySelected > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "⚠ Среди выбранных есть системные или важные приложения ($riskySelected). " +
                                "Их заморозка может сломать звонки, уведомления, синхронизацию " +
                                "или другие функции телефона. Если что-то перестанет работать, " +
                                "разморозь их в меню.",
                            color = WarningOrange
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFreeze = false
                    vm.freezeSelected()
                }) {
                    Text("Заморозить", color = IceBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFreeze = false }) {
                    Text("Отмена", color = MutedText)
                }
            }
        )
    }

    // Предупреждение перед остановкой системных и важных приложений
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            containerColor = CardBackground,
            titleContentColor = Color.White,
            textContentColor = MutedText,
            title = { Text("Остановить системные приложения?") },
            text = {
                Text(
                    "⚠ Среди выбранных есть системные или важные приложения ($riskySelected). " +
                        "Их остановка может ненадолго нарушить работу телефона: пропадут " +
                        "уведомления, синхронизация или часть функций. Обычно система сама " +
                        "перезапускает их, но будь осторожен.",
                    color = WarningOrange
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    vm.stopSelected()
                }) {
                    Text("Остановить", color = Green, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) {
                    Text("Отмена", color = MutedText)
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = {
            SnackbarHost(hostState = snack)
        },
        bottomBar = {
            if (vm.selected.isNotEmpty() && currentPage == "home") {
                Surface(
                    color = CardBackground,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (riskySelected > 0) confirmStop = true else vm.stopSelected()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green,
                                contentColor = DarkBackground
                            )
                        ) {
                            Text(
                                "ОСТАНОВИТЬ (${vm.selected.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { confirmFreeze = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IceBlue,
                                contentColor = DarkBackground
                            )
                        ) {
                            Text(
                                "❄ ЗАМОРОЗИТЬ (${vm.selected.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
        ) {

            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { pageTransition(initialState, targetState) },
                label = "pages"
            ) { page ->
            Column(modifier = Modifier.fillMaxSize()) {

            if (page == "home") {

                Header(vm) {
                    currentPage = "menu"
                }

                MemoryCard(vm)

                SystemFilter(vm)

                when (vm.hasRoot) {
                    null -> Centered(
                        "Проверка Root-доступа...",
                        Modifier.weight(1f)
                    )

                    false -> Centered(
                        "Root-доступ не получен.\n\nВыдай разрешение приложению в Magisk или KernelSU и перезапусти его.",
                        Modifier.weight(1f)
                    )

                    true -> AppList(
                        vm = vm,
                        modifier = Modifier.weight(1f)
                    )
                }

            } else {

                when (page) {

                    "game_booster" -> GameBoosterScreen(
                        vm = vm,
                        onBack = { currentPage = "menu" }
                    )

                    "menu" -> ControlCenterScreen(
                        vm = vm,
                        onBack = { currentPage = "home" },
                        onSelect = { currentPage = it }
                    )

                    "freeze" -> FreezeScreen(
                        vm = vm,
                        onBack = { currentPage = "menu" }
                    )

                    "about" -> AboutScreen(onBack = { currentPage = "menu" })

                    "developers" -> DevelopersScreen(onBack = { currentPage = "menu" })

                    "support" -> SupportScreen(onBack = { currentPage = "menu" })

                    "statistics" -> StatsScreen(
                        vm = vm,
                        onBack = { currentPage = "menu" }
                    )

                    "settings" -> SettingsScreen(
                        vm = vm,
                        onBack = { currentPage = "menu" }
                    )

                    "security" -> SecurityScreen(vm = vm, onBack = { currentPage = "menu" })

                    "feedback" -> FeedbackScreen(vm = vm, onBack = { currentPage = "menu" })

                    "licenses" -> LicensesScreen(onBack = { currentPage = "menu" })

                    else -> ControlCenterScreen(
                        vm = vm,
                        onBack = { currentPage = "home" },
                        onSelect = { currentPage = it }
                    )
                }
            }
            }
            }
        }
    }
}

private fun pageDepth(page: String): Int = when (page) {
    "home" -> 0
    "menu" -> 1
    else -> 2
}

/** Вперёд окно выезжает справа, назад возвращается слева, старое мягко уходит с затуханием. */
private fun pageTransition(from: String, to: String): ContentTransform {
    val forward = pageDepth(to) > pageDepth(from)

    return if (forward) {
        (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 3 } +
            fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 5 } +
                fadeOut(tween(200)))
    } else {
        (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 5 } +
            fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 3 } +
                fadeOut(tween(200)))
    }
}

@Composable
private fun Header(vm: MainViewModel, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(SecondaryBackground)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center
        ) {
            Text("☰", color = Green, fontSize = 23.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "AIBAN-RM",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Text(
                "RAM MANAGER",
                color = MutedText,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = if (vm.hasRoot == true) Green.copy(alpha = 0.13f)
                    else SecondaryBackground
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (vm.hasRoot == true) Green else MutedText)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when (vm.hasRoot) {
                        true -> "ROOT ACTIVE"
                        false -> "ROOT OFF"
                        null -> "CHECKING"
                    },
                    color = if (vm.hasRoot == true) Green else MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MemoryCard(vm: MainViewModel) {
    val fraction = vm.mem.usedFraction.coerceIn(0f, 1f)
    val percent = fraction * 100f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ОПЕРАТИВНАЯ ПАМЯТЬ",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text("● LIVE", color = Green, fontSize = 10.sp)
            }

            Text(
                if (vm.mem.totalKb > 0)
                    String.format(Locale.US, "%.1f%%", percent)
                else "--",
                color = Green,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = Green,
                trackColor = SecondaryBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ИСПОЛЬЗОВАНО", color = MutedText, fontSize = 9.sp)
                    Text(
                        fmt(vm.mem.usedKb),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ВСЕГО RAM", color = MutedText, fontSize = 9.sp)
                    Text(
                        fmt(vm.mem.totalKb),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                "Доступно: ${fmt(vm.mem.availKb)}",
                color = MutedText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SystemFilter(vm: MainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { vm.showSystem = !vm.showSystem }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = vm.showSystem,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = Green)
        )
        Text(
            text = "Показывать системные приложения",
            color = MutedText,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Centered(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MutedText,
            fontSize = 15.sp,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun AppList(vm: MainViewModel, modifier: Modifier = Modifier) {
    val apps = vm.visibleApps

    if (apps.isEmpty()) {
        Centered(
            text = "Запущенные приложения не найдены",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 4.dp,
            bottom = 16.dp
        )
    ) {
        items(apps, key = { it.pkg }) { app ->
            Card(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    app.icon?.let { icon ->
                        androidx.compose.foundation.Image(
                            bitmap = icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(13.dp))
                        )
                    } ?: Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(SecondaryBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "APP",
                            color = Green,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = app.pkg,
                            color = MutedText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${fmt(app.rssKb)}  •  ${
                                String.format(Locale.US, "%.1f%%", app.percent)
                            }",
                            color = Green,
                            fontSize = 13.sp
                        )

                        if (app.isProtected) {
                            Text(
                                text = "Критичное приложение, трогать нельзя",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                        } else if (app.isRisky) {
                            Text(
                                text = "⚠ Системное или важное, осторожно",
                                color = WarningOrange,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Checkbox(
                        checked = app.pkg in vm.selected,
                        onCheckedChange = { vm.toggle(app.pkg) },
                        enabled = !app.isProtected,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Green,
                            uncheckedColor = MutedText,
                            disabledCheckedColor = Green.copy(alpha = 0.4f),
                            disabledUncheckedColor = MutedText.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}
