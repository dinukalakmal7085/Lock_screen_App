package com.luma.lockscreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import java.util.List;

final class EditionFeatures {
  static final boolean HAS_COMPANION = true;

  static boolean isActive() {
    return LockScreenService.isActiveOrStarting();
  }

  static void start(Context context) {
    LockScreenService.requestStart(context);
  }

  static void stop(Context context) {
    context.stopService(new Intent(context, LockScreenService.class));
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
