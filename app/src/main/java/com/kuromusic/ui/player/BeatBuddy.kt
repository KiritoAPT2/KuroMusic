package com.kuromusic.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.kuromusic.constants.BeatBuddyType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BeatBuddy(
    type: BeatBuddyType,
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (type == BeatBuddyType.NONE) return

    val infiniteTransition = rememberInfiniteTransition(label = "beatBuddy")

    val runPeriod = if (isPlaying) 1200f else 4000f

    val runPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = runPeriod.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "runPhase"
    )

    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blinkPhase"
    )

    val radians = runPhase * PI / 180f
    val isBlinking = blinkPhase in 0.44f..0.56f

    val strokeStyle = Stroke(
        width = 2.5f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    Canvas(modifier = modifier.size(48.dp)) {
        val s = size.width / 100f
        val cx = size.width / 2f
        val cy = size.height / 2f + 5f * s

        val swing = 28f * s
        val xOffset = sin(radians).toFloat() * swing
        val charX = cx + xOffset

        val movingLeft = cos(radians).toFloat() < 0f

        val bodyBob = cos(radians * 2f).toFloat() * 1.5f * s
        val bounceAtEdge = (1f - abs(sin(radians).toFloat())) * 2f * s

        withTransform({
            if (movingLeft) {
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(charX, cy))
            }
        }) {
            val drawCy = cy + bodyBob - bounceAtEdge

            when (type) {
                BeatBuddyType.CAT -> drawWalkingCat(
                    charX, drawCy, s, radians.toFloat(),
                    isBlinking, color, strokeStyle
                )
                BeatBuddyType.BEAR -> drawWalkingBear(
                    charX, drawCy, s, radians.toFloat(),
                    isBlinking, color, strokeStyle
                )
                BeatBuddyType.NONE -> {}
            }
        }
    }
}

private fun DrawScope.drawWalkingCat(
    cx: Float, cy: Float, s: Float, phase: Float,
    isBlinking: Boolean, color: Color, stroke: Stroke
) {
    val headR = 11f * s
    val bodyW = 18f * s
    val bodyH = 22f * s
    val bodyTop = cy - bodyH / 2f
    val headCenterY = bodyTop - headR + 2f * s

    val legPhase1 = sin(phase).toFloat()
    val legPhase2 = sin(phase + PI.toFloat()).toFloat()

    drawBody(cx, bodyTop, bodyW, bodyH, color, stroke)

    drawLeg(cx - 6f * s, bodyTop + bodyH, legPhase1 * 5f * s, color, stroke)
    drawLeg(cx + 6f * s, bodyTop + bodyH, legPhase2 * 5f * s, color, stroke)
    drawLeg(cx - 3f * s, bodyTop + bodyH * 0.7f, legPhase2 * 5f * s, color, stroke)
    drawLeg(cx + 3f * s, bodyTop + bodyH * 0.7f, legPhase1 * 5f * s, color, stroke)

    val earPath = Path().apply {
        moveTo(cx - 8f * s, headCenterY - headR + 6f * s)
        lineTo(cx - 11f * s, headCenterY - headR - 8f * s)
        lineTo(cx - 4f * s, headCenterY - headR + 2f * s)
        close()
    }
    drawPath(earPath, color, style = stroke)
    val earPath2 = Path().apply {
        moveTo(cx + 8f * s, headCenterY - headR + 6f * s)
        lineTo(cx + 11f * s, headCenterY - headR - 8f * s)
        lineTo(cx + 4f * s, headCenterY - headR + 2f * s)
        close()
    }
    drawPath(earPath2, color, style = stroke)

    drawCircle(color, headR, Offset(cx, headCenterY), style = stroke)

    val eyeY = headCenterY - 1f * s
    val eyeSpacing = 4.5f * s
    val eyeR = 1.8f * s
    if (isBlinking) {
        val blinkY = eyeY - 1f * s
        drawLine(color, Offset(cx - eyeSpacing - eyeR, blinkY), Offset(cx - eyeSpacing + eyeR, blinkY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + eyeSpacing - eyeR, blinkY), Offset(cx + eyeSpacing + eyeR, blinkY), strokeWidth = 2f, cap = StrokeCap.Round)
    } else {
        drawCircle(color, eyeR, Offset(cx - eyeSpacing, eyeY), style = Fill)
        drawCircle(color, eyeR, Offset(cx + eyeSpacing, eyeY), style = Fill)
    }

    val noseR = 1.2f * s
    drawCircle(color, noseR, Offset(cx, headCenterY + 3f * s), style = Fill)

    val mouthPath = Path().apply {
        moveTo(cx - 4f * s, headCenterY + 6f * s)
        quadraticTo(cx - 1f * s, headCenterY + 10f * s, cx, headCenterY + 8f * s)
        quadraticTo(cx + 1f * s, headCenterY + 10f * s, cx + 4f * s, headCenterY + 6f * s)
    }
    drawPath(mouthPath, color, style = Stroke(width = 1.5f, cap = StrokeCap.Round))

    val whiskerLen = 10f * s
    val whiskerY = headCenterY + 1f * s
    for (i in -1..1) {
        val wy = whiskerY + i * 3.5f * s
        drawLine(color, Offset(cx - headR + 1f * s, wy), Offset(cx - headR - whiskerLen, wy), strokeWidth = 1.2f, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + headR - 1f * s, wy), Offset(cx + headR + whiskerLen, wy), strokeWidth = 1.2f, cap = StrokeCap.Round)
    }

    val tailAngle = sin(phase * 2f + 1f).toFloat() * 25f
    val tailPath = Path().apply {
        val startX = cx - bodyW / 2f - 1f * s
        val startY = bodyTop + 2f * s
        moveTo(startX, startY)
        cubicTo(
            startX - 10f * s, startY - 5f * s + tailAngle * 0.3f * s,
            startX - 16f * s, startY - 12f * s + tailAngle * s,
            startX - 18f * s, startY - 8f * s + tailAngle * 1.5f * s
        )
    }
    drawPath(tailPath, color, style = Stroke(width = 2f, cap = StrokeCap.Round))
}

private fun DrawScope.drawWalkingBear(
    cx: Float, cy: Float, s: Float, phase: Float,
    isBlinking: Boolean, color: Color, stroke: Stroke
) {
    val headR = 12f * s
    val bodyW = 20f * s
    val bodyH = 24f * s
    val bodyTop = cy - bodyH / 2f
    val headCenterY = bodyTop - headR + 3f * s

    val legPhase1 = sin(phase).toFloat()
    val legPhase2 = sin(phase + PI.toFloat()).toFloat()

    drawBody(cx, bodyTop, bodyW, bodyH, color, stroke)

    drawLeg(cx - 7f * s, bodyTop + bodyH, legPhase1 * 6f * s, color, stroke)
    drawLeg(cx + 7f * s, bodyTop + bodyH, legPhase2 * 6f * s, color, stroke)
    drawLeg(cx - 4f * s, bodyTop + bodyH * 0.65f, legPhase2 * 6f * s, color, stroke)
    drawLeg(cx + 4f * s, bodyTop + bodyH * 0.65f, legPhase1 * 6f * s, color, stroke)

    val earR = 5f * s
    drawCircle(color, earR, Offset(cx - 10f * s, headCenterY - headR + 2f * s), style = stroke)
    drawCircle(color, earR, Offset(cx + 10f * s, headCenterY - headR + 2f * s), style = stroke)
    drawCircle(color.copy(alpha = 0.25f), earR * 0.5f, Offset(cx - 10f * s, headCenterY - headR + 2f * s), style = Fill)
    drawCircle(color.copy(alpha = 0.25f), earR * 0.5f, Offset(cx + 10f * s, headCenterY - headR + 2f * s), style = Fill)

    drawCircle(color, headR, Offset(cx, headCenterY), style = stroke)

    val eyeY = headCenterY - 1f * s
    val eyeSpacing = 5f * s
    val eyeR = 2f * s
    if (isBlinking) {
        val blinkY = eyeY - 1f * s
        drawLine(color, Offset(cx - eyeSpacing - eyeR, blinkY), Offset(cx - eyeSpacing + eyeR, blinkY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + eyeSpacing - eyeR, blinkY), Offset(cx + eyeSpacing + eyeR, blinkY), strokeWidth = 2f, cap = StrokeCap.Round)
    } else {
        drawCircle(color, eyeR, Offset(cx - eyeSpacing, eyeY), style = Fill)
        drawCircle(color, eyeR, Offset(cx + eyeSpacing, eyeY), style = Fill)
    }

    val noseR = 1.5f * s
    drawCircle(color, noseR, Offset(cx, headCenterY + 3.5f * s), style = Fill)

    val mouthPath = Path().apply {
        moveTo(cx - 4f * s, headCenterY + 7f * s)
        quadraticTo(cx - 1f * s, headCenterY + 11f * s, cx, headCenterY + 9f * s)
        quadraticTo(cx + 1f * s, headCenterY + 11f * s, cx + 4f * s, headCenterY + 7f * s)
    }
    drawPath(mouthPath, color, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawBody(
    cx: Float, top: Float, w: Float, h: Float, color: Color, stroke: Stroke
) {
    drawOval(color, topLeft = Offset(cx - w / 2f, top), size = Size(w, h), style = stroke)
    drawOval(color.copy(alpha = 0.12f), topLeft = Offset(cx - w / 2f, top), size = Size(w, h), style = Fill)
}

private fun DrawScope.drawLeg(
    startX: Float, startY: Float, swing: Float, color: Color, stroke: Stroke
) {
    val endX = startX + swing
    val endY = startY + 10f * (startY / 100f)
    drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth = 2.5f, cap = StrokeCap.Round)
}
