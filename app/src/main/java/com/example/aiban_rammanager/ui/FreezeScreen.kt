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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private enum class FreezeFilter(val label: String) {
    ALL("Все"),
    FROZEN("Замороженные"),
    ACTIVE("Работают")
}

internal val IceBlue = Color(0xFF7FD8FF)
internal val WarningOrange = Color(0xFFFFB454)

@Composable
fun FreezeScreen(vm: MainViewModel, onBack: () -> Unit) {
    // Список обновляется каждый раз, когда экран открывают
    LaunchedEffect(Unit) { vm.loadFreezeList() }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FreezeFilter.ALL) }
    var pending by remember { mutableStateOf<FreezeApp?>(null) }
    var showSystem by remember { mutableStateOf(false) }

    val frozenCount = vm.freezeApps.count { it.frozen }

    val shown = remember(vm.freezeApps, query, filter, showSystem) {
        vm.freezeApps
            // Замороженные системные приложения показываем всегда, чтобы их можно было вернуть
            .filter { showSystem || !it.isSystem || it.frozen }
            .filter {
                when (filter) {
                    FreezeFilter.ALL -> true
                    FreezeFilter.FROZEN -> it.frozen
                    FreezeFilter.ACTIVE -> !it.frozen
                }
            }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }

    // Подтверждение перед заморозкой
    pending?.let { app ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = CardBackground,
            titleContentColor = Color.White,
            textContentColor = MutedText,
            title = { Text("Заморозить ${app.label}?") },
            text = {
                Column {
                    Text(
                        "Приложение будет остановлено и выключено: оно пропадёт с рабочего стола, " +
                            "не сможет работать в фоне и присылать уведомления. Данные сохранятся. " +
                            "Вернуть его можно здесь же одним переключателем."
                    )
                    if (app.isSystem) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "⚠ Это системное приложение. Его заморозка может сломать часть функций " +
                                "телефона: звонки, уведомления, синхронизацию, магазин приложений " +
                                "или другие службы. Если что-то перестанет работать, просто " +
                                "разморозь его здесь.",
                            color = WarningOrange
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setFrozen(app.pkg, true)
                    pending = null
                }) {
                    Text("Заморозить", color = IceBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text("Отмена", color = MutedText)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        item {
            ScreenHeader("ЗАМОРОЗКА", "Заморозка приложений", onBack)
        }

        // Объяснение и сводка
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "❄ Полная остановка без удаления",
                        color = IceBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Замороженное приложение вообще не работает: не запускается, не ест " +
                            "память и батарею, не показывает уведомления. Размораживаешь, и оно " +
                            "снова как раньше, со всеми данными. Скачивать заново не нужно.",
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Заморожено: $frozenCount из ${vm.freezeApps.size}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (frozenCount > 0) {
                            OutlinedButton(
                                onClick = { vm.unfreezeAll() },
                                enabled = !vm.isFreezeBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)
                            ) {
                                Text("Разморозить всё", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Фильтры и поиск
        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FreezeFilter.values().forEach { item ->
                    FilterPill(
                        label = item.label,
                        selected = item == filter,
                        modifier = Modifier.weight(1f)
                    ) { filter = item }
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = "Системные и Google-приложения",
                subtitle = "⚠ Показывать их в списке. Заморозка может быть опасной",
                checked = showSystem
            ) { showSystem = it }
            Spacer(Modifier.height(6.dp))
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

        // Список
        when {
            vm.isFreezeBusy && vm.freezeApps.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Green)
                }
            }

            vm.freezeApps.isEmpty() -> item {
                CenteredNote(
                    "Приложений для заморозки нет.\n" +
                        "Для заморозки нужен root, проверь статус в «Безопасности»."
                )
            }

            shown.isEmpty() -> item {
                CenteredNote("Ничего не найдено")
            }

            else -> items(shown, key = { it.pkg }) { app ->
                FreezeRow(
                    app = app,
                    enabled = !vm.isFreezeBusy,
                    onToggle = { wantFrozen ->
                        if (wantFrozen) pending = app else vm.setFrozen(app.pkg, false)
                    }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CenteredNote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MutedText, fontSize = 13.sp)
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) IceBlue.copy(alpha = 0.18f) else CardBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) IceBlue else MutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FreezeRow(
    app: FreezeApp,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.frozen) IceBlue.copy(alpha = 0.10f) else CardBackground
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
                        app.frozen -> "❄ Заморожено"
                        app.isSystem -> "⚠ Системное, осторожно"
                        else -> "Работает"
                    },
                    color = when {
                        app.frozen -> IceBlue
                        app.isSystem -> WarningOrange
                        else -> MutedText
                    },
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = app.frozen,
                onCheckedChange = onToggle,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = IceBlue
                )
            )
        }
    }
}
