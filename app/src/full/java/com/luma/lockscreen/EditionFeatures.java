package com.luma.lockscreen;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import java.util.List;

final class EditionFeatures {
  private static String torchId;
  private static boolean torchOn;

  static String flashlight(Context c) {
    if (c.checkSelfPermission(android.Manifest.permission.CAMERA)
        != android.content.pm.PackageManager.PERMISSION_GRANTED)
      return "Enable the flashlight shortcut in Luma Settings first.";
    try {
      android.hardware.camera2.CameraManager manager =
          c.getSystemService(android.hardware.camera2.CameraManager.class);
      if (torchId == null)
        for (String id : manager.getCameraIdList()) {
          Boolean flash =
              manager
                  .getCameraCharacteristics(id)
                  .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
          if (Boolean.TRUE.equals(flash)) {
            torchId = id;
            break;
          }
        }
      if (torchId == null) return "No flashlight is available on this device.";
      manager.setTorchMode(torchId, !torchOn);
      torchOn = !torchOn;
      return torchOn ? "Flashlight on" : "Flashlight off";
    } catch (android.hardware.camera2.CameraAccessException | SecurityException e) {
      return "The flashlight is unavailable right now.";
    }
  }

  static void stopFlashlight(Context c) {
    if (torchOn && torchId != null)
      try {
        c.getSystemService(android.hardware.camera2.CameraManager.class)
            .setTorchMode(torchId, false);
        torchOn = false;
      } catch (Exception ignored) {
      }
  }

  static final boolean HAS_COMPANION = true;

  static boolean isActive() {
    return LockScreenService.isActiveOrStarting();
  }

  static void start(Context context) {
    LockScreenService.requestStart(context);
  }

  static void stop(Context context) {
    LockScreenService.stop(context);
  }

  static boolean accessGranted(Context context) {
    String enabled =
        Settings.Secure.getString(
            context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    if (enabled == null) return false;
    String own = new ComponentName(context, LockScreenService.class).flattenToString();
    for (String value : enabled.split(":")) if (value.equals(own)) return true;
    return false;
  }

  static List<NotificationSummary> summaries() {
    return LumaNotificationListener.entries();
  }

  static boolean notificationAccess(Context context) {
    String listeners =
        Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
    if (listeners == null) return false;
    String own = new ComponentName(context, LumaNotificationListener.class).flattenToString();
    for (String value : listeners.split(":")) if (value.equals(own)) return true;
    return false;
  }
}
