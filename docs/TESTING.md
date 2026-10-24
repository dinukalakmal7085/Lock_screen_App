# Phone test: 1.3.0-beta.1

Start with [Studio](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-studio.apk) to review the design. Use the [full edition](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-debug.apk) to test automatic display if Android permits installation. These editions replace each other and preserve appearance data. Keep Play Protect enabled. Neither APK is a Play Store release.

## Glass rendering pass

1. Select Abyss, Cloudwalker and Eclipse Guardian. Choose **Glass XL → Clear**, default size and full width. Compare the live clock to the [native rendering proofs](GLASS_RENDERING.md): rounded counters, consistent stroke weight, a bright directional bevel and wallpaper visible through the body.
2. Compare Clear, Frosted and Solid on both bright and dark areas. Move the clock vertically and horizontally; refraction must sample the wallpaper behind its new position. Change the width and size, then save and restore the look.
3. Check the actual current time and a minute rollover. The new numeral shapes apply to saved Glass XL looks too; previous size/position/width preferences are preserved. A full preset starts without a date; enabling the date should add its row without leaving an empty spacer when disabled.
4. Compare full preview, editor preview, saved looks and the actual wake screen. Inspect rounded edges for jagged pixels at normal viewing size. Report visible stalls when opening a collection, moving editor sliders or changing the time.

## Design pass

1. Open **Wallpapers → Cinematic** and select **Abyss**, then **Cloudwalker**. Compare **Clear**, **Frosted** and **Solid** in the preview; use the arrows to browse without applying. Open **Customize** and try all seven clocks, including **Glass XL**, colors, size and vertical position. Confirm the clock stays readable with **Depth** on/off.
2. Cinematic presets start with a clean face and no widgets. Set the four widget slots to Battery, Calendar, Alarm and Focus. Enter a focus phrase. Set a real alarm in your clock app and compare its time. Save with a name.
3. Back out of a theme preview and confirm your active look stays intact. Use **Use theme** and confirm only that explicit action applies it. Rotate with a preview draft and a filtered gallery open; the draft and filters should survive. Open **Saved**, switch to another theme and restore the named look. Confirm its clock, widgets, photo composition and depth return. Delete a disposable look.
4. Change an editor draft and tap Cancel. Confirm the active design was not changed. Rotate while editing and check the draft survives.
5. Choose a portrait camera photo. Check orientation, zoom, position and shade. Optional photo depth requires a transparent PNG of the same image canvas; an opaque image should be rejected with an explanation. Confirm saved photo looks remain usable after importing another photo.
6. Preview and hold the screen to switch looks. Apply the background to Android's lock screen. The system's own clock remains; this is expected, especially in Studio.
7. Try your normal font size, a larger size, gesture navigation and three-button navigation. In full-screen preview, system bars should hide until an edge swipe. On the automatic overlay, record any bars retained by HyperOS. Swipe upward starting on each shortcut; this should unlock without launching the camera or toggling the torch. Note clipped controls, unreadable clocks or missing bottom buttons.

## Full edition: automatic display

1. Read the in-app disclosure, then choose **Settings → Set up lock screen**. Explicitly enable **Luma lock screen** in Android Accessibility settings. Luma never enables this access for you.
2. In Settings, use **Match system lock wallpaper** if you want Android to use the same background. Open Preview, then sleep the phone; the preview must close. Lock the phone with its existing PIN/biometrics. Sleep/wake several times, both from Luma and from another app. Compare the wake transition with the previous build; a native system frame can still appear because Luma does not replace keyguard. Check that the chosen face appears, the clock updates and swiping up or tapping **Swipe up to unlock** reaches Android's secure unlock.
3. Check both a successful unlock and a cancelled PIN attempt. Cancellation must return to Android without leaving a Luma authentication activity in front. Hold-to-switch and camera access must require Android unlock while locked. The × control must let you close Luma.
4. Receive a call while locked. Confirm Android's call and credential screens stay reachable. Report any overlay that obstructs them; turn the service off if this happens.
5. Optionally grant notification access and enable **Show notifications**. Post, update and dismiss real notifications. Cards should update within the 15-second refresh period. Titles remain hidden while locked and for private/secret notifications.
6. Optionally enable the flashlight shortcut and grant its camera permission. Check torch on/off and the camera-app shortcut independently. Declining permission must leave the rest of the app usable.
7. Turn off the lock screen in Luma Settings; wake the phone again and confirm no Luma overlay appears. Revoke optional access in Android settings and repeat. Recheck after a reboot and after leaving the phone idle to assess Redmi battery restrictions.

For feedback, include the edition, theme/clock, exact steps, expected/actual result, and phone/Android version. A screen recording or screenshot is useful for layout problems. The user reported wake delay and visual defects in the prior full beta on Redmi 13C / Android 15. Version 1.3.0 needs a physical-phone retest; simulated tests cannot establish HyperOS timing or system-bar behavior.
