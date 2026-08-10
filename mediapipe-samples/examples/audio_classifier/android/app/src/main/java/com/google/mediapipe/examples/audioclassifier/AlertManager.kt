package com.google.mediapipe.examples.audioclassifier

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.ColorRes

enum class AlertTier(
    @ColorRes val colorRes: Int,
    val vibrationPattern: LongArray,
    val priority: Int
) {
    CRITICAL(R.color.alert_critical, longArrayOf(0, 500, 200, 500), 3),
    EMERGENCY(R.color.alert_emergency, longArrayOf(0, 400, 100, 400), 3),
    FUSION(R.color.alert_fusion, longArrayOf(0, 1000), 4),
    HIGH(R.color.alert_high, longArrayOf(0, 200), 2),
    PERSONAL(R.color.alert_personal, longArrayOf(0, 300), 2),
    MEDIUM(R.color.alert_medium, longArrayOf(0, 150, 50, 150), 1),
    LOW(R.color.ecosense_bg, longArrayOf(0), 0)
}

data class AlertInfo(
    val label: String,
    val tier: AlertTier,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

class AlertManager(private val context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var lastFireTime: Long = 0
    private var lastScreamTime: Long = 0

    fun getTierForLabel(label: String): AlertTier {
        val currentTime = System.currentTimeMillis()
        val normalizedLabel = label.lowercase()
        
        if (normalizedLabel.contains("fire") || normalizedLabel.contains("smoke")) {
            lastFireTime = currentTime
        }
        if (normalizedLabel.contains("scream") || normalizedLabel.contains("baby")) {
            lastScreamTime = currentTime
        }

        // Context Fusion check (both detected within 10 seconds)
        if (Math.abs(lastFireTime - lastScreamTime) < 10000 && lastFireTime != 0L && lastScreamTime != 0L) {
            return AlertTier.FUSION
        }

        return when {
            normalizedLabel.contains("fire") || normalizedLabel.contains("smoke") -> AlertTier.CRITICAL
            normalizedLabel.contains("siren") || normalizedLabel.contains("emergency") -> AlertTier.EMERGENCY
            normalizedLabel.contains("doorbell") || normalizedLabel.contains("knock") -> AlertTier.HIGH
            normalizedLabel.contains("scream") || normalizedLabel.contains("baby") -> AlertTier.MEDIUM
            else -> AlertTier.LOW
        }
    }

    fun triggerVibration(tier: AlertTier) {
        if (tier == AlertTier.LOW) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(tier.vibrationPattern, -1))
        } else {
            vibrator.vibrate(tier.vibrationPattern, -1)
        }
    }
}
