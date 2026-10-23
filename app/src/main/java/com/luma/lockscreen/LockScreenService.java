package com.luma.lockscreen;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** User-controlled wake-screen companion; never disables the secure system keyguard. */
public final class LockScreenService extends Service {
  private static volatile boolean activeOrStarting;

  public static boolean isActiveOrStarting() {
    return activeOrStarting;
  }

  public static void requestStart(Context context) {
    activeOrStarting = true;
    try {
      context.startForegroundService(new Intent(context, LockScreenService.class));
    } catch (RuntimeException e) {
      activeOrStarting = false;
      throw e;
    }
  }

  public static final String STOP = "com.luma.lockscreen.STOP";
  private WindowManager manager;
  private FrameLayout overlay;
  private boolean registered;
  private final BroadcastReceiver screen =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) showCompanion();
          if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) removeOverlay();
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    activeOrStarting = true;
    manager = getSystemService(WindowManager.class);
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(
        new NotificationChannel(
            "companion", "Screen companion", NotificationManager.IMPORTANCE_LOW));
    Intent open = new Intent(this, MainActivity.class);
    PendingIntent app = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
    PendingIntent stop =
        PendingIntent.getService(
            this,
            1,
            new Intent(this, LockScreenService.class).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE);
    Notification n =
        new Notification.Builder(this, "companion")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Luma is enabled")
            .setContentText("Your screen, a little more you.")
            .setContentIntent(app)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "Turn off", stop).build())
            .build();
    startForeground(7, n);
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(screen, filter, Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(screen, filter);
    registered = true;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int id) {
    if (intent != null && STOP.equals(intent.getAction())) {
      new Preferences(this).setFlag("enabled", false);
      stopSelf();
      return START_NOT_STICKY;
    }
    if (!new Preferences(this).flag("enabled", false) || !Settings.canDrawOverlays(this)) {
      stopSelf();
      return START_NOT_STICKY;
    }
    return START_STICKY;
  }

  private void showCompanion() {
    if (!new Preferences(this).flag("enabled", false)
        || !Settings.canDrawOverlays(this)
        || overlay != null) return;
    overlay = new SwipeFrameLayout(this, this::removeOverlay);
    overlay.addView(new LockFace(this, false), new FrameLayout.LayoutParams(-1, -1));
    TextView close = Ui.button(this, "⌃   Continue", false, this::removeOverlay);
    close.setBackground(Ui.bordered(0x33FFFFFF, 0x66FFFFFF, 28, this));
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 190), Ui.dp(this, 54), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    lp.bottomMargin = Ui.dp(this, 45);
    overlay.addView(close, lp);
    TextView exit = Ui.button(this, "×  Close", false, this::removeOverlay);
    FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
    ep.topMargin = Ui.dp(this, 32);
    ep.rightMargin = Ui.dp(this, 16);
    overlay.addView(exit, ep);
    WindowManager.LayoutParams window =
        new WindowManager.LayoutParams(
            -1,
            -1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    try {
      manager.addView(overlay, window);
    } catch (RuntimeException denied) {
      overlay = null;
      new Preferences(this).setFlag("enabled", false);
      stopSelf();
    }
  }

  private void removeOverlay() {
    if (overlay != null) {
      try {
        manager.removeView(overlay);
      } catch (IllegalArgumentException ignored) {
      }
      overlay = null;
    }
  }

  @Override
  public void onDestroy() {
    activeOrStarting = false;
    removeOverlay();
    if (registered) unregisterReceiver(screen);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
