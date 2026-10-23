package com.luma.lockscreen;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The same live face is used by the editor, full-screen preview and screen companion. */
public final class LockFace extends FrameLayout {
  public LockFace(Context context) {
    this(context, false);
  }

  private final Preferences prefs;
  private final boolean miniature;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final LinearLayout front, notifications;
  private final TextView date, battery;
  public final TextClock clock;
  public final WallpaperView wallpaper;
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          refreshData();
          handler.postDelayed(this, 15000);
        }
      };

  public LockFace(Context c, boolean small) {
    super(c);
    miniature = small;
    prefs = new Preferences(c);
    wallpaper = new WallpaperView(c, prefs.wallpaper());
    if (prefs.flag("photo", false)) wallpaper.loadPhoto();
    addView(wallpaper, new LayoutParams(-1, -1));
    front = Ui.column(c);
    front.setGravity(Gravity.CENTER_HORIZONTAL);
    front.setPadding(Ui.dp(c, 20), Ui.dp(c, small ? 25 : 108), Ui.dp(c, 20), Ui.dp(c, 16));
    addView(front, new LayoutParams(-1, -1));
    date = Ui.bold(c, "", small ? 12 : 19, Color.WHITE);
    date.setGravity(Gravity.CENTER);
    front.addView(date);
    clock =
        new TextClock(c) {
          @Override
          protected void onDraw(android.graphics.Canvas canvas) {
            if (prefs.clock() == 2) {
              Paint p = getPaint();
              p.setStyle(Paint.Style.STROKE);
              p.setStrokeWidth(Ui.dp(c, 1.3f));
              super.onDraw(canvas);
              p.setStyle(Paint.Style.FILL);
            } else super.onDraw(canvas);
          }
        };
    boolean h24 = prefs.flag("24hour", false);
    clock.setFormat12Hour(h24 ? "HH:mm" : "h:mm");
    clock.setFormat24Hour(h24 ? "HH:mm" : "h:mm");
    clock.setTextColor(Color.WHITE);
    clock.setTextSize(small ? 64 : 94);
    clock.setGravity(Gravity.CENTER);
    clock.setTypeface(
        Typeface.create(
            prefs.clock() == 1 ? "sans-serif-medium" : "sans-serif-thin", Typeface.NORMAL));
    clock.setShadowLayer(prefs.clock() == 0 ? 12 : 0, 0, 2, 0x55DCD0FF);
    clock.setLetterSpacing(-.065f);
    front.addView(clock, new LinearLayout.LayoutParams(-1, Ui.dp(c, small ? 85 : 124)));
    battery = Ui.bold(c, "", small ? 10 : 13, Color.WHITE);
    battery.setGravity(Gravity.CENTER);
    battery.setPadding(Ui.dp(c, 14), Ui.dp(c, 8), Ui.dp(c, 14), Ui.dp(c, 8));
    battery.setBackground(Ui.bordered(0x28FFFFFF, 0x33FFFFFF, 24, c));
    front.addView(battery, new LinearLayout.LayoutParams(-2, -2));
    ViewSpacer.add(front);
    notifications = Ui.column(c);
    front.addView(notifications, new LinearLayout.LayoutParams(-1, -2));
    TextView hint =
        Ui.text(c, small ? "TAP TO PREVIEW" : "Swipe up to unlock", small ? 9 : 13, 0xDDFFFFFF);
    hint.setGravity(Gravity.CENTER);
    hint.setLetterSpacing(small ? .17f : .02f);
    front.addView(hint);
    refreshData();
    setContentDescription("Live lock screen with " + prefs.wallpaperName() + " wallpaper");
  }

  void refreshData() {
    date.setText(
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())));
    date.setVisibility(prefs.flag("date", true) ? VISIBLE : INVISIBLE);
    Intent b = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int value =
        b == null
            ? -1
            : Math.round(
                100f
                    * b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    / Math.max(1, b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)));
    battery.setText(value < 0 ? "BATTERY" : "◉  " + value + "%  ·  Battery");
    battery.setVisibility(prefs.flag("battery", true) ? VISIBLE : GONE);
    notifications.removeAllViews();
    if (prefs.flag("notifications", false) && !miniature) {
      for (LumaNotificationListener.Entry e : LumaNotificationListener.entries()) {
        LinearLayout card = Ui.column(getContext());
        card.setPadding(
            Ui.dp(getContext(), 16),
            Ui.dp(getContext(), 12),
            Ui.dp(getContext(), 16),
            Ui.dp(getContext(), 12));
        card.setBackground(Ui.bordered(0x38FFFFFF, 0x40FFFFFF, 20, getContext()));
        card.addView(Ui.bold(getContext(), e.app, 13, Color.WHITE));
        KeyguardManager kg = getContext().getSystemService(KeyguardManager.class);
        boolean privateContent = kg != null && kg.isDeviceLocked();
        String content =
            prefs.flag("details", false) && !privateContent ? e.title : "New notification";
        TextView body = Ui.text(getContext(), content, 13, 0xEFFFFFFF);
        body.setMaxLines(2);
        card.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Ui.dp(getContext(), 8);
        notifications.addView(card, lp);
      }
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    handler.post(refresh);
  }

  @Override
  protected void onDetachedFromWindow() {
    handler.removeCallbacks(refresh);
    super.onDetachedFromWindow();
  }

  private static final class ViewSpacer {
    static void add(LinearLayout l) {
      l.addView(new android.view.View(l.getContext()), new LinearLayout.LayoutParams(1, 0, 1));
    }
  }
}
