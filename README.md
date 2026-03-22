# LED HTTP Service for ALLNET Meeting Room Tablet (RK3399)

## Overview
LED HTTP Service is an Android application specifically designed for the **ALLNET Meeting Room RGB LED Tablet RK3399 Android 10**. It acts as a bridge between your network and the tablet's hardware LED bar, exposing a local HTTP API to control the light frame remotely.

The app is built to be a robust background utility, running as a persistent Foreground Service that automatically starts on system boot. It handles complex hardware behaviors, such as mandatory display activation and state-dependent timing, ensuring reliable LED control for integration with room booking systems and automation platforms.

## Features
- **Local HTTP API**: Lightweight NanoHTTPD server running on port 8080.
- **Hardware LED Control**: Direct manipulation of the RGB LED bar via root-level sysfs commands.
- **Adaptive Hardware Timing**: Detects if the display was off and applies robust delays and retries to ensure command registration.
- **Smart Display Management**: Automatically wakes the tablet display before every LED command sequence.
- **Persistent State**: Remembers the last set mode across service restarts and reboots.
- **Auto-Start on Boot**: Automatically initializes the service when the tablet finishes booting.
- **Foreground Service**: Maintains a persistent background process with a system notification.
- **Material 3 Dashboard**: Polished, tablet-optimized local UI to monitor status and manually test modes.

## Hardware / Target Device
This application is specifically developed and tested for:
- **Device**: ALLNET Meeting Room RGB LED Tablet RK3399 Android 10 (10" or 15" variants).
- **Chipset**: Rockchip RK3399.
- **OS**: Android 10 (API 29).
- **Requirement**: The device **must have root access** for the shell commands to reach the hardware platform.

## How It Works
### Hardware Command
The LED bar is controlled by writing specific hex codes to the kernel's platform driver:
`sh -c "echo 'w $CODE' > /sys/devices/platform/led_con_h/zigbee_reset"`

### Intelligent Wakeup & Timing
On this hardware, the LED controller is only reliable when the display is active. The app handles this automatically:
1.  **Detection**: Checks `PowerManager.isInteractive()` to see if the screen was off.
2.  **Wakeup**: Executes `input keyevent KEYCODE_WAKEUP`.
3.  **Adaptive Delay**: 
    - If display was **OFF**: Waits **4000ms** (4s) for hardware initialization.
    - If display was **ON**: Waits **250ms**.
4.  **Sequence Reliability**: 
    - If the display was previously off, the app automatically **retries** the target LED command after a **250ms** interval to ensure the hardware bus processes the change.

### Power-On Logic
If the device is currently in the `off` mode, it must be logically "powered on" before a color can be applied. When switching from `off` or `unknown` to a color/effect, the app sends the internal `on` (0x03) command and waits **500ms** before applying the final target mode.

## LED Modes
The following modes are supported by the public API, listed in their logical order:

**Power**
- `off`, `on`

**Base Colors**
- `red`, `green`, `blue`, `white`

**Extended Colors**
- `orange_red`, `orange`, `yellow_orange`, `yellow`, `turquoise`, `light_blue`, `cyan`, `medium_blue`, `blue_green`, `violet`, `purple`, `pink`

**Dynamic Effects**
- `flash`, `strobe`, `fade`, `smooth`

## HTTP API
The server listens on **port 8080**.

### Endpoints

#### 1. Home / Dashboard
`GET /`
Returns a tablet-friendly HTML control panel for manual interaction.

#### 2. Health Check
`GET /health`
- **Response**: `{"success": true}`

#### 3. Service Status
`GET /status`
- **Response**:
  ```json
  {
    "success": true,
    "service": "led-http-service",
    "lastMode": "red"
  }
  ```

#### 4. List Available Modes
`GET /modes`
- **Response**: `{"modes": ["off", "on", ... ]}`

#### 5. Wake Display
`GET /wake`
- **Response**: `{"success": true, "action": "wake"}`

#### 6. Set LED Mode
`GET /led?mode={name}` or `GET /led/{name}`
- **Parameters**: `mode` (string)
- **Response**: `{"success": true, "mode": "green"}`
- **Error Response**: `{"success": false, "message": "Failed to set LED for mode: ..."}`

#### 7. Help
`GET /help`
- **Response**: Returns a JSON list of all available endpoints and usage hints.

## Example curl Commands
```bash
# Set mode to red
curl "http://TABLET_IP:8080/led?mode=red"

# Turn LED off
curl "http://TABLET_IP:8080/led/off"

# Get current state
curl "http://TABLET_IP:8080/status"
```

## Build Instructions
1. Open the project in **Android Studio**.
2. Sync Project with Gradle Files.
3. Build the APK via **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Alternatively, use the Gradle wrapper from the terminal:
```bash
./gradlew assembleDebug
```

## Installation
1. Enable **USB Debugging** on the tablet.
2. Install the APK via ADB:
   ```bash
   adb install app-debug.apk
   ```
3. Open the app once to grant any necessary permissions and allow the root manager (e.g., Magisk) to grant root access.

## Permissions & Android Behavior
The app declares and uses the following:
-   `INTERNET`: For the NanoHTTPD server.
-   `RECEIVE_BOOT_COMPLETED`: To start the service automatically after restart.
-   `WAKE_LOCK`: To ensure the CPU stays active during command processing.
-   `FOREGROUND_SERVICE`: To maintain a persistent background state.
-   `usesCleartextTraffic="true"`: Required for HTTP local network access.

## Project Structure
-   `LedService`: Manages the lifecycle of the HTTP server and the foreground notification.
-   `LedHttpServer`: Implements the NanoHTTPD logic and endpoint routing.
-   `LedController`: Core logic for state persistence, color mapping, and adaptive timing.
-   `ShellExecutor`: Robustly executes root commands and captures `ShellResult` (success, exitCode, stdout, stderr).
-   `WakeHelper`: Checks interactive state and manages display wakeup.
-   `BootReceiver`: Triggers the service on system startup.
-   `MainActivity`: Provides the Material 3 dashboard UI.

## Notes & Caveats
-   **Root Required**: This app will not function without root access. It must be able to call `su`.
-   **Hardware Specific**: Tested exclusively on the ALLNET RK3399 series.
-   **Display Dependency**: LED commands are only reliable when the display is awake. The app handles this, but the display will briefly turn on during every LED change.

## License
This project is licensed under the MIT License. See the LICENSE file for details.
