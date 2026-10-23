package com.luma.lockscreen;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android editor. All preferences and imported artwork stay on this device. */
public final class MainActivity extends Activity {
  private Preferences prefs;
  private LinearLayout root, body;
  private ScrollView scroll;
  private int tab, restoreScroll;
  private boolean awaitingOverlay, applying;
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private static final int PHOTO = 10, NOTIFICATIONS = 11;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    prefs = new Preferences(this);
    if (!EditionFeatures.HAS_COMPANION) {
      prefs.setFlag("enabled", false);
      prefs.setFlag("notifications", false);
      prefs.setFlag("details", false);
    }
    if (saved != null) tab = saved.getInt("tab", 0);
    render();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("tab", tab);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (awaitingOverlay) {
      awaitingOverlay = false;
      if (Settings.canDrawOverlays(this)) enableCompanion();
      else toast("Display permission wasn't enabled. Preview still works.");
    }
    if (root != null) refresh();
  }

  private void refresh() {
    restoreScroll = scroll == null ? 0 : scroll.getScrollY();
    render();
  }

  private void switchTab(int selected) {
    tab = selected;
    restoreScroll = 0;
    render();
  }

  private void render() {
    root = Ui.column(this);
    root.setBackgroundColor(Ui.BG);
    if (Build.VERSION.SDK_INT >= 30)
      root.setOnApplyWindowInsetsListener(
          (v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
          });
    // Android 8–10 use the legacy inset API.
    if (Build.VERSION.SDK_INT < 30)
      root.setOnApplyWindowInsetsListener(
          (v, insets) -> {
            v.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
            return insets;
          });
    LinearLayout header = Ui.row(this);
    header.setPadding(dp(24), dp(14), dp(24), dp(12));
    TextView mark = Ui.text(this, "✦", 32, Ui.ACCENT);
    mark.setContentDescription("Luma logo");
    header.addView(mark);
    TextView logo = Ui.bold(this, "  Luma", 25, Ui.TEXT);
    header.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
    TextView badge = Ui.bold(this, "●  ON DEVICE", 9, Ui.MUTED);
    badge.setLetterSpacing(.12f);
    badge.setPadding(dp(11), dp(8), dp(11), dp(8));
    badge.setBackground(Ui.bordered(Ui.PANEL, 0xFF34313F, 20, this));
    header.addView(badge);
    root.addView(header);
    scroll = new ScrollView(this);
    scroll.setFillViewport(false);
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    body = Ui.column(this);
    body.setPadding(dp(24), dp(12), dp(24), dp(24));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (tab == 0) studio();
    else if (tab == 1) wallpapers();
    else settings();
    root.addView(navigation());
    setContentView(root);
    root.requestApplyInsets();
    scroll.post(() -> scroll.scrollTo(0, restoreScroll));
  }

  private void studio() {
    eyebrow("LOCK SCREEN STUDIO");
    title("A little more you.");
    subtitle("A fresh look. Every time you wake up.");
    gap(22);
    LinearLayout previewTitle = Ui.row(this);
    previewTitle.addView(
        Ui.bold(this, "YOUR LOCK SCREEN", 10, Ui.MUTED), new LinearLayout.LayoutParams(0, -2, 1));
    TextView live = Ui.bold(this, "●  LIVE", 9, 0xFFB2DDC5);
    previewTitle.addView(live);
    body.addView(previewTitle);
    gap(12);
    FrameLayout hero = new RoundedFrameLayout(this, 28);
    hero.setContentDescription("Open full-screen lock-screen preview");
    hero.setOnClickListener(v -> preview());
    hero.setBackground(Ui.rounded(Ui.PANEL, 28, this));
    hero.setClipToOutline(true);
    hero.addView(new LockFace(this, true), new FrameLayout.LayoutParams(-1, -1));
    TextView expand = Ui.bold(this, "⤢", 24, Color.WHITE);
    expand.setGravity(Gravity.CENTER);
    expand.setContentDescription("Open full-screen preview");
    Ui.clickable(expand, 0x28FFFFFF, 18);
    FrameLayout.LayoutParams ep =
        new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.BOTTOM | Gravity.END);
    ep.setMargins(0, 0, dp(14), dp(14));
    hero.addView(expand, ep);
    expand.setOnClickListener(v -> preview());
    TextView caption =
        Ui.bold(
            this,
            prefs.wallpaperName() + "  /  " + Preferences.CLOCKS[prefs.clock()],
            10,
            Color.WHITE);
    caption.setPadding(dp(12), dp(7), dp(12), dp(7));
    caption.setBackground(Ui.rounded(0x35FFFFFF, 20, this));
    FrameLayout.LayoutParams cp =
        new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
    cp.setMargins(dp(14), 0, 0, dp(14));
    hero.addView(caption, cp);
    body.addView(hero, new LinearLayout.LayoutParams(-1, dp(312)));
    gap(12);
    LinearLayout actions = Ui.row(this);
    weighted(actions, Ui.button(this, "Customize", true, this::customize), true);
    weighted(actions, Ui.button(this, "Full preview  ↗", false, this::preview), false);
    body.addView(actions);
    gap(26);
    section("Find your mood", "View all  →", () -> switchTab(1));
    gap(13);
    wallpaperStrip();
    gap(26);
    section("The finishing touches", null, null);
    gap(13);
    LinearLayout card = panel();
    card.addView(Ui.bold(this, "Clock style", 14, Ui.TEXT));
    Ui.space(card, 13);
    card.addView(clockChoices());
    Ui.space(card, 16);
    card.addView(toggleRow("Date", "A little context for your day", "date", true));
    card.addView(toggleRow("Battery", "A quick glance at your charge", "battery", true));
    body.addView(card);
    gap(20);
    body.addView(
        Ui.button(
            this, applying ? "Applying…" : "Apply lock wallpaper  ↗", true, this::applyWallpaper));
    gap(10);
    TextView info =
        Ui.text(
            this,
            EditionFeatures.HAS_COMPANION
                ? "Wallpaper applies to Android’s lock screen. Use the screen companion for Luma’s"
                    + " clock and widgets."
                : "Wallpaper applies to Android’s lock screen. Customize the clock and widgets in"
                    + " Preview; Android keeps its system clock.",
            11,
            Ui.MUTED);
    info.setGravity(Gravity.CENTER);
    info.setLineSpacing(dp(3), 1);
    body.addView(info);
  }

  private void wallpapers() {
    eyebrow("THE COLLECTION");
    title("Set the mood.");
    subtitle("Original artwork. A screen that feels like you.");
    gap(22);
    TextView importPhoto = Ui.button(this, "＋   Choose a photo", false, this::pickPhoto);
    body.addView(importPhoto);
    gap(22);
    for (int row = 0; row < 3; row++) {
      LinearLayout line = Ui.row(this);
      line.setGravity(Gravity.TOP);
      for (int col = 0; col < 2; col++) {
        final int index = row * 2 + col;
        LinearLayout tile = Ui.column(this);
        FrameLayout art = new RoundedFrameLayout(this, 22);
        art.setBackground(Ui.rounded(Ui.PANEL, 22, this));
        art.setClipToOutline(true);
        art.addView(new WallpaperView(this, index), new FrameLayout.LayoutParams(-1, -1));
        TextView sample = Ui.text(this, "9:41", 38, Color.WHITE);
        sample.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        sample.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        sample.setPadding(0, dp(30), 0, 0);
        art.addView(sample, new FrameLayout.LayoutParams(-1, -1));
        if (index == prefs.wallpaper() && !prefs.flag("photo", false)) {
          TextView check = Ui.bold(this, "✓", 15, Ui.BG);
          check.setGravity(Gravity.CENTER);
          check.setBackground(Ui.rounded(Ui.ACCENT, 20, this));
          FrameLayout.LayoutParams ck =
              new FrameLayout.LayoutParams(dp(27), dp(27), Gravity.BOTTOM | Gravity.END);
          ck.setMargins(0, 0, dp(10), dp(10));
          art.addView(check, ck);
        }
        art.setContentDescription("Select " + Preferences.WALLPAPERS[index] + " wallpaper");
        art.setOnClickListener(
            v -> {
              prefs.wallpaper(index);
              refresh();
              toast(Preferences.WALLPAPERS[index] + " selected");
            });
        art.setFocusable(true);
        tile.addView(art, new LinearLayout.LayoutParams(-1, dp(210)));
        Ui.space(tile, 10);
        tile.addView(Ui.bold(this, Preferences.WALLPAPERS[index], 15, Ui.TEXT));
        Ui.space(tile, 3);
        tile.addView(
            Ui.text(
                this,
                new String[] {
                      "Soft violet · Liquid",
                      "Rose glow · Liquid",
                      "Ocean air · Liquid",
                      "Warm light · Liquid",
                      "Quiet green · Liquid",
                      "Cool calm · Liquid"
                    }
                    [index],
                10,
                Ui.MUTED));
        weighted(line, tile, col == 0);
      }
      body.addView(line);
      gap(22);
    }
    body.addView(Ui.button(this, "Preview selected wallpaper  ↗", true, this::preview));
    gap(12);
    body.addView(Ui.button(this, "Apply lock wallpaper", false, this::applyWallpaper));
  }

  private void settings() {
    eyebrow("YOUR WAY");
    title("Make it personal.");
    subtitle("Little details. Thoughtfully yours.");
    gap(24);
    if (EditionFeatures.HAS_COMPANION) {
      LinearLayout companion = panel();
      companion.addView(Ui.bold(this, "✦   Screen companion", 18, Ui.TEXT));
      Ui.space(companion, 8);
      TextView help =
          Ui.text(
              this,
              "Show your Luma face when the screen wakes. Android’s secure lock stays in place;"
                  + " some phones show the companion only after unlocking.",
              13,
              Ui.MUTED);
      help.setLineSpacing(dp(4), 1);
      companion.addView(help);
      Ui.space(companion, 16);
      boolean enabled =
          prefs.flag("enabled", false)
              && EditionFeatures.isActive()
              && Settings.canDrawOverlays(this);
      companion.addView(
          Ui.button(
              this,
              enabled ? "Turn off companion" : "Enable companion",
              true,
              () -> {
                if (enabled) {
                  prefs.setFlag("enabled", false);
                  EditionFeatures.stop(this);
                  refresh();
                } else askCompanion();
              }));
      body.addView(companion);
    } else {
      LinearLayout edition = panel();
      edition.addView(Ui.bold(this, "✦   Luma Studio", 18, Ui.TEXT));
      Ui.space(edition, 8);
      TextView scope =
          Ui.text(
              this,
              "Create your wallpaper and explore clock styles in Preview. Your phone keeps its"
                  + " system clock and secure unlock.",
              13,
              Ui.MUTED);
      scope.setLineSpacing(dp(4), 1);
      edition.addView(scope);
      body.addView(edition);
    }
    gap(24);
    section("Display", null, null);
    gap(12);
    LinearLayout display = panel();
    display.addView(toggleRow("24-hour clock", "Use 14:30 instead of 2:30", "24hour", false));
    display.addView(toggleRow("Show date", "Day, month, and date", "date", true));
    display.addView(toggleRow("Battery widget", "Current charge, at a glance", "battery", true));
    body.addView(display);
    gap(24);
    if (EditionFeatures.HAS_COMPANION) {
      section("Notifications", null, null);
      gap(12);
      LinearLayout nc = panel();
      boolean access = notificationAccess();
      nc.addView(
          Ui.bold(
              this,
              access ? "Notification access is on" : "Your notifications, if you choose",
              14,
              Ui.TEXT));
      Ui.space(nc, 7);
      TextView note =
          Ui.text(
              this,
              "Optional access shows up to three recent notifications. Summaries stay in memory on"
                  + " your phone. Sensitive contents remain hidden while locked.",
              12,
              Ui.MUTED);
      note.setLineSpacing(dp(4), 1);
      nc.addView(note);
      Ui.space(nc, 12);
      nc.addView(
          Ui.button(
              this,
              access ? "Manage Android access  ↗" : "Allow notification access  ↗",
              false,
              this::requestNotificationAccess));
      Ui.space(nc, 10);
      nc.addView(
          toggleRow(
              "Show notifications",
              "Display recent notifications on your face",
              "notifications",
              false));
      nc.addView(
          toggleRow("Show public titles", "Only while your device is unlocked", "details", false));
      body.addView(nc);
      gap(24);
    }
    section("About Luma", null, null);
    gap(12);
    LinearLayout about = panel();
    about.addView(Ui.bold(this, "Made for a quieter screen.", 16, Ui.TEXT));
    Ui.space(about, 8);
    TextView privacy =
        Ui.text(
            this,
            "No account. No ads. No tracking.\n"
                + "Your photos and preferences stay on your device.\n\n"
                + (EditionFeatures.HAS_COMPANION
                    ? "Luma never replaces your PIN or biometrics. The companion needs"
                        + " display-over-other-apps access and a visible ongoing notification."
                        + " Android and manufacturer battery settings can limit it. Enable it"
                        + " again after restarting your phone."
                    : "Luma Studio changes your wallpaper and previews your design. It has no"
                        + " notification reading, overlay access, or background service."),
            12,
            Ui.MUTED);
    privacy.setLineSpacing(dp(4), 1);
    about.addView(privacy);
    Ui.space(about, 16);
    about.addView(
        Ui.button(
            this,
            "Reset appearance",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Start fresh?")
                    .setMessage(
                        EditionFeatures.HAS_COMPANION
                            ? "Reset wallpaper, clock, and widgets. Your companion and permission"
                                + " settings will stay as they are."
                            : "Reset wallpaper, clock, and preview widgets?")
                    .setNegativeButton("Keep it", null)
                    .setPositiveButton(
                        "Reset",
                        (d, w) -> {
                          prefs.wallpaper(0);
                          prefs.clock(0);
                          prefs.setFlag("date", true);
                          prefs.setFlag("battery", true);
                          prefs.setFlag("24hour", false);
                          refresh();
                        })
                    .show()));
    body.addView(about);
    gap(16);
    TextView version = Ui.text(this, "LUMA  /  VERSION " + BuildConfig.VERSION_NAME, 10, Ui.MUTED);
    version.setLetterSpacing(.16f);
    version.setGravity(Gravity.CENTER);
    body.addView(version);
  }

  private LinearLayout navigation() {
    LinearLayout nav = Ui.row(this);
    nav.setPadding(dp(14), dp(9), dp(14), dp(9));
    nav.setBackground(Ui.bordered(Ui.BG, 0xFF252430, 0, this));
    String[] names = {"Studio", "Wallpapers", "Settings"};
    String[] symbols = {"✦", "▧", "⚙"};
    for (int i = 0; i < 3; i++) {
      final int index = i;
      LinearLayout item = Ui.column(this);
      item.setGravity(Gravity.CENTER);
      Ui.clickable(item, i == tab ? 0xFF272236 : Ui.BG, 18);
      item.setPadding(0, dp(7), 0, dp(7));
      TextView icon = Ui.text(this, symbols[i], 22, i == tab ? Ui.ACCENT : Ui.MUTED);
      icon.setGravity(Gravity.CENTER);
      item.addView(icon);
      TextView label = Ui.bold(this, names[i], 10, i == tab ? Ui.ACCENT : Ui.MUTED);
      label.setGravity(Gravity.CENTER);
      item.addView(label);
      item.setContentDescription(names[i] + " tab");
      item.setSelected(i == tab);
      item.setOnClickListener(v -> switchTab(index));
      nav.addView(item, new LinearLayout.LayoutParams(0, dp(62), 1));
    }
    return nav;
  }

  private void wallpaperStrip() {
    HorizontalScrollView hsv = new HorizontalScrollView(this);
    hsv.setHorizontalScrollBarEnabled(false);
    LinearLayout row = Ui.row(this);
    for (int i = 0; i < 6; i++) {
      final int index = i;
      LinearLayout tile = Ui.column(this);
      WallpaperView art = new WallpaperView(this, i);
      art.setBackground(Ui.rounded(Ui.PANEL, 16, this));
      art.setClipToOutline(true);
      art.setContentDescription("Select " + Preferences.WALLPAPERS[i] + " wallpaper");
      art.setOnClickListener(
          v -> {
            prefs.wallpaper(index);
            refresh();
          });
      art.setFocusable(true);
      tile.addView(art, new LinearLayout.LayoutParams(dp(90), dp(112)));
      Ui.space(tile, 7);
      TextView label =
          Ui.bold(
              this,
              Preferences.WALLPAPERS[i]
                  + (i == prefs.wallpaper() && !prefs.flag("photo", false) ? "  ✓" : ""),
              11,
              i == prefs.wallpaper() && !prefs.flag("photo", false) ? Ui.ACCENT : Ui.MUTED);
      tile.addView(label);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
      lp.rightMargin = dp(10);
      row.addView(tile, lp);
    }
    hsv.addView(row);
    body.addView(hsv);
  }

  private LinearLayout clockChoices() {
    LinearLayout choices = Ui.row(this);
    for (int i = 0; i < 3; i++) {
      final int index = i;
      TextView button =
          Ui.bold(this, Preferences.CLOCKS[i], 12, i == prefs.clock() ? Ui.ACCENT : Ui.MUTED);
      button.setGravity(Gravity.CENTER);
      button.setPadding(dp(6), dp(12), dp(6), dp(12));
      Ui.clickable(button, i == prefs.clock() ? 0xFF373047 : 0xFF22222F, 12);
      button.setContentDescription(Preferences.CLOCKS[i] + " clock style");
      button.setSelected(i == prefs.clock());
      button.setOnClickListener(
          v -> {
            prefs.clock(index);
            refresh();
          });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      if (i < 2) lp.rightMargin = dp(6);
      choices.addView(button, lp);
    }
    return choices;
  }

  private LinearLayout toggleRow(String title, String description, String key, boolean fallback) {
    LinearLayout row = Ui.row(this);
    row.setPadding(0, dp(9), 0, dp(9));
    LinearLayout labels = Ui.column(this);
    labels.addView(Ui.bold(this, title, 14, Ui.TEXT));
    Ui.space(labels, 4);
    labels.addView(Ui.text(this, description, 11, Ui.MUTED));
    row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    Switch control = new Switch(this);
    control.setContentDescription(title);
    control.setChecked(prefs.flag(key, fallback));
    control.setThumbTintList(
        android.content.res.ColorStateList.valueOf(control.isChecked() ? Ui.ACCENT : 0xFFA4A0B3));
    control.setOnCheckedChangeListener(
        (v, checked) -> {
          if (key.equals("notifications") && checked && !notificationAccess()) {
            control.setChecked(false);
            requestNotificationAccess();
            return;
          }
          prefs.setFlag(key, checked);
          refresh();
        });
    row.addView(control);
    return row;
  }

  private void customize() {
    String[] items = {
      "Liquid clock",
      "Classic clock",
      "Outline clock",
      "Choose a wallpaper",
      "Import your photo",
      "Display & notification settings"
    };
    new AlertDialog.Builder(this)
        .setTitle("Make it yours")
        .setItems(
            items,
            (d, w) -> {
              if (w < 3) {
                prefs.clock(w);
                refresh();
              } else if (w == 3) switchTab(1);
              else if (w == 4) pickPhoto();
              else switchTab(2);
            })
        .setNegativeButton("Done", null)
        .show();
  }

  private void preview() {
    startActivity(new Intent(this, LockScreenActivity.class));
  }

  private void pickPhoto() {
    Intent intent =
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("image/*")
            .addCategory(Intent.CATEGORY_OPENABLE);
    try {
      startActivityForResult(intent, PHOTO);
    } catch (ActivityNotFoundException e) {
      toast("No photo picker is available on this device.");
    }
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (req != PHOTO || result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    toast("Importing photo…");
    io.execute(
        () -> {
          Bitmap bitmap = null;
          File temp = new File(getFilesDir(), "wallpaper.pending");
          try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
              BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth < 1 || bounds.outHeight < 1)
              throw new IOException("This image format couldn't be opened.");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 2400)
              options.inSampleSize *= 2;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
              bitmap = BitmapFactory.decodeStream(input, null, options);
            }
            if (bitmap == null) throw new IOException("Couldn't read this photo.");
            // Apply image orientation (including mirrored EXIF orientations) before saving.
            try (InputStream input = getContentResolver().openInputStream(uri)) {
              androidx.exifinterface.media.ExifInterface exif =
                  new androidx.exifinterface.media.ExifInterface(input);
              int orientation =
                  exif.getAttributeInt(
                      androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1);
              Matrix matrix = new Matrix();
              switch (orientation) {
                case 2:
                  matrix.setScale(-1, 1);
                  break;
                case 3:
                  matrix.setRotate(180);
                  break;
                case 4:
                  matrix.setScale(1, -1);
                  break;
                case 5:
                  matrix.setRotate(90);
                  matrix.postScale(-1, 1);
                  break;
                case 6:
                  matrix.setRotate(90);
                  break;
                case 7:
                  matrix.setRotate(-90);
                  matrix.postScale(-1, 1);
                  break;
                case 8:
                  matrix.setRotate(-90);
                  break;
                default:
                  break;
              }
              Bitmap rotated =
                  Bitmap.createBitmap(
                      bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
              if (rotated != bitmap) {
                bitmap.recycle();
                bitmap = rotated;
              }
            } catch (IOException ignored) {
              /* Formats without EXIF keep their decoded orientation. */
            }
            try (FileOutputStream out = new FileOutputStream(temp)) {
              if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out))
                throw new IOException("Couldn't save this photo.");
            }
            File target = new File(getFilesDir(), "wallpaper.jpg");
            if (!temp.renameTo(target)) throw new IOException("Couldn't save this photo.");
            prefs.setFlag("photo", true);
            runOnUiThread(
                () -> {
                  if (!isFinishing()) {
                    refresh();
                    toast("Your photo is ready.");
                  }
                });
          } catch (IOException | SecurityException | OutOfMemoryError e) {
            temp.delete();
            runOnUiThread(() -> toast("Couldn't import this photo. Try a smaller JPEG or PNG."));
          } finally {
            if (bitmap != null) bitmap.recycle();
          }
        });
  }

  private void applyWallpaper() {
    if (applying) return;
    new AlertDialog.Builder(this)
        .setTitle("Apply to your lock screen?")
        .setMessage(
            "This sets the wallpaper background. Android keeps its own clock and secure unlock."
                + (EditionFeatures.HAS_COMPANION
                    ? " Luma’s full design is available in Preview and the optional screen"
                        + " companion."
                    : " Luma’s clock and widgets are available in Preview."))
        .setNegativeButton("Cancel", null)
        .setPositiveButton(
            "Apply wallpaper",
            (d, w) -> {
              WallpaperManager wm = WallpaperManager.getInstance(this);
              if (!wm.isWallpaperSupported() || !wm.isSetWallpaperAllowed()) {
                toast("This device doesn't allow wallpaper changes.");
                return;
              }
              applying = true;
              refresh();
              WallpaperView art = new WallpaperView(this, prefs.wallpaper());
              if (prefs.flag("photo", false)) art.loadPhoto();
              android.util.DisplayMetrics display = getResources().getDisplayMetrics();
              int width = Math.min(1440, display.widthPixels),
                  height = Math.round(width * (display.heightPixels / (float) display.widthPixels));
              io.execute(
                  () -> {
                    Bitmap bitmap = null;
                    try {
                      bitmap = art.bitmap(width, height);
                      wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                      runOnUiThread(() -> toast("Lock-screen wallpaper applied."));
                    } catch (IOException | SecurityException | OutOfMemoryError e) {
                      runOnUiThread(
                          () ->
                              toast(
                                  "Couldn't apply wallpaper. Check your device's wallpaper"
                                      + " settings."));
                    } finally {
                      if (bitmap != null) bitmap.recycle();
                      runOnUiThread(
                          () -> {
                            applying = false;
                            if (!isFinishing()) refresh();
                          });
                    }
                  });
            })
        .show();
  }

  private void askCompanion() {
    if (!EditionFeatures.HAS_COMPANION) return;
    new AlertDialog.Builder(this)
        .setTitle("Enable screen companion?")
        .setMessage(
            "Luma needs permission to display over other apps and keeps a visible notification so"
                + " you can turn it off. It never replaces Android’s PIN or biometrics. On some"
                + " phones it appears only after unlocking. No accessibility service is needed.")
        .setNegativeButton("Maybe later", null)
        .setPositiveButton(
            "Continue",
            (d, w) -> {
              if (!Settings.canDrawOverlays(this)) {
                awaitingOverlay = true;
                try {
                  startActivity(
                      new Intent(
                          Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                          Uri.parse("package:" + getPackageName())));
                } catch (ActivityNotFoundException e) {
                  awaitingOverlay = false;
                  toast("This phone doesn't support overlay access.");
                }
              } else enableCompanion();
            })
        .show();
  }

  private void enableCompanion() {
    if (!EditionFeatures.HAS_COMPANION) return;
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS);
      return;
    }
    prefs.setFlag("enabled", true);
    try {
      EditionFeatures.start(this);
      toast("Companion enabled. Wake your screen to try it.");
    } catch (RuntimeException e) {
      prefs.setFlag("enabled", false);
      toast("Android couldn't start the companion. Preview is available.");
    }
    refresh();
  }

  @Override
  public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(req, permissions, results);
    if (req == NOTIFICATIONS) {
      if (results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
        enableCompanion();
      else toast("Allow notifications to keep the companion controls visible.");
    }
  }

  private boolean notificationAccess() {
    return EditionFeatures.notificationAccess(this);
  }

  private void requestNotificationAccess() {
    if (!EditionFeatures.HAS_COMPANION) return;
    new AlertDialog.Builder(this)
        .setTitle("Show your notifications?")
        .setMessage(
            "Android will grant Luma access to notifications. Luma uses this only for device-local"
                + " summaries, never uploads them, and hides sensitive content. You can revoke"
                + " access at any time in Android settings.")
        .setNegativeButton("Not now", null)
        .setPositiveButton(
            "Open Android settings",
            (d, w) -> {
              try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
              } catch (ActivityNotFoundException e) {
                toast("Notification access settings aren't available.");
              }
            })
        .show();
  }

  private void eyebrow(String text) {
    TextView t = Ui.bold(this, text, 9, Ui.ACCENT);
    t.setLetterSpacing(.18f);
    body.addView(t);
    gap(9);
  }

  private void title(String text) {
    TextView t = Ui.text(this, text, 33, Ui.TEXT);
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    t.setLetterSpacing(-.04f);
    body.addView(t);
    gap(7);
  }

  private void subtitle(String text) {
    TextView t = Ui.text(this, text, 12, Ui.MUTED);
    t.setLineSpacing(dp(3), 1);
    body.addView(t);
  }

  private void section(String title, String action, Runnable run) {
    LinearLayout row = Ui.row(this);
    row.addView(Ui.bold(this, title, 18, Ui.TEXT), new LinearLayout.LayoutParams(0, -2, 1));
    if (action != null) {
      TextView button = Ui.bold(this, action, 11, Ui.ACCENT);
      button.setPadding(dp(8), dp(8), 0, dp(8));
      button.setOnClickListener(v -> run.run());
      button.setFocusable(true);
      row.addView(button);
    }
    body.addView(row);
  }

  private LinearLayout panel() {
    LinearLayout card = Ui.column(this);
    card.setPadding(dp(16), dp(16), dp(16), dp(16));
    card.setBackground(Ui.bordered(Ui.PANEL, 0xFF2E2B3D, 22, this));
    return card;
  }

  private void weighted(LinearLayout row, android.view.View child, boolean margin) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
    if (margin) lp.rightMargin = dp(12);
    row.addView(child, lp);
  }

  private void gap(int value) {
    Ui.space(body, value);
  }

  private int dp(float n) {
    return Ui.dp(this, n);
  }

  private void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
  }

  @Override
  protected void onDestroy() {
    io.shutdown();
    super.onDestroy();
  }
}
