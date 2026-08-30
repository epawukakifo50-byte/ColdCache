package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Provides discrete, satisfying tactile feedback optimized for neurodivergent sensory UX
 * (ADHD/ASD/PDA friendly — subtle, velvety, satisfying, never startling or abrasive).
 */
object AppHaptics {

    /**
     * Ultra-light, crisp micro-tick (for subtask checkboxes, slider dragging, small switches).
     */
    fun tick(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 80))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    /**
     * Crisp, satisfying mechanical button click (for cards, major buttons, modal opens/closes).
     */
    fun click(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, 150))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Exception) {}
    }

    /**
     * Magnetic "snap" tactile feel (for moving tasks between RAM, CRYO, and Latent Space).
     */
    fun snap(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.5f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 20)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 12, 25, 18)
                val amplitudes = intArrayOf(0, 120, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 12, 25, 18), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Deep "drop & release" feel (for dropping tasks to DevNull / deleting).
     */
    fun dumpRelease(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.9f, 30)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 22, 30, 14)
                val amplitudes = intArrayOf(0, 190, 0, 110)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 22, 30, 14), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Smooth toggle transition feel (for sensory theme change, shape switch, settings toggles).
     */
    fun toggle(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 0.5f)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(14, 130))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(14)
            }
        } catch (_: Exception) {}
    }

    /**
     * Multi-pulse subtle celebratory pattern (task completed / compile finished).
     */
    fun success(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, 30)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.9f, 40)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 40, 25, 40, 30)
                val amplitudes = intArrayOf(0, 140, 0, 190, 0, 240)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 20, 40, 25, 40, 30), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Decaying tactile pulses representing heat dissipation (Thermal Dissipation effect).
     */
    fun thermalCooling(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 35, 40, 25, 40, 18, 50, 12)
                val amplitudes = intArrayOf(0, 220, 0, 160, 0, 100, 0, 50)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 40, 20, 40, 10), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Subtle low double-knock for limits/overflow (gentle reminder, never jarring).
     */
    fun overloadWarning(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val composition = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 70)
                    .compose()
                vibrator.vibrate(composition)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 25, 60, 20)
                val amplitudes = intArrayOf(0, 160, 0, 120)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 25, 60, 20), -1)
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
