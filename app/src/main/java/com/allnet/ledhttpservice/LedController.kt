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
    private const val DELAY_WAKE_LONG = 4000L   // Delay if display was off
    private const val DELAY_WAKE_SHORT = 250L   // Delay if display was already on
    private const val DELAY_POST_ON = 500L      // Delay after internal 'on' command
    private const val DELAY_RETRY = 250L        // Delay between retries if display was off

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

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    /**
     * Initializes the controller with a context to enable disk persistence.
     */
    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getContext(): Context = checkNotNull(appContext) { "LedController must be initialized." }
    private fun getPrefs(): SharedPreferences = checkNotNull(prefs) { "LedController must be initialized." }

    fun getAvailableModes(): List<String> = modeMap.keys.toList()

    fun getLastMode(): String {
        return getPrefs().getString(KEY_LAST_MODE, "unknown") ?: "unknown"
    }

    /**
     * Sets the LED mode, applying robust timing and retries if the display was previously off.
     */
    fun setLed(modeName: String): Boolean {
        val normalizedMode = modeName.lowercase()
        val code = modeMap[normalizedMode] ?: return false
        val currentLastMode = getLastMode()
        
        // 1. Detect display state BEFORE waking
        val wasDisplayOff = !WakeHelper.isInteractive(getContext())
        Log.d(TAG, "Request: $normalizedMode (Display off: $wasDisplayOff, Previous: $currentLastMode)")

        // 2. Always wake the device
        WakeHelper.wakeDevice()

        // 3. Apply appropriate delay after wake
        sleep(if (wasDisplayOff) DELAY_WAKE_LONG else DELAY_WAKE_SHORT)

        // 4. Command Execution Logic
        if ((currentLastMode == "off" || currentLastMode == "unknown") && normalizedMode != "on" && normalizedMode != "off") {
            Log.d(TAG, "State transition detected. Sending internal ON command (0x03).")
            val onResult = ShellExecutor.executeRootCommand(getCommand("0x03"))
            if (!onResult.success) {
                Log.e(TAG, "Initial ON failed. Stderr: ${onResult.stderr}")
                return false
            }
            sleep(DELAY_POST_ON)
        }

        // 5. Execute the target mode command
        Log.d(TAG, "Executing target mode: $normalizedMode ($code)")
        val success = executeWithRetryIfNeed(code, wasDisplayOff)

        // 6. Persist and return
        if (success) {
            Log.d(TAG, "Successfully applied $normalizedMode.")
            getPrefs().edit().putString(KEY_LAST_MODE, normalizedMode).apply()
        }
        else {
            Log.e(TAG, "Failed to apply $normalizedMode.")
        }
        return success
    }

    /**
     * Executes the root command, with an optional retry if the display was previously off.
     */
    private fun executeWithRetryIfNeed(code: String, shouldRetry: Boolean): Boolean {
        val firstAttempt = ShellExecutor.executeRootCommand(getCommand(code))
        if (!firstAttempt.success) return false
        
        if (shouldRetry) {
            sleep(DELAY_RETRY)
            val secondAttempt = ShellExecutor.executeRootCommand(getCommand(code))
            return secondAttempt.success
        }
        
        return true
    }

    private fun sleep(millis: Long) {
        try { Thread.sleep(millis) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
    }

    private fun getCommand(code: String): String {
        return "sh -c \"echo 'w $code' > /sys/devices/platform/led_con_h/zigbee_reset\""
    }
}
