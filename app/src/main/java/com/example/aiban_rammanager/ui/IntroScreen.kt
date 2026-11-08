package com.example.aiban_rammanager.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val IntroBackground = Color(0xFF070A08)

/**
 * Анимация запуска. Первый кадр совпадает с системной заставкой (глаз орла в центре
 * на тёмном фоне), поэтому переход получается бесшовным. Дальше глаз сердито щурится,
 * а вокруг него расходится зелёное кольцо. Без надписей, только глаз.
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val glare = remember { Animatable(0f) }
    val pulse = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(140)
        coroutineScope {
            launch { glare.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { pulse.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing)) }
        }
        delay(380)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IntroBackground),
        contentAlignment = Alignment.Center
    ) {
        // Глаз по размеру совпадает с иконкой системной заставки (288 dp) и остаётся по центру
        Canvas(modifier = Modifier.size(288.dp)) {
            drawEagleEye(glare = glare.value, pulse = pulse.value)
        }
    }
}

/** Мягко соединяет две точки квадратичной кривой через кубическую. */
private fun Path.quad(x0: Float, y0: Float, qx: Float, qy: Float, x1: Float, y1: Float) {
    cubicTo(
        x0 + 2f / 3f * (qx - x0), y0 + 2f / 3f * (qy - y0),
        x1 + 2f / 3f * (qx - x1), y1 + 2f / 3f * (qy - y1),
        x1, y1
    )
}

/**
 * Рисует глаз в тех же координатах (108 x 108), что и иконка приложения.
 * glare: 0 = спокойный взгляд как в иконке, 1 = бровь опущена, зрачок сужен.
 * pulse: 0..1, расходящееся кольцо.
 */
private fun DrawScope.drawEagleEye(glare: Float, pulse: Float) {
    val k = size.minDimension / 108f

    // Та же компоновка, что у иконки: масштаб 0.82 вокруг центра (54, 54)
    fun x(v: Float) = (54f + (v - 54f) * 0.82f) * k
    fun y(v: Float) = (54f + (v - 54f) * 0.82f) * k
    fun r(v: Float) = v * 0.82f * k

    // Зелёное свечение
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x5500E676), Color(0x0000E676)),
            center = Offset(x(54f), y(56f)),
            radius = r(38f)
        ),
        radius = r(38f),
        center = Offset(x(54f), y(56f))
    )

    // Расходящееся кольцо
    if (pulse in 0.001f..0.999f) {
        drawCircle(
            color = Color(0xFF00E676).copy(alpha = (1f - pulse) * 0.55f),
            radius = r(26f + 40f * pulse),
            center = Offset(x(54f), y(58f)),
            style = Stroke(width = r(1.8f))
        )
    }

    // Перья над бровью
    val feather = Color(0xFF6B4528)
    fun tuft(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val p = Path().apply {
            moveTo(x(x0), y(y0))
            lineTo(x(x1), y(y1))
            lineTo(x(x2), y(y2))
            close()
        }
        drawPath(p, feather)
    }
    tuft(31f, 38f, 29f, 22f, 45f, 35f)
    tuft(45f, 36f, 50f, 18f, 60f, 36f)
    tuft(60f, 37f, 69f, 23f, 76f, 40f)

    // Белок: обводка и заливка
    val outline = Color(0xFF1A1210)
    drawOval(
        color = outline,
        topLeft = Offset(x(28f) - r(1.5f), y(58f - 21f) - r(1.5f)),
        size = Size(r(52f) + r(3f), r(42f) + r(3f))
    )
    drawOval(
        color = Color(0xFFFFF4D6),
        topLeft = Offset(x(28f), y(58f - 21f)),
        size = Size(r(52f), r(42f))
    )

    // Радужка
    val irisCenter = Offset(x(58f), y(59f))
    drawCircle(Color(0xFFB85C00), radius = r(14.8f), center = irisCenter)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE066), Color(0xFFF08A00)),
            center = irisCenter,
            radius = r(14f)
        ),
        radius = r(14f),
        center = irisCenter
    )

    // Зрачок сужается, когда орёл щурится
    drawCircle(Color(0xFF0A0A0A), radius = r(7f - 2.2f * glare), center = irisCenter)

    // Блики
    drawCircle(Color.White, radius = r(2.6f), center = Offset(x(54f), y(55f)))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = r(1.4f), center = Offset(x(63f), y(63f)))

    // Бровь: с ростом glare опускается и становится злее
    val dyTip = 1.5f * glare
    val dyMid = 3.5f * glare
    val dyEnd = 6f * glare

    val brow = Path().apply {
        moveTo(x(21f), y(41f + dyTip))
        quad(
            x(21f), y(41f + dyTip),
            x(46f), y(28f + dyMid),
            x(91f), y(43f + dyEnd)
        )
        lineTo(x(89f), y(58f + dyEnd))
        quad(
            x(89f), y(58f + dyEnd),
            x(55f), y(50f + dyMid),
            x(21f), y(41f + dyTip)
        )
        close()
    }
    drawPath(brow, outline)
    drawPath(
        brow,
        outline,
        style = Stroke(width = r(4f), join = androidx.compose.ui.graphics.StrokeJoin.Round)
    )

    // Светлая кромка на брови
    val edge = Path().apply {
        moveTo(x(28f), y(38.5f + dyTip))
        quad(
            x(28f), y(38.5f + dyTip),
            x(50f), y(31.5f + dyMid),
            x(84f), y(44.5f + dyEnd)
        )
    }
    drawPath(
        edge,
        Color(0xFF4A3322),
        style = Stroke(
            width = r(1.6f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
}