# Voice Commander

An advanced Android App built with Kotlin. It provides a control panel and a foreground service with a floating bubble. It listens to Arabic or English voice commands to perform actions globally across the device.

## Features
- **Control Panel**: Material Design dashboard to safely grant and monitor permissions (Audio, Camera, Overlay, Accessibility, Device Admin).
- **Floating Bubble**: Tap to toggle voice recognition.
- **Bilingual Voice Recognition**: Interprets Arabic ("ar"), English ("en"), or Both.
- **Deep Integrations**:
  - Open arbitrary Apps (Package Manager)
  - Control Hardware (Camera Flashlight)
  - Screen Controls (Lock via Device Policy Manager, Sleep/Wake via Power Manager)
  - Global Actions (Close App, Power Menu via Accessibility Service)

## Installation and Setup
1. Download this workspace as a ZIP file (top-right menu > Export).
2. Open the project in Android Studio.
3. Build and install the APK to your device.
4. Launch the app to view the **Control Panel**.
5. Grant all permissions required using the UI buttons:
   - Microphone & Camera
   - Overlay (Navigate to Settings)
   - Accessibility Service (Navigate to Settings & switch on "Voice Commander")
   - Device Admin (Activate)
6. Choose your language.
7. Click **Start Service**.

Tap the floating bubble globally on any app to speak a command! See the Help section for examples.
