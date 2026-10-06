package com.nutriai.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nutriai.core.ui.theme.DarkBase
import com.nutriai.core.ui.theme.DarkBlobs
import com.nutriai.core.ui.theme.LightBase
import com.nutriai.core.ui.theme.LightBlobs
import com.nutriai.core.ui.theme.LocalDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/** Estado del desenfoque compartido entre el fondo y las superficies de cristal. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Fondo de la app: un degradado suave con manchas de color que se mueven muy despacio.
 * Las superficies de cristal lo desenfocan (efecto "vidrio" real en Android 12+).
 */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = LocalDarkTheme.current
    val hazeState = remember { HazeState() }
    val transition = rememberInfiniteTransition(label = "blobs")
    val drift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val base = if (dark) DarkBase else LightBase
    val blobs = if (dark) DarkBlobs else LightBlobs
    Box(Modifier.fillMaxSize().background(base)) {
        Canvas(Modifier.fillMaxSize().haze(hazeState)) {
            drawRect(base)
            val w = size.width
            val h = size.height
            val positions = listOf(
                Offset(w * (0.15f + 0.10f * drift), h * 0.12f),
                Offset(w * (0.90f - 0.10f * drift), h * (0.30f + 0.05f * drift)),
                Offset(w * 0.20f, h * (0.62f - 0.06f * drift)),
                Offset(w * (0.85f - 0.05f * drift), h * 0.88f),
            )
            positions.forEachIndexed { i, center ->
                val radius = w * (0.65f + 0.05f * i)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobs[i].copy(alpha = if (dark) 0.55f else 0.75f), Color.Transparent),
                        center = center, radius = radius,
                    ),
                    radius = radius, center = center,
                )
            }
        }
        CompositionLocalProvider(LocalHazeState provides hazeState) { content() }
    }
}

/** Estilo de cristal según el tema. */
@Composable
fun glassStyle(strength: Float = 1f): HazeStyle {
    val dark = LocalDarkTheme.current
    val tint = if (dark) Color(0xFF1C1C22).copy(alpha = 0.42f * strength) else Color.White.copy(alpha = 0.52f * strength)
    return HazeStyle(
        backgroundColor = if (dark) DarkBase else LightBase,
        tint = HazeTint(tint),
        blurRadius = 28.dp,
        noiseFactor = 0.04f,
        fallbackTint = HazeTint(if (dark) Color(0xFF1C1C22).copy(alpha = 0.86f) else Color.White.copy(alpha = 0.82f)),
    )
}

/** Borde con brillo superior, como el canto de un cristal. */
@Composable
fun glassBorderBrush(): Brush {
    val dark = LocalDarkTheme.current
    return Brush.verticalGradient(
        if (dark) listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
        else listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.35f)),
    )
}

/** Aplica cristal (desenfoque del fondo + tinte + borde) a cualquier elemento. */
@Composable
fun Modifier.glass(shape: Shape, strength: Float = 1f, onClick: (() -> Unit)? = null): Modifier {
    val state = LocalHazeState.current
    val style = glassStyle(strength)
    val dark = LocalDarkTheme.current
    val base = this.clip(shape)
    val blurred = if (state != null) {
        base.hazeChild(state = state, style = style)
    } else {
        base.background(if (dark) Color(0xFF1C1C22).copy(alpha = 0.86f) else Color.White.copy(alpha = 0.82f))
    }
    val bordered = blurred.border(1.dp, glassBorderBrush(), shape)
    return if (onClick != null) bordered.clickable(onClick = onClick) else bordered
}
