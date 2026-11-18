package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural PCM audio synthesizer providing tactile 3D puzzle sound effects and
 * a gentle ambient pentatonic background music loop with independent Music and SFX controls.
 */
class SoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    private var musicEnabled: Boolean = true

    @Volatile
    private var sfxEnabled: Boolean = true

    fun setPreferences(musicOn: Boolean, sfxOn: Boolean) {
        sfxEnabled = sfxOn
        if (musicEnabled != musicOn) {
            musicEnabled = musicOn
            if (musicOn) startMusic() else stopMusic()
        } else if (musicOn && musicJob?.isActive != true) {
            startMusic()
        }
    }

    fun startMusic() {
        if (!musicEnabled || musicJob?.isActive == true) return
        musicJob = scope.launch {
            // Soft pentatonic ambient sequence (C4, E4, G4, B4, D5, G4, E4, A4)
            val notes = doubleArrayOf(261.63, 329.63, 392.00, 493.88, 587.33, 392.00, 329.63, 440.00)
            var idx = 0
            while (isActive && musicEnabled) {
                playTone(
                    frequencies = doubleArrayOf(notes[idx % notes.size], notes[idx % notes.size] * 0.5),
                    durationMs = 520,
                    volume = 0.08,
                    decay = 3.2
                )
                idx++
                delay(640L)
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun playBlockPlace() {
        if (!sfxEnabled) return
        scope.launch {
            playTone(doubleArrayOf(310.0, 465.0), durationMs = 70, volume = 0.28, decay = 22.0)
        }
    }

    fun playLineClear(linesCount: Int) {
        if (!sfxEnabled) return
        scope.launch {
            val base = 440.0 + (linesCount.coerceAtMost(5) * 55.0)
            val chord = listOf(base, base * 1.25, base * 1.5, base * 2.0)
            chord.forEachIndexed { i, freq ->
                playTone(doubleArrayOf(freq), durationMs = 110, volume = 0.26, decay = 9.0)
                if (i < chord.lastIndex) delay(48L)
            }
        }
    }

    fun playCoinReward() {
        if (!sfxEnabled) return
        scope.launch {
            playTone(doubleArrayOf(987.77), durationMs = 75, volume = 0.24, decay = 14.0)
            delay(55L)
            playTone(doubleArrayOf(1318.51), durationMs = 160, volume = 0.26, decay = 8.0)
        }
    }

    fun playLevelComplete() {
        if (!sfxEnabled) return
        scope.launch {
            val fanfare = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            fanfare.forEachIndexed { i, freq ->
                playTone(doubleArrayOf(freq, freq * 1.5), durationMs = if (i == 3) 340 else 120, volume = 0.28, decay = 6.0)
                if (i < fanfare.lastIndex) delay(90L)
            }
        }
    }

    fun playGameOver() {
        if (!sfxEnabled) return
        scope.launch {
            val descent = doubleArrayOf(392.0, 349.23, 311.13, 261.63)
            descent.forEachIndexed { i, freq ->
                playTone(doubleArrayOf(freq), durationMs = if (i == 3) 360 else 140, volume = 0.24, decay = 5.5)
                if (i < descent.lastIndex) delay(110L)
            }
        }
    }

    private fun playTone(
        frequencies: DoubleArray,
        durationMs: Int,
        volume: Double,
        decay: Double
    ) {
        val sampleRate = 22050
        val numSamples = (sampleRate * durationMs) / 1000
        if (numSamples <= 0) return

        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val envelope = exp(-decay * t) * (if (i < 120) i / 120.0 else 1.0)
            var mixed = 0.0
            for (f in frequencies) {
                mixed += sin(2.0 * PI * f * t)
            }
            val normalized = (mixed / frequencies.size) * envelope * volume
            samples[i] = (normalized * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuf, samples.size * 2)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferSize)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 15L)
        } catch (e: Exception) {
            Log.d("SoundEngine", "Audio playback skipped: ${e.message}")
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        stopMusic()
        scope.cancel()
    }
}
