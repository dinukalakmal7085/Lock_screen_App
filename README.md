# Luma

An offline, native Android lock-screen personalizer with an iPhone-inspired composition and original artwork. Version **1.2.0-beta.1** is for phone testing and iteration; Play Store publication is deferred.

## Download the beta

| Edition | Download | Behaviour |
| --- | --- | --- |
| Luma Studio | [Luma-studio.apk](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-studio.apk) | Full visual editor, saved looks, preview and system wallpaper. Only `SET_WALLPAPER`; no services or notification access. |
| Luma | [Luma-debug.apk](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-debug.apk) | Same editor plus an optional accessibility lock-screen overlay, notification summaries and flashlight shortcut. |

Open a download link on your phone and open the downloaded APK. Requires Android 8.0 or newer. If GitHub opens a file page, choose **Download raw file**. Both editions share the existing package (`com.luma.lockscreen`) and debug signing identity, so installing one replaces the other and preserves appearance data. [SHA-256 checksums](downloads/SHA256SUMS.txt) accompany the APKs.

The user tested the previous Studio edition on a Redmi 13C / Android 15. That phone previously blocked the full edition with a Play Protect sensitive-data warning. The new full beta has not been verified on that phone and may still be blocked. Keep Play Protect enabled; a cloud build cannot establish Google's installation verdict. Studio supports testing all visual features without the full edition's access services.

## What changed

- **41 original theme presets:** nine new cinematic photographic and 3D scenes plus the existing 32 procedural themes. Search, favorite and browse Cinematic, Ocean, Space, Sport, Architecture, Fantasy, Abstract and the original collections.
- **Live editor with Save and Cancel:** seven clock styles including the new tall **Glass XL**, eight ink colors, size, vertical position, alignment, four date formats and 12/24-hour display.
- **Up to four compact widgets:** real battery, date, next alarm and a personal focus phrase. Missing device data displays a dash.
- **Layered depth:** built-in mountains and six cinematic subjects can cover part of the live clock using authored boundaries. Photo depth uses a separately imported transparent PNG cutout; automatic subject removal is not included.
- **Photo composition:** Android's picker, EXIF orientation, zoom, horizontal/vertical position and shade. Originals are copied into private app storage; saved looks retain their own image references.
- **Named looks:** save, restore, edit and delete up to 20 designs. Hold the full-screen face to open the horizontal look switcher, after Android unlock if necessary.
- **Full edition:** opt-in accessibility overlay on locked screen wake, secure unlock bridge, camera-app shortcut, optional torch and privacy-aware notification cards.
- Local preferences and artwork. No accounts, ads, analytics, remote catalogue or Internet permission.

The new themes are **Abyss**, **Cloudwalker**, **Velocity**, **Stillwater**, **Eclipse Guardian**, **Emerald Halo**, **Helio Dunes**, **Soft Orbit** and **Stadium Flight**. Their clocks are drawn live rather than embedded in the image. Original AI-generated artwork is bundled for offline use; provenance and file hashes are documented in [ARTWORK.md](docs/ARTWORK.md).

## First phone test

1. Open **Wallpapers → Cinematic**, select **Abyss** or **Cloudwalker**, then **Edit this look**.
2. Change the clock, widgets and depth. Name the look and tap **Save**. Check **Saved** and **Full preview**.
3. Hold the preview to switch looks. Try a portrait photo under **Backdrop**; photo depth requires a matching transparent PNG with the same canvas and subject position.
4. **Apply lock wallpaper** changes Android's wallpaper. Android retains its system clock. Studio's custom clock and widgets appear in Luma's preview only.
5. For the full edition's automatic screen, use **Settings → Set up lock screen**, read the disclosure and explicitly enable **Luma lock screen** in Android Accessibility settings. The service uses window-state metadata to yield to recognized system prompts; it cannot retrieve screen content or perform gestures.
6. Notification access and flashlight permission are separate, optional choices. The torch uses Android's camera permission; Luma never captures photos. The camera shortcut opens your existing camera app after unlock.

The full screen is a third-party overlay, not a replacement for Android's secure keyguard. PIN, pattern, password and biometrics remain with Android. Wake, calls, credential screens, manufacturer power management and navigation insets need testing on actual phones. See [the focused phone test guide](docs/TESTING.md).

## Build

Open the project in Android Studio with SDK 36, Build Tools 36.0.0 and JDK 17 or 21. The pinned build uses Android Gradle Plugin 8.11.1 and Gradle 8.13.

In the cloud workspace:

```bash
cd /workspace/Lock_screen_App
bash scripts/bootstrap.sh
source scripts/env.sh
./gradlew --no-daemon --console=plain assembleFullDebug assembleStudioDebug testFullDebugUnitTest testStudioDebugUnitTest lintFullDebug lintStudioDebug
python3 scripts/verify_apks.py
```

`bootstrap.sh` installs checksum-verified JDK 21.0.8, Gradle and Android tools under `/workspace/toolchains`, preserves TLS verification and proxy settings, and runs the checks above. `env.sh` selects the retained cloud toolchain and writable caches. Android Studio does not require that script.

With an authorized connected phone:

```bash
adb devices
adb install -r app/build/outputs/apk/studio/debug/app-studio-debug.apk
adb shell am start -n com.luma.lockscreen/.MainActivity
```

Use `app/build/outputs/apk/full/debug/app-full-debug.apk` for the full edition. No physical Android device or hardware-accelerated emulator is connected to this workspace.

## Validation and scope

Robolectric checks cover real activity navigation, editor drafts and rotation, Save/Cancel, four-widget configuration, saved looks, favorites, image import, theme rendering/search, bounded cinematic image decoding, authored subject boundaries, live glass-clock redraw, secure-unlock gating and API 26/36 compatibility. Full-edition checks additionally exercise wake/lock guards, yielding to credential prompts, notification privacy, updates and dismissal. Studio checks verify its reduced settings and permission/service boundary. These checks simulate Android; they do not prove manufacturer-specific lock-screen behaviour.

The beta passes **52 simulated checks** (29 full, 23 Studio) and both Android lint checks with zero errors. Remaining lint warnings concern English UI literals and newer attributes ignored on older Android versions.

`verify_apks.py` inspects the compiled APKs: signatures, exact requested permissions, service bindings and DEX classes. Full uses only `SET_WALLPAPER` and optional `CAMERA` as ordinary permissions; its two services require protected Android binding permissions. Studio has only `SET_WALLPAPER` and excludes both service implementations. Neither edition requests ordinary application-overlay, foreground-service, notification-posting, storage or Internet permissions.

[Preview the cinematic gallery](docs/screenshots/cinematic-gallery.png), [ocean lock screen](docs/screenshots/cinematic-ocean.png), [space lock screen](docs/screenshots/cinematic-space.png) and [original landscape](docs/screenshots/lock-screen.png), [editor](docs/screenshots/editor.png) and [themes](docs/screenshots/themes.png). These images come from simulated Android rendering, not a physical phone.

Test reports are in `app/build/reports/`; generated native rendering screenshots are in `app/build/screenshots/`. [Reference notes](docs/REFERENCE.md) distinguish the listing's features from this beta. Music controls, stickers, automatic photo segmentation and weather are not implemented. Debug APKs are for testing; signing, store materials, access-service policy review and store distribution will be addressed after the user tests and improves the app.

## Privacy

Photos, cutouts, preferences and saved designs stay in private app storage. Accessibility access is optional and uses transient window-state metadata to display the chosen face and yield to recognized prompts; it does not retrieve screen text, inject input or automate gestures. Notification access is separate. Up to three recent summaries display from memory; secret notifications are excluded, private titles stay hidden, and public titles require explicit enablement plus an unlocked device. No notification messages or credentials are persisted or uploaded.

Android settings can revoke optional access. Studio clears unused service/display flags when replacing the full edition. Saved designs contain appearance fields only, so restoring one cannot grant or enable access. Uninstalling removes app-private data; the applied system wallpaper remains under Android's control.
