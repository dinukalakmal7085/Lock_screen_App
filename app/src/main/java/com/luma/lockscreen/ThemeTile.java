package com.luma.lockscreen;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;

/** A gallery sample uses the same live glass renderer and depth boundary as the real face. */
final class ThemeTile extends FrameLayout {
  public ThemeTile(Context c) {
    this(c, 32);
  }

  ThemeTile(Context c, int id) {
    super(c);
    Preferences sample = new Preferences(c, "luma_catalog_" + id);
    ThemeCatalog.apply(sample, id);
    WallpaperView wallpaper = new WallpaperView(c, id).config(sample);
    addView(wallpaper, new LayoutParams(-1, -1));
    GlassClock clock = new GlassClock(c, sample, wallpaper);
    clock.setFormat12Hour("'09:41'");
    clock.setFormat24Hour("'09:41'");
    LayoutParams time = new LayoutParams(-1, Ui.dp(c, 126), Gravity.TOP);
    time.topMargin = Ui.dp(c, 24);
    time.leftMargin = Ui.dp(c, 5);
    time.rightMargin = Ui.dp(c, 5);
    addView(clock, time);
    View foreground =
        new View(c) {
          @Override
          protected void onDraw(android.graphics.Canvas canvas) {
            wallpaper.drawForeground(canvas, getWidth(), getHeight());
          }
        };
    addView(foreground, new LayoutParams(-1, -1));
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  }
}
