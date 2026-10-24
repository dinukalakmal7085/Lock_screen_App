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
  private LockScreenLayout overlay, prepared;
  private String preparedDesign;
  private boolean registered, allowForWake;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable show = this::showIfLocked;
  private final Runnable prepare = this::prepareNextWake;
  private final BroadcastReceiver screen =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
            allowForWake = true;
            handler.removeCallbacks(show);
            showIfLocked();
            // Keyguard may become locked just after SCREEN_ON; the first attempt has no delay.
            handler.postDelayed(show, 80);
            handler.postDelayed(show, 240);
            handler.postDelayed(show, 650);
          } else {
            allowForWake = false;
            hide();
            handler.removeCallbacks(show);
            handler.removeCallbacks(prepare);
            prepared = null;
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) handler.post(prepare);
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
    if (!(c instanceof android.app.Activity))
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
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
    handler.post(prepare);
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
      handler.removeCallbacks(show);
    } else if (!getSystemService(KeyguardManager.class).isKeyguardLocked()) hide();
    else if (allowForWake) showIfLocked();
  }

  private void showIfLocked() {
    if (!allowForWake
        || overlay != null
        || !new Preferences(this).flag("enabled", false)
        || !getSystemService(PowerManager.class).isInteractive()
        || !getSystemService(KeyguardManager.class).isKeyguardLocked()) return;
    String design = new Preferences(this).design().toString();
    overlay = prepared != null && design.equals(preparedDesign) ? prepared : createLayout();
    prepared = null;
    // Prepared cards may have been built before keyguard locked; redact before attaching.
    overlay.face.refreshData();
    LockScreenWindow.immersive(overlay);
    WindowManager.LayoutParams params =
        new WindowManager.LayoutParams(
            -1,
            -1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT);
    if (Build.VERSION.SDK_INT >= 28)
      params.layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
    if (Build.VERSION.SDK_INT >= 30) params.setFitInsetsTypes(0);
    try {
      windows.addView(overlay, params);
      LockScreenWindow.immersive(overlay);
    } catch (RuntimeException denied) {
      overlay = null;
      allowForWake = false;
    }
  }

  private LockScreenLayout createLayout() {
    return new LockScreenLayout(
        this,
        false,
        () -> authenticate(null),
        () -> authenticate("camera"),
        () -> Toast.makeText(this, EditionFeatures.flashlight(this), Toast.LENGTH_SHORT).show(),
        () -> authenticate("switcher"),
        () -> {
          allowForWake = false;
          handler.removeCallbacks(show);
          hide();
        });
  }

  private void prepareNextWake() {
    if (overlay != null || !new Preferences(this).flag("enabled", false)) return;
    preparedDesign = new Preferences(this).design().toString();
    prepared = createLayout();
    android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
    int width = metrics.widthPixels, height = metrics.heightPixels;
    if (Build.VERSION.SDK_INT >= 30) {
      android.graphics.Rect bounds = windows.getMaximumWindowMetrics().getBounds();
      width = bounds.width();
      height = bounds.height();
    }
    prepared.prepare(Math.max(1, width), Math.max(1, height));
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration config) {
    super.onConfigurationChanged(config);
    prepared = null;
    hide();
    if (allowForWake) showIfLocked();
  }

  private void authenticate(String destination) {
    allowForWake = false;
    hide();
    Intent intent =
        new Intent(this, LockScreenActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION)
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
    prepared = null;
    hide();
    EditionFeatures.stopFlashlight(this);
    if (registered) unregisterReceiver(screen);
    connectedRef.clear();
    super.onDestroy();
  }
}
