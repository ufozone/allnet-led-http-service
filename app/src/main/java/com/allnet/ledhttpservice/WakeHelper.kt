package com.allnet.ledhttpservice

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Helper to manage device wakeup and screen state detection.
 */
object WakeHelper {
    private const val TAG = "WakeHelper"

    /**
     * Checks if the device is currently in an interactive state (screen on).
     */
    fun isInteractive(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive ?: true
    }

    /**
     * Sends a root command to wake the device display.
     */
    fun wakeDevice() {
        Log.d(TAG, "Attempting to wake device via KEYCODE_WAKEUP")
        val result = ShellExecutor.executeRootCommand("input keyevent KEYCODE_WAKEUP")
        if (!result.success) {
            Log.e(TAG, "Failed to wake device. Stderr: ${result.stderr}")
        }
    }
}
