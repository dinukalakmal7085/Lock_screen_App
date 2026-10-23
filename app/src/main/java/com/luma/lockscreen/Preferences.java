package com.luma.lockscreen;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local customization. No analytics, accounts, or network calls. */
public final class Preferences {
  public static final String[] WALLPAPERS = {"Iris", "Dusk", "Tide", "Ember", "Moss", "Graphite"};
  public static final String[] CLOCKS = {"Liquid", "Classic", "Outline"};
  private final SharedPreferences prefs;

  public Preferences(Context context) {
    prefs = context.getSharedPreferences("luma", Context.MODE_PRIVATE);
  }

  public int wallpaper() {
    return Math.max(0, Math.min(5, prefs.getInt("wallpaper", 0)));
  }

  public void wallpaper(int index) {
    prefs
        .edit()
        .putInt("wallpaper", Math.max(0, Math.min(5, index)))
        .putBoolean("photo", false)
        .apply();
  }

  public int clock() {
    return Math.max(0, Math.min(2, prefs.getInt("clock", 0)));
  }

  public void clock(int index) {
    prefs.edit().putInt("clock", Math.max(0, Math.min(2, index))).apply();
  }

  public boolean flag(String key, boolean fallback) {
    return prefs.getBoolean(key, fallback);
  }

  public void setFlag(String key, boolean value) {
    prefs.edit().putBoolean(key, value).apply();
  }

  public String wallpaperName() {
    return flag("photo", false) ? "Your photo" : WALLPAPERS[wallpaper()];
  }
}
