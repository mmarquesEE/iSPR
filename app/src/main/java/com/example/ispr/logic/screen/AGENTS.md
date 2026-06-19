# Screen Module: Technical Specification & Agents

This module is responsible for the discovery, extraction, and encapsulation of physical display characteristics. It serves as the source of truth for all geometry-dependent calculations within the application, particularly those requiring knowledge of pixel density and camera placement. It also provides low-level hardware control for precise optical stimulus generation.

## Core Agents

### 1. ScreenHardwareManager
The `ScreenHardwareManager` is the primary service provider for display-related hardware data. It acts as an abstraction layer over the Android `WindowManager` and `Display` APIs.

**Key Responsibilities:**
- Interfacing with `DisplayMetrics` to retrieve physical and logical DPI.
- Extracting `WindowInsets` to identify `DisplayCutout` geometry.
- Querying `HdrCapabilities` and wide color gamut support.

**Public API:**
- `getScreenInfo(): ScreenHardwareInfo`: 
  Constructs a comprehensive snapshot of the device's screen.

---

### 2. CameraCutoutInfo
A specialized geometric agent that translates system bounding boxes into usable center-point coordinates.

**Key Responsibilities:**
- Calculating the exact pixel-center of the camera lens.
- Providing normalized bounds for UI "perimeter" drawings.

**Public API:**
- `centerX: Float`: Returns the horizontal center of the camera cutout.
- `centerY: Float`: Returns the vertical center, adjusted for lens positioning.

---

### 3. ScreenHardwareInfo
The immutable data container representing the total state of the display hardware.

**Key Responsibilities:**
- Aggregating hardware signals (DPI, HDR, Resolution).
- Calculating precise pixel coordinates for physical measurements.

**Public API:**
- `toInfoRowContentList(): List<InfoRowContent>`: Formats data for technical summaries.
- `calculateRectangleBounds(distCm, widthCm, heightCm): Rect?`: Computes pixel coordinates for a physical area relative to the camera.

---

### 4. ScreenBrightnessManager
Handles the synchronization between system-wide brightness and window-specific high-luminance overrides.

**Key Responsibilities:**
- Capturing and restoring system brightness levels.
- Forcing the display hardware to maximum physical output for the current window.
- Calculating dimming factors for the UI layer to maintain a consistent visual experience for the user while the hardware is at 100%.

**Public API:**
- `synchronizeWithSystem()`: Saves current system state.
- `setMaxWindowBrightness()`: Overrides window brightness to 100%.
- `restoreSystemBrightness()`: Returns control to the system settings.
- `getUiDimmingAlpha()`: Provides the alpha value needed to dim SDR content.

---

### 5. ScreenHardwareControlledArea
A high-performance rendering agent that bypasses the Android View hierarchy to drive pixels directly.

**Key Responsibilities:**
- Managing a dedicated `SurfaceView` and its hardware surface.
- Running a high-frequency background rendering loop.
- Performing flicker-free sub-pixel drawing.

**Public API:**
- `updateBounds(Rect)`: Changes the target area of the controlled area.
- `updateColor(Int)`: Changes the spectral intensity/color of the controlled area.
- `start() / stop()`: Manages the lifecycle of the internal rendering thread.

## Interaction Flow
1. **Calibration**: `ScreenHardwareManager` provides the `ScreenHardwareInfo`.
2. **Setup**: `ScreenBrightnessManager` captures the baseline and ramps the hardware to max.
3. **Targeting**: `ScreenHardwareInfo.calculateRectangleBounds()` determines the precise pixel location.
4. **Injection**: `ScreenHardwareControlledArea` receives the coordinates and begins high-frequency modulation on the hardware surface.
5. **UI Protection**: The app uses `ScreenBrightnessManager.getUiDimmingAlpha()` to darken the UI components, preventing user discomfort from the 100% backlight.
