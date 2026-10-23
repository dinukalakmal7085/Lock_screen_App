package com.luma.lockscreen;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;

/** Opt-in accessibility overlay. No window-content retrieval, gestures or input injection. */
public final class LockScreenService extends AccessibilityService {
  public static final String STOP = "com.luma.lockscreen.STOP";
  private static java.lang.ref.WeakReference<LockScreenService> connectedRef =
      new java.lang.ref.WeakReference<>(null);
  private WindowManager windows;
  private FrameLayout overlay;
  private boolean registered, allowForWake;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable show = this::showIfLocked;
  private final BroadcastReceiver screen =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
            allowForWake = true;
            handler.postDelayed(show, 180);
            handler.postDelayed(show, 650);
          } else {
            allowForWake = false;
            hide();
            handler.removeCallbacks(show);
          }
        }
      };

  public static boolean isActiveOrStarting() {
    return connectedRef.get() != null;
  }

  public static void stop(Context c) {
    new Preferences(c).setFlag("enabled", false);
    LockScreenService connected = connectedRef.get();
    if (connected != null) {
      connected.hide();
      connected.disableSelf();
    }
  }

  public static void requestStart(Context c) {
    Intent i = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
    if (!(c instanceof android.app.Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    c.startActivity(i);
  }

  @Override
  public void onServiceConnected() {
    super.onServiceConnected();
    connectedRef = new java.lang.ref.WeakReference<>(this);
    windows = getSystemService(WindowManager.class);
    new Preferences(this).setFlag("enabled", true);
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_SCREEN_ON);
    f.addAction(Intent.ACTION_SCREEN_OFF);
    f.addAction(Intent.ACTION_USER_PRESENT);
    if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, f, Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(screen, f);
    registered = true;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    // Only transient window-class metadata is inspected, to keep system prompts and calls visible.
    CharSequence type = event.getClassName();
    String name = type == null ? "" : type.toString().toLowerCase(java.util.Locale.ROOT);
    if (name.contains("credential")
        || name.contains("bouncer")
        || name.contains("password")
        || name.contains("permission")
        || name.contains("biometric")
        || name.contains("emergency")
        || name.contains("incall")
        || name.contains("globalactions")) {
      allowForWake = false;
      hide();
    } else if (!getSystemService(KeyguardManager.class).isKeyguardLocked()) hide();
  }

  private void showIfLocked() {
    if (!allowForWake
        || overlay != null
        || !new Preferences(this).flag("enabled", false)
        || !getSystemService(PowerManager.class).isInteractive()
        || !getSystemService(KeyguardManager.class).isKeyguardLocked()) return;
    overlay = new SwipeFrameLayout(this, () -> authenticate(null));
    overlay.setOnLongClickListener(
        v -> {
          authenticate("switcher");
          return true;
        });
    overlay.addView(new LockFace(this, false), new FrameLayout.LayoutParams(-1, -1));
    TextView open = Ui.button(this, "⌃   Open device", false, () -> authenticate(null));
    open.setBackground(Ui.bordered(0x25FFFFFF, 0x60FFFFFF, 28, this));
    FrameLayout.LayoutParams op =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 190), Ui.dp(this, 54), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    op.bottomMargin = Ui.dp(this, 50);
    overlay.addView(open, op);
    TextView camera = Ui.button(this, "◉", false, () -> authenticate("camera"));
    camera.setContentDescription("Open camera after unlocking");
    camera.setBackground(Ui.bordered(0x44000000, 0x30FFFFFF, 26, this));
    FrameLayout.LayoutParams cp =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 52), Ui.dp(this, 52), Gravity.BOTTOM | Gravity.END);
    cp.setMargins(0, 0, Ui.dp(this, 24), Ui.dp(this, 54));
    overlay.addView(camera, cp);
    TextView torch =
        Ui.button(
            this,
            "ϟ",
            false,
            () ->
                Toast.makeText(this, EditionFeatures.flashlight(this), Toast.LENGTH_SHORT).show());
    torch.setContentDescription("Toggle flashlight");
    torch.setBackground(Ui.bordered(0x44000000, 0x30FFFFFF, 26, this));
    FrameLayout.LayoutParams tp =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 52), Ui.dp(this, 52), Gravity.BOTTOM | Gravity.START);
    tp.setMargins(Ui.dp(this, 24), 0, 0, Ui.dp(this, 54));
    overlay.addView(torch, tp);
    TextView close =
        Ui.button(
            this,
            "×",
            false,
            () -> {
              allowForWake = false;
              hide();
            });
    close.setContentDescription("Close Luma lock screen");
    close.setBackground(Ui.bordered(0x28000000, 0x25FFFFFF, 24, this));
    FrameLayout.LayoutParams xp =
        new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.TOP | Gravity.END);
    xp.topMargin = Ui.dp(this, 35);
    xp.rightMargin = Ui.dp(this, 16);
    overlay.addView(close, xp);
    WindowManager.LayoutParams params =
        new WindowManager.LayoutParams(
            -1,
            -1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    try {
      windows.addView(overlay, params);
    } catch (RuntimeException denied) {
      overlay = null;
      allowForWake = false;
    }
  }

  private void authenticate(String destination) {
    allowForWake = false;
    hide();
    Intent intent =
        new Intent(this, LockScreenActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("authenticate", true);
    if (destination != null) intent.putExtra("destination", destination);
    startActivity(intent);
  }

  private void hide() {
    if (overlay != null) {
      try {
        windows.removeView(overlay);
      } catch (IllegalArgumentException ignored) {
      }
      overlay = null;
    }
  }

  @Override
  public void onInterrupt() {
    allowForWake = false;
    hide();
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    hide();
    EditionFeatures.stopFlashlight(this);
    if (registered) unregisterReceiver(screen);
    connectedRef.clear();
    super.onDestroy();
  }
}
