# LED HTTP Service for ALLNET Meeting Room Tablet

## Overview
LED HTTP Service is a specialized Android application designed for the **ALLNET Meeting Room RGB LED Tablet** (RK3399). It acts as a bridge between your network and the tablet's hardware LED bar, exposing a local HTTP API to control the light frame remotely.

The app is built to be a robust background utility, running as a persistent Foreground Service that automatically starts on system boot. This ensures that the LED control remains available at all times without manual intervention, making it ideal for integration with room booking systems, automation platforms, or status indicators.

## Features
- **Local HTTP API**: Lightweight server (NanoHTTPD) running on port 8080.
- **Hardware LED Control**: Direct manipulation of the RGB LED bar via root-level sysfs commands.
- **Smart Display Management**: Automatically wakes the tablet display before every LED command, as required by the hardware.
- **Persistent State**: Remembers the last set mode across service restarts and reboots.
- **Auto-Start on Boot**: Automatically initializes the service when the tablet finishes booting.
- **Foreground Service**: Ensures Android does not kill the process during background operation.
- **Material 3 Dashboard**: Clean, tablet-optimized local UI to monitor service status and manually test modes.

## Hardware / Target Device
This application is specifically developed and tested for:
- **Device**: ALLNET Meeting Room RGB LED Tablet (10" or 15" variants).
- **Chipset**: Rockchip RK3399.
- **OS**: Android 10.
- **Requirement**: The device **must have root access** for the shell commands to reach the hardware platform.

## How It Works
### Hardware Command
The LED bar is controlled by writing specific hex codes to the kernel's platform driver:
`echo 'w $CODE' > /sys/devices/platform/led_con_h/zigbee_reset`

### Display Wake Logic
On this specific hardware, the LED controller only accepts state changes when the display is active. Before every command sequence, the app executes:
`input keyevent KEYCODE_WAKEUP`

### Power-On Sequence
If the device is currently in the `off` mode, it must be logically "powered on" before a color can be applied. When switching from `off` or `unknown` to a color/effect, the app:
1. Wakes the display.
2. Sends the internal `on` (0x03) command.
3. Waits for the hardware to stabilize (approx. 200-500ms).
4. Sends the requested target mode.

## LED Modes
The following modes are supported by the public API, listed in their logical groupings:

**Brightness & Power**
- `dim_up`, `dim_down`
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
- **Response**: `{"modes": ["dim_up", "dim_down", "off", "on", ... ]}`

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
# Set the LED to red
curl "http://TABLET_IP:8080/led?mode=red"

# Turn the LED off
curl "http://TABLET_IP:8080/led/off"

# Check if the service is alive
curl "http://TABLET_IP:8080/health"

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
- `INTERNET`: For the NanoHTTPD server.
- `RECEIVE_BOOT_COMPLETED`: To start the service automatically after restart.
- `WAKE_LOCK`: To ensure the CPU stays active during command processing.
- `FOREGROUND_SERVICE`: To maintain a persistent background state.
- `usesCleartextTraffic="true"`: Required for HTTP local network access.

## Project Structure
- `LedService`: Manages the lifecycle of the HTTP server and the foreground notification.
- `LedHttpServer`: Implements the NanoHTTPD logic and endpoint routing.
- `LedController`: Handles the logical state, color mapping, and hardware timing.
- `ShellExecutor`: Utility for executing commands via `su`.
- `WakeHelper`: Encapsulates the display wake logic.
- `BootReceiver`: Triggers the service on system startup.
- `MainActivity`: Provides the Material 3 dashboard UI.

## Notes & Caveats
- **Root Required**: This app will not function without root access. It must be able to call `su`.
- **Hardware Specific**: The sysfs path used (`/sys/devices/platform/led_con_h/zigbee_reset`) is specific to the ALLNET RK3399 tablet series.
- **Display Dependency**: LED commands are only reliable when the display is awake. The app handles this, but the display will briefly turn on during every LED change.

## License
This project is licensed under the MIT License. See the LICENSE file for details.
