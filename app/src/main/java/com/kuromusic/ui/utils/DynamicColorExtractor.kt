package com.kuromusic.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for extracting dynamic colors from album art using Palette API
 * Safe implementation with fallback to default dark gray
 */
object DynamicColorExtractor {
    
    private val DEFAULT_COLOR = Color(0xFF121212) // Dark gray fallback
    
    /**
     * Extract vibrant or dark muted color from album art bitmap
     * @param bitmap Album art bitmap
     * @return Extracted Color or default dark gray (#121212)
     */
    suspend fun extractDominantColor(bitmap: Bitmap?): Color = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext DEFAULT_COLOR
        
        try {
            val palette = Palette.from(bitmap).generate()
            
            // Priority order: Vibrant -> Dark Vibrant -> Dark Muted -> Dominant -> Default
            val colorInt = palette.vibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: DEFAULT_COLOR.toArgb()
            
            Color(colorInt)
        } catch (e: Exception) {
            // Always fallback to default on any error
            DEFAULT_COLOR
        }
    }
}
