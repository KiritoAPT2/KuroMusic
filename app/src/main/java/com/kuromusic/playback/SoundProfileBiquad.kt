package com.kuromusic.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class BiquadCoefficients(
    val b0: Float,
    val b1: Float,
    val b2: Float,
    val a1: Float,
    val a2: Float,
) {
    companion object {
        val BYPASS = BiquadCoefficients(b0 = 1f, b1 = 0f, b2 = 0f, a1 = 0f, a2 = 0f)
    }
}

fun lerp(a: BiquadCoefficients, b: BiquadCoefficients, t: Float): BiquadCoefficients =
    BiquadCoefficients(
        b0 = a.b0 + (b.b0 - a.b0) * t,
        b1 = a.b1 + (b.b1 - a.b1) * t,
        b2 = a.b2 + (b.b2 - a.b2) * t,
        a1 = a.a1 + (b.a1 - a.a1) * t,
        a2 = a.a2 + (b.a2 - a.a2) * t,
    )

class BiquadFilter {
    private lateinit var x1: FloatArray
    private lateinit var x2: FloatArray
    private lateinit var y1: FloatArray
    private lateinit var y2: FloatArray

    var currentCoeffs: BiquadCoefficients = BiquadCoefficients.BYPASS
        private set

    private var startCoeffs: BiquadCoefficients = BiquadCoefficients.BYPASS
    private var targetCoeffs: BiquadCoefficients = BiquadCoefficients.BYPASS
    private var transitionSamples = 0
    private var samplesProcessed = 0

    fun init(channels: Int) {
        if (!::x1.isInitialized || x1.size != channels) {
            x1 = FloatArray(channels)
            x2 = FloatArray(channels)
            y1 = FloatArray(channels)
            y2 = FloatArray(channels)
        }
    }

    fun setCoefficients(coeffs: BiquadCoefficients) {
        currentCoeffs = coeffs
        startCoeffs = coeffs
        targetCoeffs = coeffs
        transitionSamples = 0
        samplesProcessed = 0
    }

    fun transitionTo(newCoeffs: BiquadCoefficients, sampleRate: Int, durationMs: Int = 100) {
        startCoeffs = currentCoeffs
        targetCoeffs = newCoeffs
        transitionSamples = (sampleRate * durationMs) / 1000
        samplesProcessed = 0
    }

    fun process(input: FloatArray, output: FloatArray, channels: Int) {
        var cb0 = currentCoeffs.b0
        var cb1 = currentCoeffs.b1
        var cb2 = currentCoeffs.b2
        var ca1 = currentCoeffs.a1
        var ca2 = currentCoeffs.a2

        for (i in input.indices step channels) {
            if (transitionSamples > 0 && samplesProcessed < transitionSamples) {
                val t = (samplesProcessed.toFloat() / transitionSamples).coerceAtMost(1f)
                cb0 = startCoeffs.b0 + (targetCoeffs.b0 - startCoeffs.b0) * t
                cb1 = startCoeffs.b1 + (targetCoeffs.b1 - startCoeffs.b1) * t
                cb2 = startCoeffs.b2 + (targetCoeffs.b2 - startCoeffs.b2) * t
                ca1 = startCoeffs.a1 + (targetCoeffs.a1 - startCoeffs.a1) * t
                ca2 = startCoeffs.a2 + (targetCoeffs.a2 - startCoeffs.a2) * t
                samplesProcessed++
                if (samplesProcessed >= transitionSamples) {
                    currentCoeffs = targetCoeffs
                }
            }

            for (ch in 0 until channels) {
                val s = input[i + ch]
                val out = cb0 * s + cb1 * x1[ch] + cb2 * x2[ch] - ca1 * y1[ch] - ca2 * y2[ch]

                x2[ch] = x1[ch]
                x1[ch] = s
                y2[ch] = y1[ch]
                y1[ch] = out
                output[i + ch] = out
            }
        }
    }

    fun reset() {
        if (::x1.isInitialized) {
            x1.fill(0f)
            x2.fill(0f)
            y1.fill(0f)
            y2.fill(0f)
        }
    }
}

object BiquadCoefficientsFactory {

    fun lowShelf(freq: Float, gainDb: Float, q: Float, sampleRate: Int): BiquadCoefficients {
        val a = 10f.pow(gainDb / 40f)
        val w0 = 2f * PI.toFloat() * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2f * q)
        val sqrtA = sqrt(a)

        val b0 = a * ((a + 1f) - (a - 1f) * cosW0 + 2f * sqrtA * alpha)
        val b1 = 2f * a * ((a - 1f) - (a + 1f) * cosW0)
        val b2 = a * ((a + 1f) - (a - 1f) * cosW0 - 2f * sqrtA * alpha)
        val a0 = (a + 1f) + (a - 1f) * cosW0 + 2f * sqrtA * alpha
        val a1 = -2f * ((a - 1f) + (a + 1f) * cosW0)
        val a2 = (a + 1f) + (a - 1f) * cosW0 - 2f * sqrtA * alpha

        return BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun highShelf(freq: Float, gainDb: Float, q: Float, sampleRate: Int): BiquadCoefficients {
        val a = 10f.pow(gainDb / 40f)
        val w0 = 2f * PI.toFloat() * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2f * q)
        val sqrtA = sqrt(a)

        val b0 = a * ((a + 1f) + (a - 1f) * cosW0 + 2f * sqrtA * alpha)
        val b1 = -2f * a * ((a - 1f) + (a + 1f) * cosW0)
        val b2 = a * ((a + 1f) + (a - 1f) * cosW0 - 2f * sqrtA * alpha)
        val a0 = (a + 1f) - (a - 1f) * cosW0 + 2f * sqrtA * alpha
        val a1 = 2f * ((a - 1f) - (a + 1f) * cosW0)
        val a2 = (a + 1f) - (a - 1f) * cosW0 - 2f * sqrtA * alpha

        return BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun peaking(freq: Float, gainDb: Float, q: Float, sampleRate: Int): BiquadCoefficients {
        val a = 10f.pow(gainDb / 40f)
        val w0 = 2f * PI.toFloat() * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2f * q)

        val b0 = 1f + alpha * a
        val b1 = -2f * cosW0
        val b2 = 1f - alpha * a
        val a0 = 1f + alpha / a
        val a1 = -2f * cosW0
        val a2 = 1f - alpha / a

        return BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun lowPass(freq: Float, q: Float, sampleRate: Int): BiquadCoefficients {
        val w0 = 2f * PI.toFloat() * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2f * q)

        val b0 = (1f - cosW0) / 2f
        val b1 = 1f - cosW0
        val b2 = (1f - cosW0) / 2f
        val a0 = 1f + alpha
        val a1 = -2f * cosW0
        val a2 = 1f - alpha

        return BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }
}
