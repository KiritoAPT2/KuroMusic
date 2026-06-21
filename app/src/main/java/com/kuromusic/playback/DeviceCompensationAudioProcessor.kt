package com.kuromusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class DeviceCompensationAudioProcessor(
    private val deviceStateHolder: DeviceAudioStateHolder,
) : BaseAudioProcessor() {

    private var currentEncoding = C.ENCODING_PCM_FLOAT
    private var gainComp = 1.0f
    private var bassComp = 1.0f
    private var midComp = 1.0f
    private var workBuffer = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        currentEncoding = inputAudioFormat.encoding
        updateProfile()
        return inputAudioFormat
    }

    fun updateProfile() {
        when (deviceStateHolder.device) {
            OutputDevice.HEADPHONES -> {
                gainComp = 1.0f
                bassComp = 1.0f
                midComp = 1.0f
            }
            OutputDevice.SPEAKER -> {
                gainComp = 1.0f
                bassComp = 1.05f
                midComp = 0.98f
            }
            OutputDevice.BLUETOOTH -> {
                gainComp = 1.0f
                bassComp = 1.05f
                midComp = 1.0f
            }
            else -> {
                gainComp = 1.0f
                bassComp = 1.0f
                midComp = 1.0f
            }
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())

        if (gainComp == 1.0f && bassComp == 1.0f && midComp == 1.0f) {
            out.put(inputBuffer)
            inputBuffer.position(inputBuffer.limit())
            out.flip()
            return
        }

        when (currentEncoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, out)
            C.ENCODING_PCM_16BIT -> processShort(inputBuffer, out)
            else -> out.put(inputBuffer)
        }

        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    private fun processFloat(inputBuffer: ByteBuffer, out: ByteBuffer) {
        val floatBuf = inputBuffer.asFloatBuffer()
        val size = floatBuf.remaining()
        if (workBuffer.size < size) workBuffer = FloatArray(size)
        floatBuf.get(workBuffer, 0, size)

        val g = gainComp
        val bBoost = bassComp - 1.0f
        val m = midComp
        var i = 0
        while (i < size) {
            val left = workBuffer[i] * g
            val right = workBuffer[i + 1] * g
            val bass = (left + right) * 0.5f * bBoost
            out.putFloat(left * m + bass)
            out.putFloat(right * m + bass)
            i += 2
        }
    }

    private fun processShort(inputBuffer: ByteBuffer, out: ByteBuffer) {
        val shortBuf = inputBuffer.asShortBuffer()
        val size = shortBuf.remaining()
        if (workBuffer.size < size) workBuffer = FloatArray(size)
        for (i in 0 until size) {
            workBuffer[i] = shortBuf[i] / 32768f
        }

        val g = gainComp
        val bBoost = bassComp - 1.0f
        val m = midComp
        var i = 0
        while (i < size) {
            val left = workBuffer[i] * g
            val right = workBuffer[i + 1] * g
            val bass = (left + right) * 0.5f * bBoost
            val outL = (left * m + bass).coerceIn(-1f, 1f)
            val outR = (right * m + bass).coerceIn(-1f, 1f)
            out.putShort((outL * 32768f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            out.putShort((outR * 32768f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            i += 2
        }
    }
}
