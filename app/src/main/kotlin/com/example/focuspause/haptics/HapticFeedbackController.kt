/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

interface HapticFeedbackProvider {
    fun vibrateClick()
    fun vibrateFocusCompleted()
    fun vibrateBreakCompleted()
    fun vibrateAction()
}

class HapticFeedbackController(context: Context) : HapticFeedbackProvider {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun vibrateClick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun vibrateFocusCompleted() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val timings = longArrayOf(0, 200, 120, 300)
        val amplitudes = intArrayOf(0, 220, 0, 255)
        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    override fun vibrateBreakCompleted() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val timings = longArrayOf(0, 120, 80, 120, 80, 220)
        val amplitudes = intArrayOf(0, 180, 0, 180, 0, 240)
        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    override fun vibrateAction() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val timings = longArrayOf(0, 80, 60, 80)
        val amplitudes = intArrayOf(0, 180, 0, 180)
        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}
