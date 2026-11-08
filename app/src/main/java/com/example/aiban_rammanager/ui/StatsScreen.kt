package com.example.aiban_rammanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StatsPeriod(val label: String, val windowMs: Long) {
    LIVE("5 мин", 5 * 60_000L),
    HOUR("1 час", 60 * 60_000L),
    SIX_HOURS("6 часов", 6 * 60 * 60_000L),
    TWELVE_HOURS("12 часов", 12 * 60 * 60_000L)
}

private val GridColor = Color(0xFF2B352D)

@Composable
fun StatsScreen(vm: MainViewModel, onBack: () -> Unit) {
    var period by remember { mutableStateOf(StatsPeriod.LIVE) }

    // Для «5 мин» берём живые измерения, для остальных периодов сохранённую историю
    val samples = if (period == StatsPeriod.LIVE) {
        vm.live
    } else {
        val from = System.currentTimeMillis() - period.windowMs
        vm.history.filter { it.timeMs >= from }
    }

    val timeFormat = remember(period) {
        SimpleDateFormat(
            if (period == StatsPeriod.LIVE) "HH:mm:ss" else "HH:mm",
            Locale.getDefault()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader("МОНИТОРИНГ", "Статистика", onBack)

        // Выбор периода
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.values().forEach { item ->
                PeriodChip(
                    label = item.label,
                    selected = item == period,
                    modifier = Modifier.weight(1f)
                ) { period = item }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Текущее состояние
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel("СЕЙЧАС")
                Spacer(Modifier.height(6.dp))
                Text(
                    if (vm.mem.totalKb > 0) {
                        String.format(Locale.US, "%.1f%%", vm.mem.usedFraction * 100f)
                    } else {
                        "--"
                    },
                    color = Green,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ЗАНЯТО", color = MutedText, fontSize = 9.sp)
                        Text(
                            fmt(vm.mem.usedKb),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("СВОБОДНО", color = MutedText, fontSize = 9.sp)
                        Text(
                            fmt(vm.mem.availKb),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ВСЕГО", color = MutedText, fontSize = 9.sp)
                        Text(
                            fmt(vm.mem.totalKb),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // График
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("ЗАНЯТАЯ RAM")
                    Text("100%", color = MutedText, fontSize = 9.sp)
                }

                Spacer(Modifier.height(10.dp))

                if (samples.size >= 2) {
                    RamChart(
                        samples = samples,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            timeFormat.format(Date(samples.first().timeMs)),
                            color = MutedText,
                            fontSize = 10.sp
                        )
                        Text(
                            timeFormat.format(Date(samples.last().timeMs)),
                            color = MutedText,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (period != StatsPeriod.LIVE && !vm.settings.saveHistory) {
                                "Сохранение истории выключено в настройках."
                            } else {
                                "Данных пока мало. График появится через несколько обновлений."
                            },
                            color = MutedText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Максимум, минимум, среднее
        if (samples.isNotEmpty()) {
            val maxSample = samples.maxByOrNull { it.usedKb }
            val minSample = samples.minByOrNull { it.usedKb }
            val avgKb = samples.map { it.usedKb }.average().toLong()
            val totalKb = samples.last().totalKb

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel("ЗА ПЕРИОД: ${period.label.uppercase(Locale.getDefault())}")
                        Text("точек: ${samples.size}", color = MutedText, fontSize = 10.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell("МАКСИМУМ", maxSample?.usedKb ?: 0L, totalKb)
                        StatCell("СРЕДНЕЕ", avgKb, totalKb)
                        StatCell("МИНИМУМ", minSample?.usedKb ?: 0L, totalKb)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // История
        SettingSwitch(
            title = "Сохранять историю",
            subtitle = "Одна точка раз в 30 секунд, хранится до 12 часов",
            checked = vm.settings.saveHistory
        ) { vm.settings.saveHistory = it }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { vm.clearHistory() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)
        ) {
            Text("Очистить историю")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Green.copy(alpha = 0.18f) else CardBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Green else MutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatCell(title: String, kb: Long, totalKb: Long) {
    val percent = if (totalKb > 0) kb * 100f / totalKb else 0f

    Column {
        Text(title, color = MutedText, fontSize = 9.sp)
        Text(
            fmt(kb),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            String.format(Locale.US, "%.1f%%", percent),
            color = Green,
            fontSize = 11.sp
        )
    }
}

/** Линейный график занятой памяти в процентах от общей RAM, шкала 0–100%. */
@Composable
private fun RamChart(samples: List<RamSample>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Сетка: 0, 25, 50, 75 и 100 процентов
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(
                color = GridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (samples.size < 2) return@Canvas

        val t0 = samples.first().timeMs
        val span = (samples.last().timeMs - t0).coerceAtLeast(1L)

        val line = Path()
        samples.forEachIndexed { index, s ->
            val x = (s.timeMs - t0).toFloat() / span * w
            val fraction = if (s.totalKb > 0) {
                (s.usedKb.toFloat() / s.totalKb).coerceIn(0f, 1f)
            } else {
                0f
            }
            val y = h - fraction * h
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }

        val area = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(area, color = Green.copy(alpha = 0.15f))
        drawPath(
            line,
            color = Green,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
