package com.allnet.ledhttpservice

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton controller for handling LED hardware commands.
 * Maps mode names to hardware hex codes and handles device-specific power behavior with disk persistence.
 */
object LedController {
    private const val PREFS_NAME = "LedControllerPrefs"
    private const val KEY_LAST_MODE = "last_mode"

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

        // 1. Always call WakeHelper.wakeDevice()
        WakeHelper.wakeDevice()

        // 2. Always wait 200ms
        try { Thread.sleep(200) } catch (e: InterruptedException) { /* ignore */ }

        // 3. Conditional 'on' command for transition from off/unknown to a specific mode
        if ((currentLastMode == "off" || currentLastMode == "unknown") && 
            (normalizedMode != "off" && normalizedMode != "on")) {
            
            val onResult = ShellExecutor.executeRootCommand(getCommand("0x03"))
            if (!onResult) return false
            
            try { Thread.sleep(200) } catch (e: InterruptedException) { /* ignore */ }
        }

        // 4. Always execute the root command for the target code as the final step
        val success = ShellExecutor.executeRootCommand(getCommand(code))

        // 5. If successful, persist normalizedMode to lastMode
        if (success) {
            getPrefs().edit().putString(KEY_LAST_MODE, normalizedMode).apply()
        }

        // 6. Return the final success status
        return success
    }

    private fun getCommand(code: String): String {
        return "sh -c \"echo 'w $code' > /sys/devices/platform/led_con_h/zigbee_reset\""
    }
}
