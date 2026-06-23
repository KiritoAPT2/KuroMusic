package com.kuromusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.pow

class SoundProfileAudioProcessor(
    private val stateHolder: SoundProfileStateHolder,
    private val deviceStateHolder: DeviceAudioStateHolder,
) : BaseAudioProcessor() {

    private var currentEncoding = C.ENCODING_PCM_FLOAT
    private var channelCount = 2
    private var sampleRate = 48000
    private var workBuffer = FloatArray(0)

    private var cachedProfile: SoundProfile? = null
    private var cachedDevice: OutputDevice? = null

    private val filterPool = Array(4) { BiquadFilter() }
    private var activeFilters = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        currentEncoding = inputAudioFormat.encoding
        channelCount = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        buildFilters(stateHolder.profile)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val profile = stateHolder.profile
        if (profile != cachedProfile) {
            cachedDevice = deviceStateHolder.device
            buildFilters(profile, cachedDevice)
            cachedProfile = profile
        } else {
            val device = deviceStateHolder.device
            if (device != cachedDevice) {
                cachedDevice = device
                buildFilters(profile, cachedDevice)
            }
        }

        val out = replaceOutputBuffer(inputBuffer.remaining())

        when (currentEncoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, out, profile)
            C.ENCODING_PCM_16BIT -> processShort(inputBuffer, out, profile)
            else -> out.put(inputBuffer)
        }

        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    private fun buildFilters(profile: SoundProfile, device: OutputDevice? = null) {
        for (i in 0 until activeFilters) {
            filterPool[i].reset()
        }
        val sections = getSectionsForProfile(profile, device ?: deviceStateHolder.device)
        activeFilters = sections.size
        for (i in sections.indices) {
            val filter = filterPool[i]
            filter.init(channelCount)
            filter.setCoefficients(sections[i])
        }
    }

    private fun getSectionsForProfile(profile: SoundProfile, device: OutputDevice): List<BiquadCoefficients> {
        return when (profile) {
            SoundProfile.CLEAN -> emptyList()

            SoundProfile.WARM -> listOf(
                BiquadCoefficientsFactory.lowShelf(120f, 3.0f, 0.7f, sampleRate),
                BiquadCoefficientsFactory.highShelf(7000f, -1.2f, 0.5f, sampleRate),
            )

            SoundProfile.BASS -> {
                val boost = if (device == OutputDevice.SPEAKER) 1.5f else 2.0f
                listOf(
                    BiquadCoefficientsFactory.lowShelf(100f, boost, 0.5f, sampleRate),
                    BiquadCoefficientsFactory.highShelf(8000f, 0f, 0.5f, sampleRate),
                )
            }

            SoundProfile.LOFI -> listOf(
                BiquadCoefficientsFactory.lowPass(10000f, 0.707f, sampleRate),
                BiquadCoefficientsFactory.lowShelf(100f, 0.5f, 0.5f, sampleRate),
            )

            SoundProfile.STUDIO -> listOf(
                BiquadCoefficientsFactory.peaking(3500f, 2.5f, 0.7f, sampleRate),
                BiquadCoefficientsFactory.lowShelf(80f, 2.0f, 0.5f, sampleRate),
            )
        }
    }

    private fun processFloat(inputBuffer: ByteBuffer, out: ByteBuffer, profile: SoundProfile) {
        val floatBuf = inputBuffer.asFloatBuffer()
        val size = floatBuf.remaining()
        if (workBuffer.size < size) workBuffer = FloatArray(size)
        floatBuf.get(workBuffer, 0, size)

        for (i in 0 until activeFilters) {
            filterPool[i].process(workBuffer, workBuffer, channelCount)
        }

        val finalGainDb = stateHolder.getFinalGainDb(profile)
        val gainComp = (10f.pow(finalGainDb / 20f)).coerceAtMost(4f).takeIf(Float::isFinite) ?: 1f
        var i = 0
        while (i < size) {
            workBuffer[i] *= gainComp
            i++
        }

        LoudnessTap.feed(profile, workBuffer, sampleRate)

        i = 0
        while (i < size) {
            val sample = workBuffer[i]
            out.putFloat(sample.coerceIn(-1f, 1f).takeIf(Float::isFinite) ?: 0f)
            i++
        }
    }

    private fun processShort(inputBuffer: ByteBuffer, out: ByteBuffer, profile: SoundProfile) {
        val shortBuf = inputBuffer.asShortBuffer()
        val size = shortBuf.remaining()
        if (workBuffer.size < size) workBuffer = FloatArray(size)
        for (i in 0 until size) {
            workBuffer[i] = shortBuf[i] / 32768f
        }

        for (i in 0 until activeFilters) {
            filterPool[i].process(workBuffer, workBuffer, channelCount)
        }

        val finalGainDb = stateHolder.getFinalGainDb(profile)
        val gainComp = (10f.pow(finalGainDb / 20f)).coerceAtMost(4f).takeIf(Float::isFinite) ?: 1f
        var i = 0
        while (i < size) {
            workBuffer[i] *= gainComp
            i++
        }

        LoudnessTap.feed(profile, workBuffer, sampleRate)

        i = 0
        while (i < size) {
            val sample = workBuffer[i].takeIf(Float::isFinite) ?: 0f
            val denormalized = (sample * 32768f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            out.putShort(denormalized)
            i++
        }
    }
}
