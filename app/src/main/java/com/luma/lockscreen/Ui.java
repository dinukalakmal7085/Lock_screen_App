package com.luma.lockscreen;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
  static void systemInsets(View view) {
    if (android.os.Build.VERSION.SDK_INT >= 30)
      view.setOnApplyWindowInsetsListener(
          (v, insets) -> {
            android.graphics.Insets bars =
                insets.getInsets(android.view.WindowInsets.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
          });
    else view.setFitsSystemWindows(true);
  }

  static final int BG = Color.rgb(14, 14, 21),
      PANEL = Color.rgb(26, 26, 37),
      TEXT = Color.rgb(247, 245, 255),
      MUTED = Color.rgb(155, 153, 174),
      ACCENT = Color.rgb(201, 181, 255);

  static int dp(Context c, float n) {
    return (int) (n * c.getResources().getDisplayMetrics().density + 0.5f);
  }

  static GradientDrawable rounded(int color, float radius, Context c) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(c, radius));
    return d;
  }

  static GradientDrawable bordered(int color, int stroke, float radius, Context c) {
    GradientDrawable d = rounded(color, radius, c);
    d.setStroke(dp(c, 1), stroke);
    return d;
  }

  static void clickable(View v, int color, float radius) {
    v.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(0x22FFFFFF),
            rounded(color, radius, v.getContext()),
            rounded(Color.WHITE, radius, v.getContext())));
    v.setClickable(true);
    v.setFocusable(true);
  }

  static TextView text(Context c, String value, float size, int color) {
    TextView v = new TextView(c);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setFontFeatureSettings("kern");
    return v;
  }

  static TextView bold(Context c, String s, float size, int color) {
    TextView t = text(c, s, size, color);
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    return t;
  }

  static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  static LinearLayout row(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  static void space(LinearLayout l, int size) {
    View v = new View(l.getContext());
    l.addView(v, new LinearLayout.LayoutParams(1, dp(l.getContext(), size)));
  }

  static TextView button(Context c, String label, boolean primary, Runnable action) {
    TextView v = bold(c, label, 15, primary ? BG : TEXT);
    v.setGravity(Gravity.CENTER);
    v.setMinHeight(dp(c, 52));
    v.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
    clickable(v, primary ? ACCENT : PANEL, 18);
    v.setOnClickListener(w -> action.run());
    return v;
  }
}
