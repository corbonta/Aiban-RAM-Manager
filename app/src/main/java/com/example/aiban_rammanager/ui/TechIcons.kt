package com.example.aiban_rammanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Все иконки нарисованы кодом, поэтому не нужны файлы ресурсов и дополнительные библиотеки.

val ClaudeOrange = Color(0xFFD97757)

/** Лучистая звезда в стиле значка Claude. */
@Composable
fun ClaudeIcon(modifier: Modifier = Modifier.size(32.dp), tint: Color = ClaudeOrange) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val rays = 12

        for (i in 0 until rays) {
            val angle = Math.toRadians(i * 360.0 / rays - 90.0)
            val outer = if (i % 2 == 0) r else r * 0.76f
            val inner = r * 0.14f

            drawLine(
                color = tint,
                start = Offset(
                    center.x + (cos(angle) * inner).toFloat(),
                    center.y + (sin(angle) * inner).toFloat()
                ),
                end = Offset(
                    center.x + (cos(angle) * outer).toFloat(),
                    center.y + (sin(angle) * outer).toFloat()
                ),
                strokeWidth = size.minDimension * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Значок Kotlin: квадрат с вырезом справа и градиентом. */
@Composable
fun KotlinIcon(modifier: Modifier = Modifier.size(32.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE44857),
                    Color(0xFFC711E1),
                    Color(0xFF7F52FF)
                ),
                start = Offset(w, 0f),
                end = Offset(0f, h)
            )
        )
    }
}

/** Чашка кофе с паром, как у значка Java. */
@Composable
fun JavaIcon(modifier: Modifier = Modifier.size(32.dp)) {
    val cupBlue = Color(0xFF5382A1)
    val steamRed = Color(0xFFE76F00)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.06f

        // Пар
        for (k in 0..1) {
            val x = w * (0.30f + 0.2f * k)
            val steam = Path().apply {
                moveTo(x, h * 0.40f)
                cubicTo(x - w * 0.10f, h * 0.30f, x + w * 0.10f, h * 0.20f, x, h * 0.08f)
            }
            drawPath(
                path = steam,
                color = steamRed,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Чашка
        drawRoundRect(
            color = cupBlue,
            topLeft = Offset(w * 0.14f, h * 0.48f),
            size = Size(w * 0.52f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Ручка
        drawArc(
            color = cupBlue,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.52f, h * 0.52f),
            size = Size(w * 0.30f, h * 0.22f),
            style = Stroke(width = stroke)
        )

        // Блюдце
        drawLine(
            color = cupBlue,
            start = Offset(w * 0.08f, h * 0.92f),
            end = Offset(w * 0.72f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/** Логотип Aiban-RM: кольцо загрузки памяти и буквы RM. */
@Composable
fun AppLogo(modifier: Modifier = Modifier.size(88.dp)) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(SecondaryBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.07f
            val pad = size.minDimension * 0.16f

            drawArc(
                color = Green.copy(alpha = 0.18f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2, size.height - pad * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Green,
                startAngle = 135f,
                sweepAngle = 190f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2, size.height - pad * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            "RM",
            color = Green,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}
