package com.kuromusic.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.constants.DefaultWaveformBarsCount
import com.kuromusic.constants.WaveformBarsCountKey
import com.kuromusic.utils.makeTimeString
import com.kuromusic.utils.rememberPreference
import kotlin.math.abs

@Composable
fun WaveformSeekbar(
    positionState: androidx.compose.runtime.MutableLongState,
    durationState: androidx.compose.runtime.MutableLongState,
    onBackgroundColor: Color,
    accentColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val fftData by playerConnection.visualizerEngine.fftData.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()

    val barCount by rememberPreference(WaveformBarsCountKey, DefaultWaveformBarsCount)

    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val currentPositionText by remember {
        derivedStateOf { makeTimeString(sliderPosition ?: positionState.longValue) }
    }
    val totalDurationText by remember {
        derivedStateOf { makeTimeString(durationState.longValue) }
    }

    val progress by remember {
        derivedStateOf {
            if (durationState.longValue > 0) {
                (sliderPosition ?: positionState.longValue).toFloat() / durationState.longValue
            } else 0f
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        if (width > 0) {
                            val ratio = (offset.x / width).coerceIn(0f, 1f)
                            val seekPos = (ratio * durationState.longValue).toLong()
                            sliderPosition = seekPos
                            playerConnection.player.seekTo(seekPos)
                            positionState.longValue = seekPos
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            if (width > 0) {
                                val ratio = (offset.x / width).coerceIn(0f, 1f)
                                sliderPosition = (ratio * durationState.longValue).toLong()
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat()
                            if (width > 0) {
                                val ratio = (change.position.x / width).coerceIn(0f, 1f)
                                sliderPosition = (ratio * durationState.longValue).toLong()
                            }
                        },
                        onDragEnd = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                positionState.longValue = it
                            }
                            sliderPosition = null
                        },
                        onDragCancel = { sliderPosition = null }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                val bars = barCount.coerceIn(16, 128)
                val gapPx = 2.dp.toPx()
                val totalGaps = gapPx * (bars - 1)
                val barWidth = ((size.width - totalGaps) / bars).coerceIn(1f, 8f)
                val maxHeight = size.height
                val activeColor = if (accentColor != Color.Unspecified) accentColor else onBackgroundColor
                val inactiveColor = onBackgroundColor.copy(alpha = 0.15f)
                val progressColor = onBackgroundColor

                val useFft = isPlaying && fftData.isNotEmpty()

                for (i in 0 until bars) {
                    val amplitude = if (useFft) {
                        val fftIndex = (i.toFloat() / bars * fftData.size).toInt()
                            .coerceIn(0, fftData.size - 1)
                        fftData[fftIndex].coerceIn(0.05f, 1f)
                    } else {
                        val sinVal = abs(kotlin.math.sin(i * 0.5f))
                        (sinVal * 0.3f + 0.1f).coerceIn(0.05f, 0.4f)
                    }

                    val barHeight = (amplitude * maxHeight).coerceAtLeast(2f)
                    val x = i * (barWidth + gapPx)
                    val y = maxHeight - barHeight
                    val barProgress = i.toFloat() / bars

                    val color = when {
                        barProgress <= progress -> activeColor
                        else -> inactiveColor
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }

                // subtle progress line at playhead position
                val playheadX = (progress * size.width).coerceIn(0f, size.width)
                drawRoundRect(
                    color = progressColor.copy(alpha = 0.4f),
                    topLeft = Offset(playheadX - 1f, 0f),
                    size = Size(3f, maxHeight),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentPositionText,
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor,
            )
            Text(
                text = totalDurationText,
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor,
            )
        }
    }
}
