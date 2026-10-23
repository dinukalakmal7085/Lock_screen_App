package com.luma.lockscreen;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Live clock, depth layer, up to four widgets and privacy-aware notification cards. */
public final class LockFace extends FrameLayout {
  public static final String[] WIDGETS = {"None", "Battery", "Calendar", "Alarm", "Focus"};
  private final Preferences prefs;
  private final boolean miniature;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final LinearLayout widgets, notifications;
  private final TextView date;
  public final TextClock clock;
  public final WallpaperView wallpaper;
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          refreshData();
          handler.postDelayed(this, 15000);
        }
      };

  public LockFace(Context context) {
    this(context, false);
  }

  public LockFace(Context context, boolean small) {
    this(context, small, new Preferences(context));
  }

  LockFace(Context c, boolean small, Preferences preferences) {
    super(c);
    prefs = preferences;
    miniature = small;
    wallpaper = new WallpaperView(c, prefs.wallpaper()).config(prefs);
    if (prefs.flag("photo", false)) wallpaper.loadPhoto();
    addView(wallpaper, new LayoutParams(-1, -1));
    LinearLayout top = Ui.column(c);
    int alignment = prefs.number("alignment", 1);
    int gravity = alignment == 0 ? Gravity.START : alignment == 2 ? Gravity.END : Gravity.CENTER;
    top.setGravity(gravity);
    int offset = Math.max(0, Math.min(100, prefs.number("clockOffset", 0)));
    int topPadding = small ? 24 + offset / 3 : 78 + offset;
    top.setPadding(Ui.dp(c, 18), Ui.dp(c, topPadding), Ui.dp(c, 18), 0);
    addView(top, new LayoutParams(-1, -2));
    date = Ui.bold(c, "", small ? 12 : 18, prefs.ink());
    date.setGravity(gravity);
    date.setShadowLayer(8, 0, 1, 0x44000000);
    top.addView(date, new LinearLayout.LayoutParams(-1, -2));
    clock = new GlassClock(c, prefs, wallpaper);
    boolean h24 = prefs.flag("24hour", false);
    String format = prefs.clock() == 3 ? (h24 ? "HH\nmm" : "h\nmm") : (h24 ? "HH:mm" : "h:mm");
    clock.setFormat12Hour(format);
    clock.setFormat24Hour(format);
    clock.setTextColor(prefs.ink());
    clock.setGravity(gravity | Gravity.CENTER_VERTICAL);
    clock.setIncludeFontPadding(false);
    clock.setLetterSpacing(-.045f);
    String font =
        prefs.clock() == 0
            ? "sans-serif-black"
            : prefs.clock() == 1
                ? "sans-serif-medium"
                : prefs.clock() == 4
                    ? "serif"
                    : prefs.clock() == 5 ? "monospace" : "sans-serif-thin";
    clock.setTypeface(Typeface.create(font, Typeface.NORMAL));
    int size = Math.max(54, Math.min(126, prefs.number("clockSize", 94)));
    float displaySize = small ? size * .68f : size;
    clock.setAutoSizeTextTypeUniformWithConfiguration(
        24, Math.round(displaySize), 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    int clockHeight = prefs.clock() == 3 ? (small ? 145 : 220) : (small ? 85 : 132);
    top.addView(clock, new LinearLayout.LayoutParams(-1, Ui.dp(c, clockHeight)));
    View foreground =
        new View(c) {
          @Override
          protected void onDraw(android.graphics.Canvas canvas) {
            wallpaper.drawForeground(canvas, getWidth(), getHeight());
          }
        };
    foreground.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    addView(foreground, new LayoutParams(-1, -1));
    widgets = Ui.row(c);
    widgets.setGravity(Gravity.CENTER);
    LayoutParams wp = new LayoutParams(-1, -2, Gravity.TOP);
    wp.topMargin = Ui.dp(c, topPadding + (small ? 18 : 28) + clockHeight + 8);
    wp.leftMargin = Ui.dp(c, 20);
    wp.rightMargin = Ui.dp(c, 20);
    addView(widgets, wp);
    notifications = Ui.column(c);
    LayoutParams np = new LayoutParams(-1, -2, Gravity.BOTTOM);
    np.setMargins(Ui.dp(c, 18), 0, Ui.dp(c, 18), Ui.dp(c, small ? 44 : 124));
    addView(notifications, np);
    TextView hint =
        Ui.text(c, small ? "LIVE PREVIEW" : "Swipe up to unlock", small ? 9 : 13, 0xEEFFFFFF);
    hint.setGravity(Gravity.CENTER);
    hint.setLetterSpacing(small ? .12f : .02f);
    LayoutParams hp = new LayoutParams(-1, -2, Gravity.BOTTOM);
    hp.bottomMargin = Ui.dp(c, small ? 16 : 22);
    addView(hint, hp);
    refreshData();
    setContentDescription("Live lock screen with " + prefs.wallpaperName() + " wallpaper");
  }

  void refreshData() {
    String[] formats = {"EEEE, MMMM d", "EEE, d MMM", "MMMM d", "EEEE"};
    date.setText(
        LocalDate.now()
            .format(
                DateTimeFormatter.ofPattern(
                    formats[Math.max(0, Math.min(3, prefs.number("dateFormat", 0)))],
                    Locale.getDefault())));
    date.setVisibility(prefs.flag("date", true) ? VISIBLE : INVISIBLE);
    widgets.removeAllViews();
    Intent b = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int battery =
        b == null
            ? -1
            : Math.round(
                100f
                    * b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    / Math.max(1, b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)));
    for (int i = 0; i < 4; i++) {
      int type = Math.max(0, Math.min(4, prefs.number("widget" + i, i == 0 ? 1 : i == 1 ? 2 : 0)));
      if (type == 0 || (type == 1 && !prefs.flag("battery", true))) continue;
      String value = "", label = "";
      switch (type) {
        case 1:
          value = battery < 0 ? "—" : battery + "%";
          label = "BATTERY";
          break;
        case 2:
          value = String.valueOf(LocalDate.now().getDayOfMonth());
          label =
              LocalDate.now()
                  .getDayOfWeek()
                  .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                  .toUpperCase(Locale.getDefault());
          break;
        case 3:
          AlarmManager.AlarmClockInfo alarm =
              getContext().getSystemService(AlarmManager.class).getNextAlarmClock();
          value =
              alarm == null
                  ? "—"
                  : android.text.format.DateFormat.format(
                          prefs.flag("24hour", false) ? "HH:mm" : "h:mm", alarm.getTriggerTime())
                      .toString();
          label = "ALARM";
          break;
        case 4:
          value = "✦";
          label = prefs.text("focusText", "BE PRESENT").toUpperCase(Locale.getDefault());
          break;
        default:
          break;
      }
      LinearLayout chip = Ui.column(getContext());
      chip.setGravity(Gravity.CENTER);
      chip.setPadding(
          Ui.dp(getContext(), 6),
          Ui.dp(getContext(), 7),
          Ui.dp(getContext(), 6),
          Ui.dp(getContext(), 7));
      chip.setBackground(Ui.bordered(0x20FFFFFF, 0x60FFFFFF, miniature ? 18 : 24, getContext()));
      TextView number = Ui.bold(getContext(), value, miniature ? 14 : 19, prefs.ink());
      number.setGravity(Gravity.CENTER);
      number.setMaxLines(1);
      number.setAutoSizeTextTypeUniformWithConfiguration(
          10, miniature ? 14 : 19, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
      chip.addView(number);
      TextView caption = Ui.bold(getContext(), label, miniature ? 7 : 9, prefs.ink());
      caption.setGravity(Gravity.CENTER);
      caption.setMaxLines(1);
      caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
      chip.addView(caption);
      chip.setContentDescription(label + " " + value);
      LinearLayout.LayoutParams cp =
          new LinearLayout.LayoutParams(
              Ui.dp(getContext(), miniature ? 56 : 66), Ui.dp(getContext(), miniature ? 56 : 66));
      cp.setMargins(Ui.dp(getContext(), 3), 0, Ui.dp(getContext(), 3), 0);
      widgets.addView(chip, cp);
    }
    notifications.removeAllViews();
    if (EditionFeatures.HAS_COMPANION && prefs.flag("notifications", false) && !miniature) {
      for (NotificationSummary e : EditionFeatures.summaries()) {
        LinearLayout card = Ui.column(getContext());
        card.setPadding(
            Ui.dp(getContext(), 16),
            Ui.dp(getContext(), 12),
            Ui.dp(getContext(), 16),
            Ui.dp(getContext(), 12));
        card.setBackground(Ui.bordered(0x54FFFFFF, 0x55FFFFFF, 24, getContext()));
        TextView app = Ui.bold(getContext(), e.app, 12, Color.WHITE);
        card.addView(app);
        KeyguardManager kg = getContext().getSystemService(KeyguardManager.class);
        boolean locked = kg == null || kg.isDeviceLocked();
        TextView content =
            Ui.text(
                getContext(),
                prefs.flag("details", false) && !locked ? e.title : "New notification",
                14,
                Color.WHITE);
        content.setMaxLines(2);
        card.addView(content);
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
}
