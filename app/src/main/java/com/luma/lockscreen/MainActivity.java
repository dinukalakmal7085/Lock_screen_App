package com.luma.lockscreen;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
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
  private int tab, restoreScroll, filterScrollX;
  private String themeQuery = "", themeCollection = "All";
  private boolean awaitingOverlay, applying, resumed;
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
    if (saved != null) {
      tab = saved.getInt("tab", 0);
      themeQuery = saved.getString("themeQuery", "");
      themeCollection = saved.getString("themeCollection", "All");
      filterScrollX = saved.getInt("filterScrollX", 0);
      restoreScroll = saved.getInt("scrollY", 0);
    }
    render();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("tab", tab);
    b.putString("themeQuery", themeQuery);
    b.putString("themeCollection", themeCollection);
    b.putInt("filterScrollX", filterScrollX);
    b.putInt("scrollY", scroll == null ? restoreScroll : scroll.getScrollY());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (awaitingOverlay) {
      awaitingOverlay = false;
      if (EditionFeatures.accessGranted(this)) {
        prefs.setFlag("enabled", true);
        toast("Lock-screen access is enabled.");
      } else {
        prefs.setFlag("enabled", false);
        toast("Lock-screen access wasn't enabled. Preview still works.");
      }
    }
    if (root != null && resumed) refresh();
    resumed = true;
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
    else if (tab == 2) library();
    else settings();
    root.addView(navigation());
    setContentView(root);
    root.requestApplyInsets();
    scroll.post(() -> scroll.scrollTo(0, restoreScroll));
  }

  private void studio() {
    eyebrow("YOUR LOCK SCREEN, REIMAGINED");
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
    hero.addView(new PortraitPreview(this, prefs), new FrameLayout.LayoutParams(-1, -1));
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
    gap(12);
    TextView details = Ui.text(this, "CINEMATIC THEMES  ·  GLASS XL  ·  PHOTO DEPTH", 9, Ui.MUTED);
    details.setGravity(Gravity.CENTER);
    details.setLetterSpacing(.08f);
    body.addView(details);
    gap(24);
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
    title("Find your signature.");
    subtitle("41 original themes. Cinematic scenes. Tall glass clocks.");
    gap(18);
    body.addView(Ui.button(this, "＋   Choose a photo", false, this::pickPhoto));
    gap(14);
    EditText search = new EditText(this);
    search.setTextColor(Ui.TEXT);
    search.setHintTextColor(Ui.MUTED);
    search.setTextSize(14);
    search.setSingleLine(true);
    search.setHint("Search themes");
    search.setContentDescription("Search themes");
    search.setText(themeQuery);
    body.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));
    HorizontalScrollView filters = new HorizontalScrollView(this);
    filters.setHorizontalScrollBarEnabled(false);
    filters.setContentDescription("Theme collection filters");
    int savedFilterX = filterScrollX;
    filters.setOnScrollChangeListener((v, x, y, oldX, oldY) -> filterScrollX = x);
    LinearLayout chips = Ui.row(this);
    for (String collection : ThemeCatalog.COLLECTIONS) {
      TextView chip =
          Ui.button(
              this,
              collection,
              themeCollection.equals(collection),
              () -> {
                filterScrollX = filters.getScrollX();
                themeCollection = collection;
                refresh();
              });
      chip.setTextSize(11);
      chip.setSelected(themeCollection.equals(collection));
      chip.setMinHeight(dp(42));
      chip.setContentDescription(collection + " collection");
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
      lp.rightMargin = dp(6);
      chips.addView(chip, lp);
    }
    filters.addView(chips);
    filters.post(() -> filters.scrollTo(savedFilterX, 0));
    body.addView(filters);
    gap(18);
    LinearLayout grid = Ui.column(this);
    body.addView(grid);
    renderThemes(grid);
    search.addTextChangedListener(
        new android.text.TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          public void afterTextChanged(android.text.Editable text) {
            themeQuery = text.toString();
            renderThemes(grid);
          }
        });
    gap(10);
    body.addView(Ui.button(this, "Preview selected wallpaper  ↗", true, this::preview));
    gap(10);
    body.addView(Ui.button(this, "Edit this look", false, this::customize));
    gap(10);
    body.addView(Ui.button(this, "Apply lock wallpaper", false, this::applyWallpaper));
  }

  private int[] matchingThemes() {
    java.util.List<Integer> matches = new java.util.ArrayList<>();
    for (int i : ThemeCatalog.DISPLAY_ORDER) {
      if (!ThemeCatalog.NAMES[i]
          .toLowerCase(java.util.Locale.ROOT)
          .contains(themeQuery.toLowerCase(java.util.Locale.ROOT))) continue;
      if (themeCollection.equals("Favorites") && !prefs.favorite(i)) continue;
      if (!themeCollection.equals("All")
          && !themeCollection.equals("Favorites")
          && !(themeCollection.equals("Cinematic") && ThemeCatalog.cinematic(i))
          && !ThemeCatalog.collection(i).equals(themeCollection)) continue;
      matches.add(i);
    }
    return matches.stream().mapToInt(Integer::intValue).toArray();
  }

  private void openTheme(int id, int[] choices) {
    startActivity(
        new Intent(this, ThemePreviewActivity.class)
            .putExtra("theme", id)
            .putExtra("themes", choices));
  }

  private void renderThemes(LinearLayout grid) {
    grid.removeAllViews();
    LinearLayout row = null;
    int visible = 0;
    final int[] choices = matchingThemes();
    for (int i : choices) {
      if (visible % 2 == 0) {
        row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        grid.addView(row);
      }
      final int id = i;
      LinearLayout tile = Ui.column(this);
      FrameLayout art = new RoundedFrameLayout(this, 22);
      art.setBackground(Ui.rounded(Ui.PANEL, 22, this));
      art.addView(new ThemeTile(this, id), new FrameLayout.LayoutParams(-1, -1));
      TextView favorite = Ui.bold(this, prefs.favorite(id) ? "♥" : "♡", 20, Color.WHITE);
      favorite.setGravity(Gravity.CENTER);
      favorite.setBackground(Ui.rounded(0x40000000, 22, this));
      favorite.setContentDescription("Favorite " + ThemeCatalog.NAMES[id]);
      favorite.setOnClickListener(
          v -> {
            prefs.favorite(id, !prefs.favorite(id));
            renderThemes(grid);
          });
      FrameLayout.LayoutParams fp =
          new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.BOTTOM | Gravity.END);
      fp.setMargins(0, 0, dp(8), dp(8));
      art.addView(favorite, fp);
      art.setContentDescription("Preview " + ThemeCatalog.NAMES[id] + " theme");
      art.setOnClickListener(v -> openTheme(id, choices));
      art.setFocusable(true);
      android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
      float aspect =
          Math.max(metrics.widthPixels, metrics.heightPixels)
              / (float) Math.min(metrics.widthPixels, metrics.heightPixels);
      int tileHeight =
          Math.max(
              260,
              Math.min(420, Math.round((metrics.widthPixels / metrics.density - 60) / 2 * aspect)));
      tile.addView(art, new LinearLayout.LayoutParams(-1, dp(tileHeight)));
      Ui.space(tile, 8);
      tile.addView(
          Ui.bold(
              this,
              ThemeCatalog.NAMES[id]
                  + (prefs.wallpaper() == id && !prefs.flag("photo", false) ? "  ✓" : ""),
              14,
              Ui.TEXT));
      tile.addView(
          Ui.text(
              this,
              ThemeCatalog.collection(id) + (ThemeCatalog.depth(id) ? " · Depth" : ""),
              10,
              Ui.MUTED));
      Ui.space(tile, 18);
      weighted(row, tile, visible % 2 == 0);
      visible++;
    }
    if (visible % 2 == 1) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
    if (visible == 0) {
      TextView empty =
          Ui.text(
              this,
              themeCollection.equals("Favorites")
                  ? "Tap a heart to collect your favorite themes."
                  : "No themes match your search.",
              14,
              Ui.MUTED);
      empty.setPadding(0, dp(28), 0, dp(28));
      grid.addView(empty);
    }
  }

  private void library() {
    eyebrow("YOUR COLLECTION");
    title("A look for every you.");
    subtitle("Save a signature. Switch with a tap.");
    gap(20);
    body.addView(Ui.button(this, "＋   Create a look", true, this::customize));
    gap(22);
    java.util.List<LookStore.Look> looks = new LookStore(this).all();
    if (looks.isEmpty()) {
      LinearLayout empty = panel();
      empty.addView(Ui.bold(this, "Your story starts here.", 20, Ui.TEXT));
      Ui.space(empty, 10);
      empty.addView(
          Ui.text(
              this,
              "Choose a theme, open the editor, and save your first look. Your saved looks stay on"
                  + " your phone.",
              13,
              Ui.MUTED));
      body.addView(empty);
    }
    for (LookStore.Look look : looks) {
      LinearLayout card = panel();
      Preferences snapshot = new Preferences(this, "luma_preview_" + look.id);
      snapshot.design(look.design);
      FrameLayout art = new RoundedFrameLayout(this, 20);
      art.addView(new PortraitPreview(this, snapshot), new FrameLayout.LayoutParams(-1, -1));
      card.addView(art, new LinearLayout.LayoutParams(-1, dp(245)));
      Ui.space(card, 12);
      card.addView(Ui.bold(this, look.name, 18, Ui.TEXT));
      Ui.space(card, 12);
      LinearLayout actions = Ui.row(this);
      weighted(
          actions,
          Ui.button(
              this,
              "Use look",
              true,
              () -> {
                new LookStore(this).restore(look.id, prefs);
                switchTab(0);
              }),
          true);
      weighted(
          actions,
          Ui.button(
              this,
              "Edit",
              false,
              () -> {
                new LookStore(this).restore(look.id, prefs);
                customize();
              }),
          false);
      card.addView(actions);
      Ui.space(card, 8);
      card.addView(
          Ui.button(
              this,
              "Delete",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("Delete " + look.name + "?")
                      .setMessage("This removes the saved look from this phone.")
                      .setNegativeButton("Keep", null)
                      .setPositiveButton(
                          "Delete",
                          (d, w) -> {
                            new LookStore(this).delete(look.id);
                            refresh();
                          })
                      .show()));
      body.addView(card);
      gap(16);
    }
  }

  private void settings() {
    eyebrow("YOUR WAY");
    title("Make it personal.");
    subtitle("Little details. Thoughtfully yours.");
    gap(24);
    if (EditionFeatures.HAS_COMPANION) {
      LinearLayout companion = panel();
      companion.addView(Ui.bold(this, "✦   Your lock-screen experience", 18, Ui.TEXT));
      Ui.space(companion, 8);
      TextView help =
          Ui.text(
              this,
              "Show your chosen design when your phone wakes. Luma uses optional accessibility"
                  + " access; Android keeps your PIN and biometrics.",
              13,
              Ui.MUTED);
      help.setLineSpacing(dp(4), 1);
      companion.addView(help);
      Ui.space(companion, 16);
      boolean enabled =
          prefs.flag("enabled", false)
              && EditionFeatures.isActive()
              && EditionFeatures.accessGranted(this);
      companion.addView(
          Ui.button(
              this,
              enabled ? "Turn off lock screen" : "Set up lock screen",
              true,
              () -> {
                if (enabled) {
                  prefs.setFlag("enabled", false);
                  EditionFeatures.stop(this);
                  refresh();
                } else askCompanion();
              }));
      companion.addView(
          Ui.button(
              this,
              "Enable flashlight shortcut",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("Enable flashlight control?")
                      .setMessage(
                          "Android requires camera permission to control the flashlight. Luma uses"
                              + " it only for the torch; it does not take pictures. The camera"
                              + " shortcut opens your camera app.")
                      .setNegativeButton("Not now", null)
                      .setPositiveButton(
                          "Allow",
                          (d, w) ->
                              requestPermissions(new String[] {Manifest.permission.CAMERA}, 12))
                      .show()));
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
                    ? "Your PIN and biometrics stay with Android. Accessibility access displays"
                        + " your design and observes window state to keep credential prompts and"
                        + " calls visible. Luma does not retrieve screen text or perform"
                        + " automated gestures. Notification summaries use separate optional"
                        + " access."
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
                          prefs.design(new org.json.JSONObject());
                          ThemeCatalog.apply(prefs, 0);
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
    String[] names = {"Studio", "Wallpapers", "Saved", "Settings"};
    String[] symbols = {"✦", "▧", "♡", "⚙"};
    for (int i = 0; i < 4; i++) {
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
    for (int i : ThemeCatalog.FEATURED) {
      final int index = i;
      LinearLayout tile = Ui.column(this);
      WallpaperView art = new WallpaperView(this, i);
      art.setBackground(Ui.rounded(Ui.PANEL, 16, this));
      art.setClipToOutline(true);
      art.setContentDescription("Preview " + Preferences.WALLPAPERS[i] + " theme");
      art.setOnClickListener(v -> openTheme(index, ThemeCatalog.FEATURED));
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
    startActivity(new Intent(this, EditorActivity.class));
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
    toast("Importing photo…");
    io.execute(
        () -> {
          try {
            String filename = PhotoStore.importImage(this, data.getData(), false);
            prefs.setText("photoFile", filename);
            prefs.setFlag("photo", true);
            prefs.setText("foregroundFile", "");
            prefs.setFlag("depth", false);
            prefs.setNumber("zoom", 100);
            prefs.setNumber("panX", 0);
            prefs.setNumber("panY", 0);
            prefs.setNumber("shade", 18);
            runOnUiThread(
                () -> {
                  if (!isFinishing() && !isDestroyed()) {
                    refresh();
                    customize();
                  }
                });
          } catch (IOException | SecurityException | OutOfMemoryError e) {
            runOnUiThread(() -> toast("Couldn't import this photo. Try a smaller JPEG or PNG."));
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
              WallpaperView art = new WallpaperView(this, prefs.wallpaper()).config(prefs);
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
        .setTitle("Enable Luma lock-screen access?")
        .setMessage(
            "Luma uses Android Accessibility Service to show your chosen lock screen when the phone"
                + " wakes and to keep credential prompts and calls visible. It observes window"
                + " state; it does not retrieve screen text, type, perform automated gestures, or"
                + " collect app activity. Your PIN and biometrics remain with Android. Notification"
                + " reading is separate and optional. You can disable this access at any time in"
                + " Android settings.")
        .setNegativeButton("Not now", null)
        .setPositiveButton(
            "Agree & open settings",
            (dialog, which) -> {
              awaitingOverlay = true;
              try {
                EditionFeatures.start(this);
              } catch (ActivityNotFoundException e) {
                awaitingOverlay = false;
                toast("Accessibility settings aren't available on this device.");
              }
            })
        .show();
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
