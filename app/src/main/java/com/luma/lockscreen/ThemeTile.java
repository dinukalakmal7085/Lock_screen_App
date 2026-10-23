package com.luma.lockscreen;

import android.content.Context;
import android.widget.FrameLayout;

/** A proportional sample of the complete preset, sharing full-screen composition and depth. */
final class ThemeTile extends FrameLayout {
  public ThemeTile(Context c) {
    this(c, 32);
  }

  ThemeTile(Context c, int id) {
    super(c);
    Preferences sample = new Preferences(c, "luma_catalog_" + id);
    sample.design(new org.json.JSONObject());
    ThemeCatalog.apply(sample, id);
    addView(new PortraitPreview(c, sample, true), new LayoutParams(-1, -1));
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  }
}
