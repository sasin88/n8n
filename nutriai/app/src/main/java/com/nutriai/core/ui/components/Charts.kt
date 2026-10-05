package com.nutriai.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nutriai.core.ui.format.Labels

/** Anillo de calorías consumidas frente al objetivo, animado al cambiar. */
@Composable
fun CalorieRing(consumed: Double, target: Int, modifier: Modifier = Modifier, size: Dp = 196.dp) {
    val fraction = if (target > 0) (consumed / target).toFloat() else 0f
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    val over = fraction > 1f
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = if (over) Color(0xFFE0A100) else MaterialTheme.colorScheme.primary
    val remaining = target - consumed
    Box(
        modifier.size(size).semantics { contentDescription = "Consumidas ${consumed.toInt()} de $target kcal" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(progressColor, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Labels.formatNumber(kotlin.math.abs(remaining)),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                if (remaining >= 0) "kcal restantes" else "kcal por encima",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Gráfico de barras sencillo con línea de objetivo opcional. */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    target: Double? = null,
    height: Dp = 160.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val maxValue = (values + listOfNotNull(target)).maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val track = MaterialTheme.colorScheme.surfaceVariant
    val targetColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.6f).coerceAtMost(28.dp.toPx())
            values.forEachIndexed { index, value ->
                val x = slot * index + (slot - barWidth) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
                val h = (value / maxValue).toFloat() * size.height * progress.value
                if (h > 0) {
                    drawRoundRect(color, Offset(x, size.height - h), Size(barWidth, h), CornerRadius(barWidth / 2))
                }
            }
            if (target != null) {
                val y = size.height - (target / maxValue).toFloat() * size.height
                drawLine(
                    targetColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                )
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                labels.forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Gráfico de línea para la evolución del peso. */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val padding = 12.dp.toPx()
        val usableH = size.height - padding * 2
        fun point(i: Int): Offset {
            val x = if (values.size == 1) size.width / 2 else size.width * i / (values.size - 1)
            val y = padding + usableH * (1 - ((values[i] - min) / range).toFloat())
            return Offset(x, y)
        }
        repeat(3) { line ->
            val y = padding + usableH * line / 2
            drawLine(grid.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val visible = ((values.size - 1) * progress.value).toInt()
        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1..visible) lineTo(point(i).x, point(i).y)
        }
        drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        for (i in 0..visible) drawCircle(color, 4.dp.toPx(), point(i))
    }
}
