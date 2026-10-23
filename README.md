# Luma

A native, offline Android lock-screen personalizer with original liquid-glass artwork. Built from an empty repository using the requested glass-style lock-screen concept; no assets or source were copied from the linked Play Store app.

## Download and install

[Download Luma Studio for Android](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-studio.apk)

Studio is the wallpaper-and-preview edition. It includes the wallpaper collection, photo import, clock customization, and full-screen previews. It requests **only SET_WALLPAPER**, registers no services, and contains no notification-listener or overlay-service implementation. The phone retains its own clock on the system lock screen; Luma's custom clock and widgets are available in Preview. It does not automatically appear when the phone wakes or display other apps' notifications.

Open the link on your Android phone, download `Luma-studio.apk`, then open it to install. Requires Android 8.0 or newer. It is a development build, signed with the same debug certificate and application ID as the full edition. Installing Studio over the full edition replaces it while retaining appearance preferences and clearing unused companion/notification preferences.

If the direct link does not download, open [the Studio APK on GitHub](https://github.com/dinukalakmal7085/Lock_screen_App/blob/main/downloads/Luma-studio.apk) and select **Download raw file**. Sign in to GitHub if the repository is private. SHA-256 checksums are in [downloads/SHA256SUMS.txt](downloads/SHA256SUMS.txt).

The [full edition](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-debug.apk) retains the optional screen companion and notification summaries. A Redmi 13C running Android 15 reported a Play Protect sensitive-data installation block for that edition. The precise Play Protect decision has not been independently inspected. Studio removes the sensitive capabilities rather than hiding them. It has **not** been approved by Play Protect, and installation on that phone remains unverified. Keep Play Protect enabled. If installation remains blocked, use Google Play's testing/review distribution through the app owner's Play Console; that requires separate account access and setup.

## Features

- Studio with a live clock and full-screen preview.
- Six procedural wallpapers: Iris, Dusk, Tide, Ember, Moss, and Graphite.
- Liquid, Classic, and Outline clock faces; 12/24-hour display.
- Date and live battery widgets.
- Local photo import through Android's document picker, with EXIF orientation handling.
- Apply the selected background to the Android lock screen.
- Full edition: optional notification summaries, with private content concealed and no upload or persistence.
- Full edition: optional foreground screen companion with swipe-to-continue, close, and persistent turn-off controls.
- Device-local preferences; no accounts, ads, analytics, or Internet permission.

## Run

Requires Android 8.0 (API 26) or newer. Open this folder in Android Studio and let it use its installed SDK and JDK 17 or 21. Install Android SDK 35 and Build Tools 35.0.0 when prompted. Run the `app` configuration on a phone or emulator.

Cloud workspace:

```bash
cd /workspace/Lock_screen_App
bash scripts/bootstrap.sh
source scripts/env.sh
./gradlew assembleFullDebug assembleStudioDebug testFullDebugUnitTest testStudioDebugUnitTest lintFullDebug lintStudioDebug
python3 scripts/verify_apks.py
adb install -r app/build/outputs/apk/studio/debug/app-studio-debug.apk
adb shell am start -n com.luma.lockscreen/.MainActivity
```

`bootstrap.sh` installs verified, pinned toolchains under `/workspace/toolchains` and preserves the cloud proxy and TLS trust. `env.sh` is cloud-specific; Android Studio does not need it. The Gradle wrapper downloads its pinned distribution with SHA-256 verification. AndroidX ExifInterface handles photo orientation safely across Android versions. JUnit and Robolectric are test-only dependencies.

## Using the app

1. Choose a wallpaper in Studio or Wallpapers, then select a clock style.
2. Open **Full preview** to see the complete face. Swipe up or tap **Open device** to invoke Android's existing secure unlock when locked.
3. Tap **Apply lock wallpaper** to change the system background. Android keeps its own clock; third-party apps cannot replace the system keyguard's clock through this API.
4. Full edition only: in Settings, optionally enable **Screen companion**. Android will ask for display-over-other-apps access and, on Android 13+, notification permission. Wake the screen to use it. Close it with **Continue**, **Close**, or a swipe up.
5. Full edition only: to display notifications, separately grant notification-listener access and enable **Show notifications**. Public titles are optional and shown only while the device is unlocked. Private and secret contents are not displayed.

## Android behavior and release scope

Luma never removes or bypasses a device PIN, password, pattern, or biometrics. Android places application overlays below critical system windows, including the secure keyguard on many devices. The screen companion may therefore appear **after unlocking**, depending on Android and manufacturer behavior. The full-screen preview and system wallpaper application are available independently.

The companion runs only after explicit enablement, with an ongoing notification and a **Turn off** action. It does not use accessibility services or full-screen notification intents. It does not automatically start at boot; open Luma and enable it again after restarting. Manufacturer battery restrictions can stop it. Test wake/sleep, keyguard, and battery behavior on target physical phones before distribution.

The included APK is a debug build for development. Play Store release signing, store assets, foreground-service permission declarations, device compatibility testing, and policy review remain release tasks. The linked reference listing could not be inspected because this environment blocks `play.google.com`; Luma uses an original design based on the agreed feature defaults.

## Validation

```bash
./gradlew assembleFullDebug assembleStudioDebug testFullDebugUnitTest testStudioDebugUnitTest lintFullDebug lintStudioDebug
python3 scripts/verify_apks.py
```

Robolectric tests exercise real Android activities, tab navigation, preference persistence, wallpaper rendering, local photo loading, clock/widget configuration, explicit notification permission handling, notification filtering, and preview dismissal. Studio tests additionally verify its permission/service boundary, omitted implementations, reduced settings UI, and migration from full-edition preferences. Both editions include launch checks on API 26 and/or 35. `verify_apks.py` checks signatures, permissions, service declarations, and actual DEX contents for both APKs. Rendered test screenshots are generated in `app/build/screenshots/`. Test results and lint reports are in `app/build/reports/`.

Manual device checks:

- Import a portrait photo, including a rotated camera photo; confirm its orientation.
- Apply a wallpaper and inspect the real secure lock screen.
- Deny, grant, and revoke overlay/notification permissions.
- Enable the companion; wake/sleep the phone, unlock securely, swipe to dismiss, and turn it off from its notification.
- Confirm notification contents stay hidden on a locked phone.
- Rotate, increase system font size, and test gesture and three-button navigation.

## Privacy

Preferences and the selected photo are stored in this app's private storage. Studio has no notification or overlay access. In the full edition, notification summaries exist only in memory while Android grants listener access. Secret notifications are excluded; private notifications never reveal their titles. No notification actions, messages, or credentials are stored. Neither edition has network permission. Uninstalling the app removes its private data; Android settings can revoke the full edition's optional access.
