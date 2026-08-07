package com.kuromusic.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class VisualizerEngine {

    private var visualizer: Visualizer? = null
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main)
    private val _waveformData = MutableStateFlow(FloatArray(0))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _fftData = MutableStateFlow(FloatArray(0))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _normalizedAmplitude = MutableStateFlow(0f)
    val normalizedAmplitude: StateFlow<Float> = _normalizedAmplitude.asStateFlow()

    private var captureRate: Int = Visualizer.getMaxCaptureRate()
    private val waveSize: Int = 64

    fun attach(audioSessionId: Int) {
        release()
        try {
            val vis = Visualizer(audioSessionId)
            vis.captureSize = Visualizer.getCaptureSizeRange()[1]

            vis.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { data ->
                            val magnitudes = FloatArray(data.size) { i ->
                                (data[i].toInt() and 0xFF).toFloat() / 255f
                            }
                            _waveformData.value = magnitudes
                            _amplitude.value = magnitudes.average().toFloat()
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { data ->
                            val magnitudes = mutableListOf<Float>()
                            var i = 0
                            while (i < data.size - 1) {
                                val real = data[i].toDouble()
                                val imag = data[i + 1].toDouble()
                                val magnitude = sqrt(real * real + imag * imag).toFloat()
                                magnitudes.add(magnitude)
                                i += 2
                            }

                            val normalized = normalizeFft(magnitudes, waveSize)
                            _fftData.value = normalized
                            val peak = normalized.maxOrNull() ?: 0f
                            _normalizedAmplitude.value = peak.coerceIn(0f, 1f)
                        }
                    }
                },
                captureRate,
                true,
                true
            )

            vis.enabled = true
            visualizer = vis
        } catch (e: Exception) {
            release()
        }
    }

    private fun normalizeFft(magnitudes: List<Float>, targetSize: Int): FloatArray {
        if (magnitudes.isEmpty()) return FloatArray(targetSize)
        val result = FloatArray(targetSize)
        val step = magnitudes.size.toFloat() / targetSize

        for (i in 0 until targetSize) {
            val start = (i * step).toInt()
            val end = ((i + 1) * step).toInt().coerceAtMost(magnitudes.size)
            val avg = if (end > start) {
                magnitudes.subList(start, end).average().toFloat()
            } else 0f
            result[i] = (avg / 100f).coerceIn(0f, 1f)
        }

        return result
    }

    fun release() {
        try {
            visualizer?.setDataCaptureListener(null, 0, false, false)
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        scopeJob.cancel()
        _waveformData.value = FloatArray(0)
        _fftData.value = FloatArray(0)
        _amplitude.value = 0f
        _normalizedAmplitude.value = 0f
    }

    fun updateCaptureRate(sampleMs: Int) {
        captureRate = sampleMs
        visualizer?.let { vis ->
            try {
                vis.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) {
                            waveform?.let { data ->
                                val magnitudes = FloatArray(data.size) { i ->
                                    (data[i].toInt() and 0xFF).toFloat() / 255f
                                }
                                _waveformData.value = magnitudes
                                _amplitude.value = magnitudes.average().toFloat()
                            }
                        }

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                            fft?.let { data ->
                                val magnitudes = mutableListOf<Float>()
                                var i = 0
                                while (i < data.size - 1) {
                                    val real = data[i].toDouble()
                                    val imag = data[i + 1].toDouble()
                                    val magnitude = sqrt(real * real + imag * imag).toFloat()
                                    magnitudes.add(magnitude)
                                    i += 2
                                }
                                val normalized = normalizeFft(magnitudes, waveSize)
                                _fftData.value = normalized
                                val peak = normalized.maxOrNull() ?: 0f
                                _normalizedAmplitude.value = peak.coerceIn(0f, 1f)
                            }
                        }
                    },
                    captureRate,
                    true,
                    true
                )
            } catch (_: Exception) {}
        }
    }
}
