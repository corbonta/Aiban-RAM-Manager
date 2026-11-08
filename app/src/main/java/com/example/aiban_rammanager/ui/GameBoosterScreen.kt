package com.example.aiban_rammanager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameBoosterScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val profile = vm.gameProfile

    // Монитор обновляется, только пока он виден на экране
    DisposableEffect(profile.monitor) {
        vm.monitorVisible = profile.monitor
        onDispose { vm.monitorVisible = false }
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(vm.gameApps, query) {
        if (query.isBlank()) vm.gameApps
        else vm.gameApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        item {
            ScreenHeader("ИГРОВОЙ ЦЕНТР", "Game Booster", onBack)
        }

        item {
            SelectedGameCard(vm)
        }

        item {
            Spacer(Modifier.height(14.dp))
            SectionLabel("НАСТРОЙКИ ИГРОВОГО РЕЖИМА")
            Spacer(Modifier.height(8.dp))
        }

        item {
            SettingSwitch(
                title = "Smart Cleanup",
                subtitle = "Останавливать фоновые приложения перед игрой",
                checked = profile.cleanup
            ) { vm.updateGameProfile(profile.copy(cleanup = it)) }
        }

        item {
            SettingSwitch(
                title = "Auto Launch",
                subtitle = "Запускать выбранную игру после очистки",
                checked = profile.autoLaunch
            ) { vm.updateGameProfile(profile.copy(autoLaunch = it)) }
        }

        item {
            SettingSwitch(
                title = "Не беспокоить",
                subtitle = "Глушить уведомления на время игры, режим вернётся при возврате в приложение",
                checked = profile.dnd
            ) { vm.updateGameProfile(profile.copy(dnd = it)) }
        }

        item {
            SettingSwitch(
                title = "Live Monitor",
                subtitle = "Следить за RAM и температурой батареи",
                checked = profile.monitor
            ) { vm.updateGameProfile(profile.copy(monitor = it)) }
        }

        if (profile.monitor) {
            item {
                BoosterMonitorCard(vm)
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.startGameBooster() },
                enabled = !vm.isCleaning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = DarkBackground,
                    disabledContainerColor = Green.copy(alpha = 0.35f)
                )
            ) {
                if (vm.isCleaning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("ВЫПОЛНЯЕТСЯ...")
                } else {
                    Text(
                        "START BOOST",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("ВЫБЕРИ ПРИЛОЖЕНИЕ")
            Spacer(Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск приложения", color = MutedText) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Green,
                    unfocusedBorderColor = SecondaryBackground,
                    cursorColor = Green
                )
            )
            Spacer(Modifier.height(6.dp))
        }

        when {
            vm.isLoadingGames -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Green)
                }
            }

            vm.gameApps.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Запускаемые приложения не найдены.\nНажми «Обновить список приложений».",
                        color = MutedText
                    )
                }
            }

            filtered.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ничего не найдено", color = MutedText)
                }
            }

            else -> items(filtered, key = { it.pkg }) { app ->
                GameAppRow(
                    app = app,
                    selected = app.pkg == vm.selectedGamePackage,
                    onClick = { vm.selectGame(app.pkg) }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectedGameCard(vm: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("ВЫБРАННАЯ ИГРА")

            Spacer(Modifier.height(10.dp))

            val selectedGame = vm.gameApps.firstOrNull { it.pkg == vm.selectedGamePackage }

            if (selectedGame != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    selectedGame.icon?.let {
                        Image(
                            bitmap = it,
                            contentDescription = selectedGame.label,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            selectedGame.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            selectedGame.pkg,
                            color = MutedText,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                Text(
                    "Игра пока не выбрана",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { vm.loadGames(refreshIcons = true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)
            ) {
                Text("Обновить список приложений")
            }
        }
    }
}

@Composable
private fun BoosterMonitorCard(vm: MainViewModel) {
    val totalMb = vm.monitorMem.totalKb / 1024
    val usedMb = vm.monitorMem.usedKb.coerceAtLeast(0) / 1024
    val ramFraction = vm.monitorMem.usedFraction.coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "LIVE MONITOR",
                color = Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Оперативная память", color = MutedText, fontSize = 11.sp)
                    Text(
                        if (totalMb > 0) "$usedMb / $totalMb МБ" else "Считывание...",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (totalMb > 0) "${(ramFraction * 100).toInt()}% занято" else "",
                        color = Green,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Батарея", color = MutedText, fontSize = 11.sp)
                    Text(
                        vm.batteryTempC?.let { "${"%.1f".format(it)} °C" } ?: "Нет данных",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Температура аккумулятора",
                        color = MutedText,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { ramFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Green,
                trackColor = SecondaryBackground
            )
        }
    }
}

@Composable
private fun GameAppRow(
    app: GameApp,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Green.copy(alpha = 0.13f) else CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            app.icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        app.isGame -> "Игра Android"
                        app.isSystem -> "Системное приложение"
                        else -> "Приложение"
                    },
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            if (selected) {
                Text(
                    "ВЫБРАНО",
                    color = Green,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                RadioButton(
                    selected = false,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Green,
                        unselectedColor = MutedText
                    )
                )
            }
        }
    }
}
