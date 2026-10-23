package com.luma.lockscreen;

/** A device-local display summary; populated only by the full edition. */
final class NotificationSummary {
  final String app, title;

  NotificationSummary(String app, String title) {
    this.app = app;
    this.title = title;
  }
}
