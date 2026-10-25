# LinkPad

Android virtual gamepad that publishes a standard Bluetooth HID Gamepad profile. It targets Android 9+ (API 28) and is designed for an Android phone such as the Redmi K60 Ultra to control Windows, compatible Android hosts, and iPhone games that support Bluetooth game controllers.

## Pairing

1. Install the generated APK on the Android controller and allow **Nearby devices**.
2. Keep LinkPad in the foreground; Android unregisters a HID Device app in the background.
3. On the target iPhone: **Settings → Bluetooth**, then select `LinkPad Gamepad`. On Windows use **Settings → Bluetooth & devices**.
4. Start a game with controller support. LinkPad deliberately sends only standard controller reports; it does not inject touch events into other apps.

## Build

Requires JDK 17, Android SDK platform 35, and Gradle 8.9+:

```powershell
gradle :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
