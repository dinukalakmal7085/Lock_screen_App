package com.luma.lockscreen;

import android.content.Context;
import java.util.Collections;
import java.util.List;

/** Studio has no companion service, notification listener, or special-access capability. */
final class EditionFeatures {
  static final boolean HAS_COMPANION = false;

  static boolean isActive() {
    return false;
  }

  static void start(Context context) {
    throw new UnsupportedOperationException("Studio has no companion service");
  }

  static void stop(Context context) {}

  static List<NotificationSummary> summaries() {
    return Collections.emptyList();
  }

  static boolean notificationAccess(Context context) {
    return false;
  }
}
