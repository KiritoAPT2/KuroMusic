package com.kuromusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.pow

class GainAudioProcessor(
    private val stateHolder: AudioStateHolder
) : BaseAudioProcessor() {

    private var currentEncoding = C.ENCODING_PCM_FLOAT
    private var workBuffer = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        currentEncoding = inputAudioFormat.encoding
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val state = stateHolder.get()
        val totalGain = computeGain(state.gainDb, state.loudnessDb)

        val out = replaceOutputBuffer(inputBuffer.remaining())

        when (currentEncoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, out, totalGain)
            C.ENCODING_PCM_16BIT -> processShort(inputBuffer, out, totalGain)
            else -> out.put(inputBuffer)
        }

        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    private fun computeGain(gainDb: Float, loudnessDb: Float?): Float {
        val normalizationDb = if (loudnessDb != null) -loudnessDb else 0f
        val totalDb = (normalizationDb + gainDb).coerceIn(-6f, 6f)
        return 10f.pow(totalDb / 20f)
    }

    private fun processFloat(inputBuffer: ByteBuffer, out: ByteBuffer, gain: Float) {
        val floatBuf = inputBuffer.asFloatBuffer()
        val size = floatBuf.remaining()
        if (workBuffer.size < size) workBuffer = FloatArray(size)
        floatBuf.get(workBuffer, 0, size)
        for (i in 0 until size) {
            out.putFloat(workBuffer[i] * gain)
        }
    }

    private fun processShort(inputBuffer: ByteBuffer, out: ByteBuffer, gain: Float) {
        val shortBuf = inputBuffer.asShortBuffer()
        val size = shortBuf.remaining()
        if (workBuffer.size < size) workBuffer = FloatArray(size)
        for (i in 0 until size) {
            workBuffer[i] = shortBuf[i] / 32768f
        }
        for (i in 0 until size) {
            val processed = workBuffer[i] * gain
            val denormalized = (processed * 32768f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            out.putShort(denormalized)
        }
    }
}
