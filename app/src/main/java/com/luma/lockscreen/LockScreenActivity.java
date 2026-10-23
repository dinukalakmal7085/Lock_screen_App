package com.luma.lockscreen;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public final class LockScreenActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (android.os.Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
    else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
    getWindow().setNavigationBarColor(0xFF11101C);
    FrameLayout root = new SwipeFrameLayout(this, this::unlock);
    root.addView(new LockFace(this, false), new FrameLayout.LayoutParams(-1, -1));
    TextView close = Ui.button(this, "×  Close preview", false, this::finish);
    close.setAlpha(.85f);
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
    cp.setMargins(0, Ui.dp(this, 30), Ui.dp(this, 16), 0);
    root.addView(close, cp);
    TextView unlock = Ui.button(this, "⌃   Open device", false, this::unlock);
    unlock.setBackground(Ui.bordered(0x25FFFFFF, 0x55FFFFFF, 28, this));
    FrameLayout.LayoutParams up =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 190), Ui.dp(this, 54), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    up.bottomMargin = Ui.dp(this, 42);
    root.addView(unlock, up);
    setContentView(root);
  }

  private void unlock() {
    KeyguardManager keyguard = getSystemService(KeyguardManager.class);
    if (keyguard != null && keyguard.isKeyguardLocked())
      keyguard.requestDismissKeyguard(
          this,
          new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
              finish();
            }
          });
    else finish();
  }
}
