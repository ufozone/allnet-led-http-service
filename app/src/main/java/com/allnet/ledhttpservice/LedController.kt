package com.allnet.ledhttpservice

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Singleton controller for handling LED hardware commands.
 * Maps mode names to hardware hex codes and handles device-specific power behavior with disk persistence.
 */
object LedController {
    private const val TAG = "LedController"
    private const val PREFS_NAME = "LedControllerPrefs"
    private const val KEY_LAST_MODE = "last_mode"

    // Timing Constants
    private const val DELAY_WAKE = 200L
    private const val DELAY_POST_ON = 500L

    // LinkedHashMap to preserve the logical order of modes
    private val modeMap = linkedMapOf(
        // Brightness
        "dim_up" to "0x00",
        "dim_down" to "0x01",
        // Power
        "off" to "0x02",
        "on" to "0x03",
        // Base Colors
        "red" to "0x04",
        "green" to "0x05",
        "blue" to "0x06",
        "white" to "0x07",
        // Extended Colors
        "orange_red" to "0x08",
        "orange" to "0x0C",
        "yellow_orange" to "0x10",
        "yellow" to "0x14",
        "turquoise" to "0x09",
        "light_blue" to "0x0D",
        "cyan" to "0x11",
        "medium_blue" to "0x0A",
        "blue_green" to "0x15",
        "violet" to "0x0E",
        "purple" to "0x12",
        "pink" to "0x16",
        // Dynamic Effects
        "flash" to "0x0B",
        "strobe" to "0x0F",
        "fade" to "0x13",
        "smooth" to "0x17"
    )

    private var prefs: SharedPreferences? = null

    /**
     * Initializes the controller with a context to enable disk persistence.
     * Must be called before any other method.
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Internal helper to ensure preferences are initialized.
     */
    private fun getPrefs(): SharedPreferences {
        return checkNotNull(prefs) { "LedController must be initialized by calling init(context) before use." }
    }

    /**
     * Returns a list of all supported mode names in logical order.
     */
    fun getAvailableModes(): List<String> = modeMap.keys.toList()

    /**
     * Returns the last successfully set mode from memory or disk.
     */
    fun getLastMode(): String {
        return getPrefs().getString(KEY_LAST_MODE, "unknown") ?: "unknown"
    }

    /**
     * Sets the LED to the specified mode, handling power transitions and device wakeup.
     * @param modeName The name of the mode to set.
     * @return True if the command(s) were successfully executed.
     */
    fun setLed(modeName: String): Boolean {
        val normalizedMode = modeName.lowercase()
        val code = modeMap[normalizedMode] ?: return false
        val currentLastMode = getLastMode()

        Log.d(TAG, "Request: $normalizedMode (Previous: $currentLastMode)")

        // 1. Always wake the device and wait for stabilization
        WakeHelper.wakeDevice()
        sleep(DELAY_WAKE)

        // 2. Handle transition from OFF/UNKNOWN to a specific color or effect
        if ((currentLastMode == "off" || currentLastMode == "unknown") && 
            (normalizedMode != "off" && normalizedMode != "on")) {
            
            Log.d(TAG, "State transition detected. Sending internal ON command (0x03).")
            val onResult = ShellExecutor.executeRootCommand(getCommand("0x03"))
            if (!onResult) {
                Log.e(TAG, "Internal ON command failed. Aborting.")
                return false
            }
            sleep(DELAY_POST_ON)
        }

        // 3. Execute the target mode command (one-shot)
        Log.d(TAG, "Executing target mode: $normalizedMode ($code)")
        val success = ShellExecutor.executeRootCommand(getCommand(code))

        // 4. Persist and return
        if (success) {
            Log.d(TAG, "Successfully applied $normalizedMode.")
            getPrefs().edit().putString(KEY_LAST_MODE, normalizedMode).apply()
        } else {
            Log.e(TAG, "Failed to apply $normalizedMode.")
        }

        return success
    }

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Sleep interrupted: ${e.message}")
            Thread.currentThread().interrupt()
        }
    }

    private fun getCommand(code: String): String {
        return "sh -c \"echo 'w $code' > /sys/devices/platform/led_con_h/zigbee_reset\""
    }
}
