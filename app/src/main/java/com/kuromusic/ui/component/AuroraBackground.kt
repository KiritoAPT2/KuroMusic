package com.kuromusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max

private data class AuroraBrushColors(
    val topPrimary: Color,
    val topSecondary: Color,
    val topAlpha: Float,
    val leftPrimary: Color,
    val leftSecondary: Color,
    val leftAlpha: Float,
    val rightPrimary: Color,
    val rightSecondary: Color,
    val rightAlpha: Float,
    val blendMode: BlendMode
)

private val PurpleElectric = Color(0xFF8A2BE2)
private val LightPrimary = Color(0xFFE6D7FF)
private val LightSecondary = Color(0xFFD7EFFF)

@Composable
fun AuroraBackground(
    isVisible: Boolean,
    color: Color,
    scrollOffsetProvider: () -> Float,
    maxScrollOffset: Float = 1000f,
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    val animatedDynamicColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 1000),
        label = "aurora_dynamic_color"
    )

    var offset1 by remember { mutableFloatStateOf(0f) }
    var offset2 by remember { mutableFloatStateOf(0f) }
    var offset3 by remember { mutableFloatStateOf(-50f) }

    val duration1 = 10000L
    val duration2 = 13000L
    val duration3 = 15000L

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime

                val progress1 = (elapsed % (duration1 * 2)).toFloat() / duration1
                val raw1 = if (progress1 <= 1f) progress1 else 2f - progress1
                offset1 = raw1 * 100f

                val progress2 = (elapsed % (duration2 * 2)).toFloat() / duration2
                val raw2 = if (progress2 <= 1f) progress2 else 2f - progress2
                offset2 = raw2 * -80f

                val progress3 = (elapsed % (duration3 * 2)).toFloat() / duration3
                val raw3 = if (progress3 <= 1f) progress3 else 2f - progress3
                offset3 = -50f + (raw3 * 100f)

                delay(100)
            }
        }
    }

    val brushColors = remember(animatedDynamicColor, isDarkTheme) {
        if (isDarkTheme) {
            val defaultColor = Color(0xFF121212)
            val isDynamic = animatedDynamicColor != defaultColor
            val primary = if (isDynamic) animatedDynamicColor else PurpleElectric
            val secondary = if (isDynamic) animatedDynamicColor else defaultColor
            AuroraBrushColors(
                topPrimary = primary,
                topSecondary = secondary,
                topAlpha = 0.3f,
                leftPrimary = primary,
                leftSecondary = secondary,
                leftAlpha = 0.35f,
                rightPrimary = secondary,
                rightSecondary = primary.copy(alpha = 0.6f).compositeOver(Color.White),
                rightAlpha = 0.3f,
                blendMode = BlendMode.Plus
            )
        } else {
            AuroraBrushColors(
                topPrimary = LightPrimary,
                topSecondary = LightSecondary,
                topAlpha = 0.3f,
                leftPrimary = LightPrimary,
                leftSecondary = LightSecondary,
                leftAlpha = 0.3f,
                rightPrimary = LightPrimary,
                rightSecondary = LightSecondary,
                rightAlpha = 0.3f,
                blendMode = BlendMode.SrcOver
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawBehind {
                val baseRadius = size.width * 0.8f
                val scrollOffset = scrollOffsetProvider()
                val opacity = max(0f, 1f - (scrollOffset / maxScrollOffset))
                val translationY = -(scrollOffset * 0.5f)

                if (isVisible && opacity > 0) {
                    fun makeBrush(p: Color, s: Color, a: Float, radius: Float) = Brush.radialGradient(
                        colors = listOf(
                            p.copy(alpha = a),
                            s.copy(alpha = a * 0.8f),
                            p.copy(alpha = a * 0.6f),
                            s.copy(alpha = a * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = radius
                    )

                    // Top Circle
                    drawContext.canvas.save()
                    drawContext.canvas.translate(
                        dx = size.width * 0.5f + offset3,
                        dy = -size.height * 0.25f + offset2 + translationY
                    )
                    drawCircle(
                        brush = makeBrush(brushColors.topPrimary, brushColors.topSecondary, brushColors.topAlpha, baseRadius * 1.8f),
                        radius = baseRadius * 1.8f,
                        center = Offset.Zero,
                        alpha = opacity,
                        blendMode = brushColors.blendMode
                    )
                    drawContext.canvas.restore()

                    // Left Circle
                    drawContext.canvas.save()
                    drawContext.canvas.translate(
                        dx = -size.width * 0.2f + offset1,
                        dy = -size.height * 0.1f + offset2 + translationY
                    )
                    drawCircle(
                        brush = makeBrush(brushColors.leftPrimary, brushColors.leftSecondary, brushColors.leftAlpha, baseRadius),
                        radius = baseRadius,
                        center = Offset.Zero,
                        alpha = opacity,
                        blendMode = brushColors.blendMode
                    )
                    drawContext.canvas.restore()

                    // Right Circle
                    drawContext.canvas.save()
                    drawContext.canvas.translate(
                        dx = size.width * 1.1f - offset2,
                        dy = -size.height * 0.2f + offset1 + translationY
                    )
                    drawCircle(
                        brush = makeBrush(brushColors.rightPrimary, brushColors.rightSecondary, brushColors.rightAlpha, baseRadius * 0.9f),
                        radius = baseRadius * 0.9f,
                        center = Offset.Zero,
                        alpha = opacity,
                        blendMode = brushColors.blendMode
                    )
                    drawContext.canvas.restore()
                }
            }
    ) {
        content()
    }
}
