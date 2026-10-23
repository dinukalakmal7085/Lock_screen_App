# Phone test: 1.1.0-beta.1

Start with [Studio](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-studio.apk) to review the design. Use the [full edition](https://github.com/dinukalakmal7085/Lock_screen_App/raw/refs/heads/main/downloads/Luma-debug.apk) to test automatic display if Android permits installation. These editions replace each other and preserve appearance data. Keep Play Protect enabled. Neither APK is a Play Store release.

## Design pass

1. Select **Alpine** in Wallpapers. Open **Edit this look** and try all six clocks, colors, size and vertical position. Confirm the clock stays readable with **Depth** on/off.
2. Set the four widget slots to Battery, Calendar, Alarm and Focus. Enter a focus phrase. Set a real alarm in your clock app and compare its time. Save with a name.
3. Open **Saved**, switch to another theme and restore the named look. Confirm its clock, widgets, photo composition and depth return. Delete a disposable look.
4. Change an editor draft and tap Cancel. Confirm the active design was not changed. Rotate while editing and check the draft survives.
5. Choose a portrait camera photo. Check orientation, zoom, position and shade. Optional photo depth requires a transparent PNG of the same image canvas; an opaque image should be rejected with an explanation. Confirm saved photo looks remain usable after importing another photo.
6. Preview and hold the screen to switch looks. Apply the background to Android's lock screen. The system's own clock remains; this is expected, especially in Studio.
7. Try your normal font size, a larger size, gesture navigation and three-button navigation. Note clipped controls, unreadable clocks or missing bottom buttons.

## Full edition: automatic display

1. Read the in-app disclosure, then choose **Settings → Set up lock screen**. Explicitly enable **Luma lock screen** in Android Accessibility settings. Luma never enables this access for you.
2. Lock the phone with its existing PIN/biometrics. Sleep/wake several times. Check that the chosen face appears, the clock updates and swiping up or **Open device** reaches Android's secure unlock.
3. Check both a successful unlock and a cancelled PIN attempt. Hold-to-switch and camera access must require Android unlock while locked. The × control must let you close Luma.
4. Receive a call while locked. Confirm Android's call and credential screens stay reachable. Report any overlay that obstructs them; turn the service off if this happens.
5. Optionally grant notification access and enable **Show notifications**. Post, update and dismiss real notifications. Cards should update within the 15-second refresh period. Titles remain hidden while locked and for private/secret notifications.
6. Optionally enable the flashlight shortcut and grant its camera permission. Check torch on/off and the camera-app shortcut independently. Declining permission must leave the rest of the app usable.
7. Turn off the lock screen in Luma Settings; wake the phone again and confirm no Luma overlay appears. Revoke optional access in Android settings and repeat. Recheck after a reboot and after leaving the phone idle to assess Redmi battery restrictions.

For feedback, include the edition, theme/clock, exact steps, expected/actual result, and phone/Android version. A screen recording or screenshot is useful for layout problems. New-beta wake and installation behaviour on Redmi 13C / Android 15 has not yet been verified in the cloud.
