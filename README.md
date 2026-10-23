# Luma

A native, offline Android lock-screen personalizer with original liquid-glass artwork. Built from an empty repository using the requested glass-style lock-screen concept; no assets or source were copied from the linked Play Store app.

## Features

- Studio with a live clock and full-screen preview.
- Six procedural wallpapers: Iris, Dusk, Tide, Ember, Moss, and Graphite.
- Liquid, Classic, and Outline clock faces; 12/24-hour display.
- Date and live battery widgets.
- Local photo import through Android's document picker, with EXIF orientation handling.
- Apply the selected background to the Android lock screen.
- Optional notification summaries, with private content concealed and no upload or persistence.
- Optional foreground screen companion with swipe-to-continue, close, and persistent turn-off controls.
- Device-local preferences; no accounts, ads, analytics, or Internet permission.

## Run

Requires Android 8.0 (API 26) or newer. Open this folder in Android Studio and let it use its installed SDK and JDK 17 or 21. Install Android SDK 35 and Build Tools 35.0.0 when prompted. Run the `app` configuration on a phone or emulator.

Cloud workspace:

```bash
cd /workspace/Lock_screen_App
bash scripts/bootstrap.sh
source scripts/env.sh
./gradlew assembleDebug testDebugUnitTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.luma.lockscreen/.MainActivity
```

`bootstrap.sh` installs verified, pinned toolchains under `/workspace/toolchains` and preserves the cloud proxy and TLS trust. `env.sh` is cloud-specific; Android Studio does not need it. The Gradle wrapper downloads its pinned distribution with SHA-256 verification. AndroidX ExifInterface handles photo orientation safely across Android versions. JUnit and Robolectric are test-only dependencies.

## Using the app

1. Choose a wallpaper in Studio or Wallpapers, then select a clock style.
2. Open **Full preview** to see the complete face. Swipe up or tap **Open device** to invoke Android's existing secure unlock when locked.
3. Tap **Apply lock wallpaper** to change the system background. Android keeps its own clock; third-party apps cannot replace the system keyguard's clock through this API.
4. In Settings, optionally enable **Screen companion**. Android will ask for display-over-other-apps access and, on Android 13+, notification permission. Wake the screen to use it. Close it with **Continue**, **Close**, or a swipe up.
5. To display notifications, separately grant notification-listener access and enable **Show notifications**. Public titles are optional and shown only while the device is unlocked. Private and secret contents are not displayed.

## Android behavior and release scope

Luma never removes or bypasses a device PIN, password, pattern, or biometrics. Android places application overlays below critical system windows, including the secure keyguard on many devices. The screen companion may therefore appear **after unlocking**, depending on Android and manufacturer behavior. The full-screen preview and system wallpaper application are available independently.

The companion runs only after explicit enablement, with an ongoing notification and a **Turn off** action. It does not use accessibility services or full-screen notification intents. It does not automatically start at boot; open Luma and enable it again after restarting. Manufacturer battery restrictions can stop it. Test wake/sleep, keyguard, and battery behavior on target physical phones before distribution.

The included APK is a debug build for development. Play Store release signing, store assets, foreground-service permission declarations, device compatibility testing, and policy review remain release tasks. The linked reference listing could not be inspected because this environment blocks `play.google.com`; Luma uses an original design based on the agreed feature defaults.

## Validation

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Robolectric tests exercise real Android activities, tab navigation, preference persistence, wallpaper rendering, local photo loading, clock/widget configuration, explicit notification permission handling, notification filtering, and preview dismissal. Rendered test screenshots are generated in `app/build/screenshots/`. Test results and lint reports are in `app/build/reports/`.

Manual device checks:

- Import a portrait photo, including a rotated camera photo; confirm its orientation.
- Apply a wallpaper and inspect the real secure lock screen.
- Deny, grant, and revoke overlay/notification permissions.
- Enable the companion; wake/sleep the phone, unlock securely, swipe to dismiss, and turn it off from its notification.
- Confirm notification contents stay hidden on a locked phone.
- Rotate, increase system font size, and test gesture and three-button navigation.

## Privacy

Preferences and the selected photo are stored in this app's private storage. Notification summaries exist only in memory while Android grants listener access. Secret notifications are excluded; private notifications never reveal their titles. No notification actions, messages, or credentials are stored. There is no network permission. Uninstalling the app removes its private data; Android settings can revoke all optional access.
