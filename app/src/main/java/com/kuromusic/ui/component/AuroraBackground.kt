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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max


/**
 * Aurora Borealis Background Component
 * 
 * Enhanced with Purple Electric (#8A2BE2) + Dynamic Color gradient
 * Theme-aware alpha and smooth 1000ms color transitions
 */
@Composable
fun AuroraBackground(
    isVisible: Boolean,
    color: Color,
    scrollOffsetProvider: () -> Float,
    maxScrollOffset: Float = 1000f,
    content: @Composable () -> Unit
) {
    // Detect theme properly
    val isDarkTheme = isSystemInDarkTheme()
    
        // Smooth 1000ms color transition for dynamic color
    val animatedDynamicColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 1000),
        label = "aurora_dynamic_color"
    )
    
    // State for animated offsets
    var offset1 by remember { mutableFloatStateOf(0f) }
    var offset2 by remember { mutableFloatStateOf(0f) }
    var offset3 by remember { mutableFloatStateOf(-50f) }

    // Constants for animation
    val duration1 = 10000L
    val duration2 = 13000L
    val duration3 = 15000L

    // Background calculation loop
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - startTime

                // Calculate sinusoidal motion suitable for breathing effect
                // Using sin() creates a smooth -1 to 1 oscillation, scaled to range
                
                // Offset 1: 0f to 100f
                val progress1 = (elapsed % (duration1 * 2)).toFloat() / duration1
                val raw1 = if (progress1 <= 1f) progress1 else 2f - progress1
                offset1 = raw1 * 100f

                // Offset 2: 0f to -80f
                val progress2 = (elapsed % (duration2 * 2)).toFloat() / duration2
                val raw2 = if (progress2 <= 1f) progress2 else 2f - progress2
                offset2 = raw2 * -80f

                // Offset 3: -50f to 50f
                val progress3 = (elapsed % (duration3 * 2)).toFloat() / duration3
                val raw3 = if (progress3 <= 1f) progress3 else 2f - progress3
                offset3 = -50f + (raw3 * 100f)

                delay(66) // ~15fps update rate (High Performance)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val purpleElectric = Color(0xFF8A2BE2)
                
                // --- CACHED BRUSHES ---
                // We create brushes ONCE per color change/resize, not per frame.
                // We center them at (0,0) and use translation to move them.
                
                val baseRadius = size.width * 0.8f
                
                // Helper to create a cached brush
                fun createAuroraBrush(color1: Color, color2: Color, alpha: Float): Brush {
                     // Colors for light theme logic handled inside draw? 
                     // No, "drawWithCache" rebuilds if state read inside it changes. 
                     // We need to resolve colors here.
                     
                     val lightPrimary = Color(0xFFE6D7FF) 
                     val lightSecondary = Color(0xFFD7EFFF)
                     
                     val (finalPrimary, finalSecondary, finalAlpha) = if (isDarkTheme) {
                        Triple(color1, color2, alpha)
                     } else {
                        Triple(lightPrimary, lightSecondary, 0.3f) 
                     }
                     
                     return Brush.radialGradient(
                        colors = listOf(
                            finalPrimary.copy(alpha = finalAlpha),
                            finalSecondary.copy(alpha = finalAlpha * 0.8f),
                            finalPrimary.copy(alpha = finalAlpha * 0.6f),
                            finalSecondary.copy(alpha = finalAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset.Zero, // Center at local origin
                        radius = baseRadius // Use calculated radius
                     )
                }

                // Cache the 3 brushes
                // Note: The specific radii used in previous code were:
                // Circle 3 (Top): baseRadius * 1.8f
                // Circle 1 (Left): baseRadius
                // Circle 2 (Right): baseRadius * 0.9f
                
                // Brush 3 (Top Center)
                val brush3 = Brush.radialGradient(
                        colors = listOf(
                            (if(isDarkTheme) purpleElectric else Color(0xFFE6D7FF)).copy(alpha = if(isDarkTheme) 0.3f else 0.3f),
                            (if(isDarkTheme) animatedDynamicColor else Color(0xFFD7EFFF)).copy(alpha = if(isDarkTheme) 0.3f*0.8f else 0.3f*0.8f),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = baseRadius * 1.8f
                )
                 // Wait, simplifying gradient logic for cache stability
                 // Using the complex gradient logic from before:
                 
                 val brushTop = createAuroraBrush(
                    purpleElectric, 
                    animatedDynamicColor, 
                    0.3f
                 )
                 // Override radius for Top brush in helper? No, helper uses fixed radius. 
                 // Let's manually create them to be precise.
                 
                 // 1. Top Brush (Primary: Purple, Secondary: Dynamic)
                 val topBrush = Brush.radialGradient(
                    colors = run {
                        val p = if(isDarkTheme) purpleElectric else Color(0xFFE6D7FF)
                        val s = if(isDarkTheme) animatedDynamicColor else Color(0xFFD7EFFF)
                        val a = if(isDarkTheme) 0.3f else 0.3f
                        listOf(p.copy(alpha=a), s.copy(alpha=a*0.8f), p.copy(alpha=a*0.6f), s.copy(alpha=a*0.4f), Color.Transparent)
                    },
                    center = Offset.Zero,
                    radius = baseRadius * 1.8f
                 )

                 // 2. Left Brush (Primary: Purple, Secondary: Dynamic)
                 val leftBrush = Brush.radialGradient(
                    colors = run {
                        val p = if(isDarkTheme) purpleElectric else Color(0xFFE6D7FF)
                        val s = if(isDarkTheme) animatedDynamicColor else Color(0xFFD7EFFF)
                        val a = if(isDarkTheme) 0.35f else 0.3f
                        listOf(p.copy(alpha=a), s.copy(alpha=a*0.8f), p.copy(alpha=a*0.6f), s.copy(alpha=a*0.4f), Color.Transparent)
                    },
                    center = Offset.Zero,
                    radius = baseRadius
                 )

                 // 3. Right Brush (Primary: Dynamic, Secondary: Purple mixed)
                 val rightBrush = Brush.radialGradient(
                    colors = run {
                        val p = if(isDarkTheme) animatedDynamicColor else Color(0xFFE6D7FF)
                        val s = if(isDarkTheme) purpleElectric.copy(alpha=0.6f).compositeOver(Color.White) else Color(0xFFD7EFFF)
                        val a = if(isDarkTheme) 0.3f else 0.3f
                        listOf(p.copy(alpha=a), s.copy(alpha=a*0.8f), p.copy(alpha=a*0.6f), s.copy(alpha=a*0.4f), Color.Transparent)
                    },
                    center = Offset.Zero,
                    radius = baseRadius * 0.9f
                 )

                onDrawBehind {
                    // Read buffer provider here in draw phase
                    val scrollOffset = scrollOffsetProvider()
                    val opacity = max(0f, 1f - (scrollOffset / maxScrollOffset))
                    val translationY = -(scrollOffset * 0.5f)

                    if (isVisible && opacity > 0) {
                        // Drawing cached brushes with translation
                        
                        // Top Circle
                        drawContext.canvas.save()
                        drawContext.canvas.translate(
                            dx = size.width * 0.5f + offset3,
                            dy = -size.height * 0.25f + offset2 + translationY
                        )
                        // Scale check? No, radius is baked into brush.
                        // We just draw a circle at (0,0) with large radius to fill the brush area.
                        drawCircle(
                            brush = topBrush,
                            radius = baseRadius * 1.8f,
                            center = Offset.Zero,
                            alpha = opacity, // Master fade
                            blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver
                        )
                        drawContext.canvas.restore()

                        // Left Circle
                        drawContext.canvas.save()
                        drawContext.canvas.translate(
                             dx = -size.width * 0.2f + offset1,
                             dy = -size.height * 0.1f + offset2 + translationY
                        )
                        drawCircle(
                            brush = leftBrush,
                            radius = baseRadius,
                            center = Offset.Zero,
                            alpha = opacity,
                            blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver
                        )
                        drawContext.canvas.restore()

                        // Right Circle
                        drawContext.canvas.save()
                        drawContext.canvas.translate(
                             dx = size.width * 1.1f - offset2,
                             dy = -size.height * 0.2f + offset1 + translationY
                        )
                        drawCircle(
                            brush = rightBrush,
                            radius = baseRadius * 0.9f,
                            center = Offset.Zero,
                            alpha = opacity,
                            blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver
                        )
                        drawContext.canvas.restore()
                    }
                }
            }

    ) {
         // Content needs to be drawn? 
         // Actually AuroraBackground usually wraps content. 
         // But the previous implementation used Canvas(fillMaxSize) inside the Box.
         // If I use drawWithCache on the Box, I can draw the aurora behind the content 
         // using onDrawBehind.
         
         content()
    }
}

/**
 * Enhanced Aurora Circle with dual-color radial gradient
 * Mixes Purple Electric (#8A2BE2) with dynamic color
 * Theme-aware: subtle soft pastels for light theme, vivid for dark
 */
private fun DrawScope.drawEnhancedAuroraCircle(
    primaryColor: Color,
    secondaryColor: Color,
    center: Offset,
    radius: Float,
    alpha: Float,
    isDarkTheme: Boolean = true
) {
    // Soft pastel colors for light theme (elegant and subtle)
    val lightPrimary = Color(0xFFE6D7FF) // Soft lilac  
    val lightSecondary = Color(0xFFD7EFFF) // Sky blue
    
    // Choose colors based on theme
    val (finalPrimary, finalSecondary, finalAlpha) = if (isDarkTheme) {
        Triple(primaryColor, secondaryColor, alpha)
    } else {
        // Light theme: soft pastels with 30% opacity for subtle elegance
        Triple(lightPrimary, lightSecondary, 0.3f) // Subtle, not overpowering
    }
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                finalPrimary.copy(alpha = finalAlpha),
                finalSecondary.copy(alpha = finalAlpha * 0.8f),
                finalPrimary.copy(alpha = finalAlpha * 0.6f),
                finalSecondary.copy(alpha = finalAlpha * 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius,
        blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver // Normal blend for light theme
    )
}
