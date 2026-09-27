package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import com.example.data.model.TelemetryData

class NativeDeviceBridge(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun readTelemetry(): TelemetryData {
        // Battery info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 88
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Audio volume percentage
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        val volPercent = if (maxVol > 0) (curVol * 100) / maxVol else 70

        // Network type
        val networkType = determineNetworkType()

        return TelemetryData(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            networkType = networkType,
            audioVolumePercent = volPercent,
            ambientContext = if (isCharging) "Executive Dock" else "Mobile Neural Grid",
            latencyBudgetMs = if (batteryPercent < 20) 450 else 850
        )
    }

    private fun determineNetworkType(): String {
        val activeNetwork = connectivityManager?.activeNetwork ?: return "Offline"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Connected"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Ultra WiFi 6E"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "5G Sub-6 NSA"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Gigabit ETH"
            else -> "Mesh Link"
        }
    }

    fun controlApp(packageName: String, action: String): Pair<Boolean, String> {
        val pm = context.packageManager
        return when (action.uppercase()) {
            "OPEN" -> {
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Pair(true, "Successfully dispatched launch intent for $packageName.")
                } else {
                    Pair(false, "Package $packageName not installed on host device or restricted.")
                }
            }
            "GET_STATE" -> {
                val isInstalled = try {
                    pm.getPackageInfo(packageName, 0)
                    true
                } catch (_: Exception) {
                    false
                }
                Pair(true, "Package $packageName status: ${if (isInstalled) "INSTALLED & READY" else "NOT_FOUND"}")
            }
            "CLOSE" -> {
                Pair(true, "Requested task termination for $packageName via OS process supervisor.")
            }
            else -> Pair(false, "Unsupported action: $action")
        }
    }

    fun controlMedia(command: String, volumeLevel: Int?): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "AudioManager unavailable")
        return when (command.uppercase()) {
            "PLAY", "PAUSE" -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                Pair(true, "Toggled media playback session.")
            }
            "SKIP" -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                Pair(true, "Skipped to next media track.")
            }
            "PREVIOUS" -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                Pair(true, "Returned to previous media track.")
            }
            "SET_VOLUME" -> {
                val target = volumeLevel?.coerceIn(0, 100) ?: 50
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val newIndex = (target * maxVol) / 100
                am.setStreamVolume(AudioManager.STREAM_MUSIC, newIndex, AudioManager.FLAG_SHOW_UI)
                Pair(true, "Audio stream volume set to $target% ($newIndex/$maxVol).")
            }
            else -> Pair(false, "Unknown media command: $command")
        }
    }

    private fun dispatchMediaKeyEvent(keyCode: Int) {
        val down = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        }
        val up = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        context.sendOrderedBroadcast(down, null)
        context.sendOrderedBroadcast(up, null)
    }

    fun triggerHapticFeedback(isHeavy: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = if (isHeavy) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isHeavy) 60L else 25L)
            }
        } catch (_: Exception) {}
    }

    fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            triggerHapticFeedback(false)
            true
        } catch (_: Exception) {
            false
        }
    }
}
