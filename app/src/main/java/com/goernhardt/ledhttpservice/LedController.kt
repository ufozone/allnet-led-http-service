package com.goernhardt.ledhttpservice

/**
 * Singleton controller for handling LED hardware commands.
 * Maps mode names to hardware hex codes and handles device-specific power behavior.
 */
object LedController {
    // LinkedHashMap to preserve the logical order of modes
    private val modeMap = linkedMapOf(
        // Brightness
        "dim_up" to "0x00", "dim_down" to "0x01",
        // Power
        "off" to "0x02", "on" to "0x03",
        // Base Colors
        "red" to "0x04", "green" to "0x05", "blue" to "0x06", "white" to "0x07",
        // Extended Colors
        "orange_red" to "0x08", "orange" to "0x0C", "yellow_orange" to "0x10", "yellow" to "0x14",
        "turquoise" to "0x09", "light_blue" to "0x0D", "cyan" to "0x11", "medium_blue" to "0x0A",
        "blue_green" to "0x15", "violet" to "0x0E", "purple" to "0x12", "pink" to "0x16",
        // Dynamic Effects
        "flash" to "0x0B", "strobe" to "0x0F", "fade" to "0x13", "smooth" to "0x17"
    )

    private var lastMode: String = "unknown"

    /**
     * Returns a list of all supported mode names in logical order.
     */
    fun getAvailableModes(): List<String> = modeMap.keys.toList()

    /**
     * Returns the last successfully set mode.
     */
    fun getLastMode(): String = lastMode

    /**
     * Sets the LED to the specified mode, handling power transitions and device wakeup.
     * @param modeName The name of the mode to set.
     * @return True if the command(s) were successfully executed.
     */
    fun setLed(modeName: String): Boolean {
        val normalizedMode = modeName.lowercase()
        val code = modeMap[normalizedMode] ?: return false
        
        val success = when {
            // If requested mode is NOT 'off' and last known state was 'off', we must 'on' first
            normalizedMode != "off" && lastMode == "off" -> {
                WakeHelper.wakeDevice()
                val onSuccess = ShellExecutor.executeRootCommand(getCommand("0x03")) // Send 'on' (0x03)
                try { Thread.sleep(200) } catch (e: InterruptedException) { /* ignore */ }
                onSuccess && ShellExecutor.executeRootCommand(getCommand(code))
            }
            // If requested mode is 'off'
            normalizedMode == "off" -> {
                WakeHelper.wakeDevice()
                ShellExecutor.executeRootCommand(getCommand("0x02")) // Send 'off' (0x02)
            }
            // Normal operation: just wake and send command
            else -> {
                WakeHelper.wakeDevice()
                ShellExecutor.executeRootCommand(getCommand(code))
            }
        }

        if (success) {
            lastMode = normalizedMode
        }
        return success
    }

    private fun getCommand(code: String): String {
        return "sh -c \"echo 'w $code' > /sys/devices/platform/led_con_h/zigbee_reset\""
    }
}
