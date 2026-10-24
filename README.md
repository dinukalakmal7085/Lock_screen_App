# Luma

An offline, native Android lock-screen personalizer with an iPhone-inspired composition and original artwork. Version **1.3.0-beta.1** is for phone testing and iteration; Play Store publication is deferred.

## Download the beta

| Edition | Download | Behaviour |
| --- | --- | --- |
| Luma Studio | [Luma-studio.apk](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-studio.apk) | Full visual editor, saved looks, preview and system wallpaper. Only `SET_WALLPAPER`; no services or notification access. |
| Luma | [Luma-debug.apk](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-debug.apk) | Same editor plus an optional accessibility lock-screen overlay, notification summaries and flashlight shortcut. |

Open a download link on your phone and open the downloaded APK. Requires Android 8.0 or newer. If GitHub opens a file page, choose **Download raw file**. Both editions share the existing package (`com.luma.lockscreen`) and debug signing identity, so installing one replaces the other and preserves appearance data. [SHA-256 checksums](downloads/SHA256SUMS.txt) accompany the APKs.

The user has now installed the full edition on a Redmi 13C / Android 15 and reported delayed wake display, visible system bars and weak glass contrast. The wake update addressed those reports in code; this version rebuilds the clock renderer after additional visual reference samples; the revised APK still needs a physical-phone retest. Earlier APKs received a Play Protect sensitive-data warning. Keep Play Protect enabled; cloud validation does not establish Google's installation verdict.

## What changed

- **Rebuilt Glass XL typography:** original tabular vector numerals with a consistent optical weight, taller proportions, more breathing room and a balanced colon. Other styles and localized non-Latin digits retain Android font shaping.
- **Rendered glass surfaces:** wallpaper sampling and edge refraction, directional bevel highlights from an exact distance field, a translucent body and a softer Frosted finish. Solid remains a clean vector fill. The material is cached until time, geometry or backdrop placement changes, and released on detach.
- **Visual proofs:** the app renderer exports native 720 × 1600 ocean, space and dark-scene examples, with multiple times and all three finishes. Geometry is supersampled within a fixed pixel budget; gallery textures remain bounded. Hidden dates no longer reserve an empty row above the clock.

- **Wake display:** attempt immediately on SCREEN_ON, with bounded retries while keyguard becomes ready. Prepare the next face and its image texture during screen-off. Refresh notification privacy before attaching the prepared face.
- **Preview lifecycle:** a preview closes on screen-off and no longer requests display above keyguard. Only the explicit authentication bridge can show while locked; cancellation/error closes it.
- **Full-screen presentation:** shared preview/overlay controls, transient system bars, cutout/navigation-aware button placement, real vector shortcuts and one swipe prompt. Upward swipes starting on a shortcut also unlock. OEM overlay policies may still keep system bars visible.
- **Glass quality:** stronger translucent fill and reflective edges, filtered higher-resolution glass texture, with bounded thumbnail texture sizes.
- **Matching wallpaper:** Settings offers **Match system lock wallpaper**, with explicit confirmation. Android controls its notification shade and secure keyguard, so a system frame can still appear before Luma or during authentication.

- **Preview before applying:** tap any theme to open a private preview, move through the current collection, favorite it, compare glass finishes and toggle available depth. **Use theme** applies the draft. **Customize** opens the editor with that draft; Cancel keeps the active look untouched.
- **Glass control:** Clear, Frosted and Solid finishes plus adjustable clock width, saved with each look.
- **Consistent composition:** gallery, home, saved looks, switcher and editor share a proportional portrait renderer, with bounded image decoding for thumbnails. Search, collection and filter position survive rotation.


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

1. Open **Wallpapers → Cinematic** and tap **Abyss** or **Cloudwalker**. Compare the glass finishes and depth, then choose **Use theme** or **Customize**.
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

Robolectric checks cover real activity navigation, editor drafts and rotation, Save/Cancel, four-widget configuration, saved looks, favorites, image import, theme rendering/search, preview cancellation/application/customization, rotation of browser drafts and filters, glass finishes and width, bounded cinematic image decoding, authored subject boundaries, live glass-clock redraw, secure-unlock gating, preview sleep teardown, authentication cancellation, shortcut swipes, navigation insets, glass contrast, all ten digit shapes and tabular metrics, bevel lighting, retained backdrop color, repeated-frame cache reuse, time changes and API 26/35/36 compatibility. Full-edition checks additionally exercise immediate and delayed-keyguard wake guards, prepared-design refresh, first-frame notification redaction, yielding to credential prompts, notification privacy, updates and dismissal. Studio checks verify its reduced settings and permission/service boundary. These checks simulate Android; they do not prove manufacturer-specific lock-screen behaviour.

The beta passes **90 simulated checks** (49 full, 41 Studio) and both Android lint checks with zero errors. Remaining lint warnings concern English UI literals and newer attributes ignored on older Android versions.

`verify_apks.py` inspects the compiled APKs: signatures, exact requested permissions, service bindings and DEX classes. Full uses only `SET_WALLPAPER` and optional `CAMERA` as ordinary permissions; its two services require protected Android binding permissions. Studio has only `SET_WALLPAPER` and excludes both service implementations. Neither edition requests ordinary application-overlay, foreground-service, notification-posting, storage or Internet permissions.

[Review the new glass renderer](docs/GLASS_RENDERING.md). [View the revised 720 × 1600 lock screen](docs/screenshots/redmi-cloudwalker.png). [Preview the theme browser](docs/screenshots/theme-browser.png) and [cinematic gallery](docs/screenshots/cinematic-gallery.png), [ocean lock screen](docs/screenshots/cinematic-ocean.png), [space lock screen](docs/screenshots/cinematic-space.png) and [original landscape](docs/screenshots/lock-screen.png), [editor](docs/screenshots/editor.png) and [themes](docs/screenshots/themes.png). These images come from simulated Android rendering, not a physical phone.

Test reports are in `app/build/reports/`; generated native rendering screenshots are in `app/build/screenshots/`. [Reference notes](docs/REFERENCE.md) distinguish the listing's features from this beta. Music controls, stickers, automatic photo segmentation and weather are not implemented. Debug APKs are for testing; signing, store materials, access-service policy review and store distribution will be addressed after the user tests and improves the app.

## Privacy

Photos, cutouts, preferences and saved designs stay in private app storage. Accessibility access is optional and uses transient window-state metadata to display the chosen face and yield to recognized prompts; it does not retrieve screen text, inject input or automate gestures. Notification access is separate. Up to three recent summaries display from memory; secret notifications are excluded, private titles stay hidden, and public titles require explicit enablement plus an unlocked device. No notification messages or credentials are persisted or uploaded.

Android settings can revoke optional access. Studio clears unused service/display flags when replacing the full edition. Saved designs contain appearance fields only, so restoring one cannot grant or enable access. Uninstalling removes app-private data; the applied system wallpaper remains under Android's control.
