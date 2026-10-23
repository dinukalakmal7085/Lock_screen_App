package com.luma.lockscreen;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local customization. No analytics, accounts, or network calls. */
public final class Preferences {
  public static final String[] WALLPAPERS = ThemeCatalog.NAMES;
  public static final String[] CLOCKS = {
    "Liquid", "Classic", "Outline", "Stacked", "Editorial", "Mono"
  };
  public static final int[] INKS = {
    0xFFFDFBFF, 0xFFFFE8BF, 0xFFA8F0D6, 0xFFB7DAFF, 0xFFFFC2D1, 0xFFD5C1FF, 0xFF1A2330, 0xFFF8AF72
  };
  static final String[] DESIGN_KEYS = {
    "wallpaper",
    "photo",
    "photoFile",
    "foregroundFile",
    "clock",
    "clockSize",
    "clockOffset",
    "ink",
    "alignment",
    "dateFormat",
    "date",
    "battery",
    "24hour",
    "depth",
    "zoom",
    "panX",
    "panY",
    "shade",
    "widget0",
    "widget1",
    "widget2",
    "widget3",
    "focusText"
  };
  private final SharedPreferences prefs;

  public Preferences(Context context) {
    this(context, "luma");
  }

  Preferences(Context context, String name) {
    prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
  }

  public int wallpaper() {
    return Math.max(0, Math.min(WALLPAPERS.length - 1, prefs.getInt("wallpaper", 0)));
  }

  public void wallpaper(int index) {
    prefs
        .edit()
        .putInt("wallpaper", Math.max(0, Math.min(WALLPAPERS.length - 1, index)))
        .putBoolean("photo", false)
        .apply();
  }

  public int clock() {
    return Math.max(0, Math.min(CLOCKS.length - 1, prefs.getInt("clock", 0)));
  }

  public void clock(int index) {
    prefs.edit().putInt("clock", Math.max(0, Math.min(CLOCKS.length - 1, index))).apply();
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

  public int number(String key, int fallback) {
    return prefs.getInt(key, fallback);
  }

  public void setNumber(String key, int value) {
    prefs.edit().putInt(key, value).apply();
  }

  public String text(String key, String fallback) {
    return prefs.getString(key, fallback);
  }

  public void setText(String key, String value) {
    prefs.edit().putString(key, value).apply();
  }

  public int ink() {
    return INKS[Math.max(0, Math.min(INKS.length - 1, number("ink", 0)))];
  }

  public boolean favorite(int id) {
    return prefs
        .getStringSet("favorites", java.util.Collections.emptySet())
        .contains(String.valueOf(id));
  }

  public void favorite(int id, boolean value) {
    java.util.Set<String> ids =
        new java.util.HashSet<>(prefs.getStringSet("favorites", java.util.Collections.emptySet()));
    if (value) ids.add(String.valueOf(id));
    else ids.remove(String.valueOf(id));
    prefs.edit().putStringSet("favorites", ids).apply();
  }

  org.json.JSONObject design() {
    org.json.JSONObject result = new org.json.JSONObject();
    try {
      for (String key : DESIGN_KEYS)
        if (prefs.contains(key)) result.put(key, prefs.getAll().get(key));
    } catch (org.json.JSONException e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  void design(org.json.JSONObject value) {
    SharedPreferences.Editor e = prefs.edit();
    for (String key : DESIGN_KEYS) {
      e.remove(key);
      Object v = value.opt(key);
      if (v instanceof Boolean) e.putBoolean(key, (Boolean) v);
      else if (v instanceof Number) e.putInt(key, ((Number) v).intValue());
      else if (v instanceof String) e.putString(key, (String) v);
    }
    e.apply();
  }

  void copyDesign(Preferences source) {
    design(source.design());
  }
}
