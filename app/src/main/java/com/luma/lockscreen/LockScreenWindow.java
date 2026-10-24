package com.luma.lockscreen;

import android.os.Build;
import android.view.*;

/** Full-bleed content with transient system navigation; never disables Android keyguard. */
final class LockScreenWindow {
  static void immersive(View view) {
    view.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    if (Build.VERSION.SDK_INT >= 30 && view.getWindowInsetsController() != null) {
      WindowInsetsController controller = view.getWindowInsetsController();
      controller.setSystemBarsBehavior(
          WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
      controller.hide(WindowInsets.Type.systemBars());
    }
  }

  static void configure(Window window) {
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
    window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    if (Build.VERSION.SDK_INT >= 29) {
      window.setStatusBarContrastEnforced(false);
      window.setNavigationBarContrastEnforced(false);
    }
    if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
    if (Build.VERSION.SDK_INT >= 28) {
      WindowManager.LayoutParams p = window.getAttributes();
      p.layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      window.setAttributes(p);
    }
    immersive(window.getDecorView());
  }
}
