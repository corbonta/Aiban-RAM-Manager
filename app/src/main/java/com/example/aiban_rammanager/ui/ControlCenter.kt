package com.example.aiban_rammanager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private data class Tile(
    val route: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

private val SystemTiles = listOf(
    Tile("statistics", "▥", "Статистика", "График и история загрузки RAM", Color(0xFF64B5F6)),
    Tile("settings", "⚙", "Настройки", "Интервал, экран, история", Color(0xFFB0BEC5)),
    Tile("security", "◇", "Безопасность", "Статус root и защита системы", Color(0xFF81C784))
)

private val AboutTiles = listOf(
    Tile("about", "ⓘ", "О приложении", "Возможности и технологии", Color(0xFFBA9BFF)),
    Tile("developers", "⌘", "Разработчики и помощники", "Команда и контакты", Color(0xFFFFB454)),
    Tile("support", "♥", "Поддержать нас", "Помочь развитию проекта", Color(0xFFFF7A90)),
    Tile("feedback", "✉", "Обратная связь", "Написать или сообщить об ошибке", Color(0xFF4DD0E1)),
    Tile("licenses", "§", "Лицензии", "Библиотеки и авторские права", Color(0xFF9E9E9E))
)

// ---------- анимации ----------

/** Плавное появление снизу с задержкой, чтобы элементы выезжали по очереди. */
@Composable
private fun Modifier.staggerIn(index: Int): Modifier {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(40L + index * 45L)
        progress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }

    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 40f
    }
}

/** Нажатие с лёгким пружинящим сжатием. */
@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "press"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

// ---------- экран ----------

@Composable
fun ControlCenterScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Шапка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggerIn(0)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ЦЕНТР УПРАВЛЕНИЯ",
                    color = Green,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    AppInfo.NAME,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            TextButton(onClick = onBack) {
                Text("Назад", color = Green)
            }
        }

        StatusCard(vm, Modifier.staggerIn(1))

        SectionTitle("БЫСТРЫЕ ДЕЙСТВИЯ", Modifier.staggerIn(2))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroCard(
                icon = "🎮",
                title = "Game Booster",
                subtitle = "Очистка и запуск игры",
                accent = Green,
                modifier = Modifier
                    .weight(1f)
                    .staggerIn(3)
            ) { onSelect("game_booster") }

            HeroCard(
                icon = "❄",
                title = "Заморозка",
                subtitle = "Выключить без удаления",
                accent = IceBlue,
                badge = vm.settings.frozenByUs.size.takeIf { it > 0 }?.let { "$it ❄" },
                modifier = Modifier
                    .weight(1f)
                    .staggerIn(4)
            ) { onSelect("freeze") }
        }

        SectionTitle("МОНИТОРИНГ И НАСТРОЙКА", Modifier.staggerIn(5))
        SystemTiles.forEachIndexed { i, tile ->
            TileRow(tile, Modifier.staggerIn(6 + i)) { onSelect(tile.route) }
        }

        SectionTitle("О ПРОЕКТЕ", Modifier.staggerIn(9))
        AboutTiles.forEachIndexed { i, tile ->
            TileRow(tile, Modifier.staggerIn(10 + i)) { onSelect(tile.route) }
        }

        Text(
            "${AppInfo.NAME} · ${AppInfo.DEVELOPER}",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .staggerIn(15)
                .padding(top = 18.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = MutedText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(top = 20.dp, bottom = 10.dp, start = 2.dp)
    )
}

// ---------- карточка статуса с кольцом RAM ----------

@Composable
private fun StatusCard(vm: MainViewModel, modifier: Modifier = Modifier) {
    val fraction = vm.mem.usedFraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ring"
    )

    val ringColor by animateColorAsState(
        targetValue = when {
            fraction >= 0.88f -> Color(0xFFFF6B6B)
            fraction >= 0.72f -> WarningOrange
            else -> Green
        },
        animationSpec = tween(500),
        label = "ringColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(92.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.11f
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)

                    drawArc(
                        color = SecondaryBackground,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animated,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Text(
                    if (vm.mem.totalKb > 0) {
                        String.format(Locale.US, "%.0f%%", fraction * 100f)
                    } else {
                        "--"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("ОПЕРАТИВНАЯ ПАМЯТЬ", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Text(
                    if (vm.mem.totalKb > 0) "${fmt(vm.mem.usedKb)} из ${fmt(vm.mem.totalKb)}" else "Считывание...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (vm.mem.totalKb > 0) "Свободно ${fmt(vm.mem.availKb)}" else "",
                    color = ringColor,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(10.dp))

                RootChip(vm.hasRoot)
            }
        }
    }
}

@Composable
private fun RootChip(root: Boolean?) {
    val color = when (root) {
        true -> Green
        false -> Color(0xFFFF6B6B)
        null -> MutedText
    }
    val text = when (root) {
        true -> "Root активен"
        false -> "Нет root"
        null -> "Проверка root..."
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- большие карточки быстрых действий ----------

@Composable
private fun HeroCard(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(138.dp)
            .pressable(onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.13f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 22.sp)
                }

                if (badge != null) {
                    Text(
                        badge,
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = accent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------- строка-плитка ----------

@Composable
private fun TileRow(tile: Tile, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pressable(onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tile.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(tile.icon, color = tile.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tile.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tile.subtitle,
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text("›", color = MutedText, fontSize = 24.sp)
        }
    }
}
