package com.example.aiban_rammanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings = vm.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader("ПАРАМЕТРЫ", "Настройки", onBack)

        // Интервал обновления
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Интервал обновления",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Как часто читать RAM и список процессов",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "${settings.refreshSec} сек",
                        color = Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Slider(
                    value = settings.refreshSec.toFloat(),
                    onValueChange = { settings.refreshSec = it.roundToInt() },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = Green,
                        activeTrackColor = Green,
                        inactiveTrackColor = SecondaryBackground
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        SettingSwitch(
            title = "Не выключать экран",
            subtitle = "Экран не гаснет, пока приложение открыто",
            checked = settings.keepScreenOn
        ) { settings.keepScreenOn = it }

        SettingSwitch(
            title = "Системные приложения",
            subtitle = "Показывать их в главном списке процессов",
            checked = settings.showSystem
        ) { settings.showSystem = it }

        SettingSwitch(
            title = "Сохранять историю RAM",
            subtitle = "Нужна для графиков на экране статистики",
            checked = settings.saveHistory
        ) { settings.saveHistory = it }

        Spacer(Modifier.height(12.dp))

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
