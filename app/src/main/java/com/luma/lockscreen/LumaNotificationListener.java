package com.luma.lockscreen;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.*;

/** Opt-in, in-memory summaries only. Sensitive notification contents are never displayed. */
public final class LumaNotificationListener extends NotificationListenerService {
  public static final class Entry {
    public final String app, title;

    Entry(String app, String title) {
      this.app = app;
      this.title = title;
    }
  }

  private static final Map<String, Entry> active = new LinkedHashMap<>();

  public static synchronized List<Entry> entries() {
    List<Entry> result = new ArrayList<>(active.values());
    Collections.reverse(result);
    return result.subList(0, Math.min(3, result.size()));
  }

  @Override
  public void onListenerConnected() {
    synchronized (LumaNotificationListener.class) {
      active.clear();
    }
    StatusBarNotification[] all = getActiveNotifications();
    if (all != null) for (StatusBarNotification n : all) onNotificationPosted(n);
  }

  @Override
  public void onNotificationPosted(StatusBarNotification sbn) {
    Notification n = sbn.getNotification();
    if (sbn.getPackageName().equals(getPackageName())
        || sbn.isOngoing()
        || n.visibility == Notification.VISIBILITY_SECRET) return;
    String label = sbn.getPackageName();
    try {
      label =
          getPackageManager()
              .getApplicationLabel(getPackageManager().getApplicationInfo(label, 0))
              .toString();
    } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
    }
    CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE, "");
    // A private notification's title stays private even when the device is unlocked.
    String safeTitle =
        n.visibility == Notification.VISIBILITY_PUBLIC ? String.valueOf(title) : "New notification";
    synchronized (LumaNotificationListener.class) {
      active.put(sbn.getKey(), new Entry(label, safeTitle));
    }
  }

  @Override
  public void onNotificationRemoved(StatusBarNotification sbn) {
    synchronized (LumaNotificationListener.class) {
      active.remove(sbn.getKey());
    }
  }

  @Override
  public void onListenerDisconnected() {
    synchronized (LumaNotificationListener.class) {
      active.clear();
    }
  }
}
