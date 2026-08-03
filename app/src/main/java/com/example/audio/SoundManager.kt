package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundManager(private val context: Context) {

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to init ToneGenerator: ${e.message}")
        }
    }

    fun playClick() {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            } catch (e: Exception) {
                Log.e("SoundManager", "Tone error: ${e.message}")
            }
        }
        triggerVibration(20)
    }

    fun playGuess() {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 80)
            } catch (e: Exception) {
                Log.e("SoundManager", "Tone error: ${e.message}")
            }
        }
        triggerVibration(40)
    }

    fun playWin() {
        if (soundEnabled) {
            try {
                // Play fanfare succession
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 150)
                Thread.sleep(160)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 250)
            } catch (e: Exception) {
                Log.e("SoundManager", "Tone error: ${e.message}")
            }
        }
        triggerVibration(120)
    }

    fun playLoss() {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 300)
            } catch (e: Exception) {
                Log.e("SoundManager", "Tone error: ${e.message}")
            }
        }
        triggerVibration(200)
    }

    fun playTimerTick() {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 30)
            } catch (e: Exception) {
                Log.e("SoundManager", "Tone error: ${e.message}")
            }
        }
    }

    private fun triggerVibration(milliseconds: Long) {
        if (!hapticsEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Vibration error: ${e.message}")
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("SoundManager", "Release error: ${e.message}")
        }
    }
}
