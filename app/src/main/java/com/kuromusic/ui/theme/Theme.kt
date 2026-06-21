package com.kuromusic.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.kuromusic.constants.PlayerBackgroundStyle
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import com.google.material.color.score.Score

val DefaultThemeColor = Color(0xFF8B5CF6)  // MORADO KURO ✨
val KuroSecondaryColor = Color(0xFFEC4899) // Rosa morado

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KuroMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    expressive: Boolean = true,
    enableDynamicTheme: Boolean = false, // Nuevo parámetro
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(darkTheme, pureBlack, themeColor, enableDynamicTheme) {
        val baseScheme = if (enableDynamicTheme && themeColor != DefaultThemeColor) {
            // Album art disponible: generar scheme completo desde la carátula (textos incluidos)
            SchemeTonalSpot(Hct.fromInt(themeColor.toArgb()), darkTheme, 0.0)
                .toColorScheme()
        } else if (enableDynamicTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Fallback: wallpaper del sistema mientras no hay carátula
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            // No dinámico: usar themeColor normalmente
            SchemeTonalSpot(Hct.fromInt(themeColor.toArgb()), darkTheme, 0.0)
                .toColorScheme()
        }

        // Si el scheme ya viene del album art, dejar que los colores fluyan sin forzar púrpura
        val balancedScheme = if (enableDynamicTheme && themeColor != DefaultThemeColor) {
            baseScheme
        } else {
            baseScheme.copy(
                secondary = if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                tertiary = if (darkTheme) Color(0xFFEFB8C8) else Color(0xFF7D5260),
                outline = if (darkTheme) Color(0xFF938F99) else Color(0xFF79747E),
                onSurfaceVariant = if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                surfaceVariant = if (darkTheme) Color(0xFF49454F) else Color(0xFFE7E0EC)
            )
        }

        balancedScheme.pureBlack(pureBlack, darkTheme)
    }

    val motionScheme = if (expressive) {
        MotionScheme.expressive()
    } else {
        MotionScheme.standard()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = KuroTypography,
        shapes = MaterialTheme.shapes,
        motionScheme = motionScheme,
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    // Redimensionamos a un tamaño ridículamente pequeño para ahorrar CPU
    val smallBitmap = Bitmap.createScaledBitmap(this, 50, 50, false)
    val palette = Palette.from(smallBitmap).maximumColorCount(12).generate()
    
    // Prioridad: Vibrant -> Dominant -> Gray (fallback)
    val intColor = palette.getVibrantColor(
        palette.getDominantColor(android.graphics.Color.GRAY)
    )
    
    // Si el bitmap pequeño no es el original, lo reciclamos para liberar RAM rápido
    if (smallBitmap != this) smallBitmap.recycle()
    
    return Color(intColor)
}

fun Bitmap.extractGradientColors(): List<Color> {
    val config = config
    val finalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this
    }
    val extractedColors = Palette.from(finalBitmap)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xFF4285F4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

object PlayerColorExtractor {
    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int = Color(0xFF595959).toArgb()
    ): List<Color> {
        val extractedColors = palette.swatches
            .associate { it.rgb to it.population }

        val orderedColors = Score.score(extractedColors, 2, fallbackColor, true)
            .sortedByDescending { Color(it).luminance() }

        return if (orderedColors.size >= 2) {
            listOf(Color(orderedColors[0]), Color(orderedColors[1]))
        } else {
            listOf(Color(0xFF595959), Color(0xFF0D0D0D))
        }
    }
}

fun DynamicScheme.toColorScheme() =
    ColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
    )

fun ColorScheme.pureBlack(apply: Boolean, isDarkTheme: Boolean) =
    if (apply && isDarkTheme) {
        copy(
            surface = Color.Black,
            background = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            surfaceVariant = Color.Black,
            surfaceBright = Color.Black,
            surfaceDim = Color.Black,
        )
    } else {
        this
    }

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

object PlayerSliderColors {
    @Composable
    fun getSliderColors(
        textButtonColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ) = SliderDefaults.colors(
        activeTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
        },
        inactiveTrackColor = when {
            useDarkTheme -> Color.Gray.copy(alpha = 0.5f)
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        activeTickColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
        },
        inactiveTickColor = Color.Gray,
        thumbColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
        }
    )
}
