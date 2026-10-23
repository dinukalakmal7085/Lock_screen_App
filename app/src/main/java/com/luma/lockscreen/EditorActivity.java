package com.luma.lockscreen;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

/** A private draft drives the preview; only Save commits appearance changes. */
public final class EditorActivity extends Activity {
  private Preferences draft;
  private FrameLayout preview;
  private LinearLayout controls;
  private EditText name;
  private TextView saveButton;
  private boolean importing;
  private int selectedTab;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private final Runnable update = this::refreshPreview;
  private static final int BACKGROUND = 40, FOREGROUND = 41;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    draft = new Preferences(this, "luma_editor_draft");
    if (saved == null) {
      draft.copyDesign(new Preferences(this));
      draft.setText("draftName", draft.wallpaperName());
    } else selectedTab = saved.getInt("editorTab", 0);
    LinearLayout root = Ui.column(this);
    root.setBackgroundColor(Ui.BG);
    Ui.systemInsets(root);
    LinearLayout toolbar = Ui.row(this);
    toolbar.setPadding(dp(16), dp(8), dp(16), dp(8));
    TextView cancel = Ui.button(this, "Cancel", false, this::finish);
    cancel.setContentDescription("Cancel editing");
    toolbar.addView(cancel);
    TextView heading = Ui.bold(this, "Create your look", 16, Ui.TEXT);
    heading.setGravity(Gravity.CENTER);
    toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    TextView save = Ui.button(this, "Save", true, this::save);
    saveButton = save;
    save.setContentDescription("Save look");
    toolbar.addView(save);
    root.addView(toolbar);
    preview = new RoundedFrameLayout(this, 28);
    preview.setBackgroundColor(Ui.BG);
    int height =
        getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density
                < 680
            ? 230
            : 292;
    LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(height));
    pp.setMargins(dp(24), dp(4), dp(24), dp(12));
    root.addView(preview, pp);
    LinearLayout tabs = Ui.row(this);
    tabs.setPadding(dp(20), 0, dp(20), dp(8));
    String[] labels = {"Clock", "Widgets", "Backdrop", "Depth"};
    for (int i = 0; i < labels.length; i++) {
      final int tab = i;
      TextView t =
          Ui.button(
              this,
              labels[i],
              false,
              () -> {
                selectedTab = tab;
                renderControls();
              });
      t.setTextSize(12);
      t.setPadding(dp(6), dp(10), dp(6), dp(10));
      t.setContentDescription(labels[i] + " editor tab");
      tabs.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
    }
    root.addView(tabs);
    ScrollView scroller = new ScrollView(this);
    controls = Ui.column(this);
    controls.setPadding(dp(22), dp(8), dp(22), dp(24));
    scroller.addView(controls);
    root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
    setContentView(root);
    renderControls();
    refreshPreview();
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::finish);
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("editorTab", selectedTab);
  }

  private void renderControls() {
    controls.removeAllViews();
    if (selectedTab == 0) {
      label("Type, color & composition");
      for (int row = 0; row < 2; row++) {
        LinearLayout line = Ui.row(this);
        for (int col = 0; col < 3; col++) {
          final int index = row * 3 + col;
          TextView button =
              Ui.button(
                  this,
                  Preferences.CLOCKS[index],
                  draft.clock() == index,
                  () -> {
                    draft.clock(index);
                    renderControls();
                    refreshPreview();
                  });
          button.setTextSize(12);
          button.setContentDescription(Preferences.CLOCKS[index] + " editor clock");
          LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
          lp.setMargins(dp(3), dp(3), dp(3), dp(3));
          line.addView(button, lp);
        }
        controls.addView(line);
      }
      space(16);
      label("Clock color");
      HorizontalScrollView scroll = new HorizontalScrollView(this);
      scroll.setHorizontalScrollBarEnabled(false);
      LinearLayout colors = Ui.row(this);
      String[] names = {
        "Frost", "Champagne", "Mint", "Sky", "Rose", "Lilac", "Midnight", "Apricot"
      };
      for (int i = 0; i < Preferences.INKS.length; i++) {
        final int ink = i;
        TextView swatch =
            Ui.bold(this, draft.number("ink", 0) == i ? "✓" : "", 18, i == 6 ? Color.WHITE : Ui.BG);
        swatch.setGravity(Gravity.CENTER);
        swatch.setBackground(Ui.rounded(Preferences.INKS[i], 24, this));
        swatch.setContentDescription(names[i] + " clock color");
        swatch.setOnClickListener(
            v -> {
              draft.setNumber("ink", ink);
              renderControls();
              refreshPreview();
            });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.rightMargin = dp(8);
        colors.addView(swatch, lp);
      }
      scroll.addView(colors);
      controls.addView(scroll);
      space(14);
      slider("Clock size", "clockSize", 54, 126, 94);
      slider("Vertical position", "clockOffset", 0, 100, 0);
      choices(
          "Alignment",
          new String[] {"Left", "Center", "Right"},
          draft.number("alignment", 1),
          i -> {
            draft.setNumber("alignment", i);
            refreshPreview();
          });
      choices(
          "Date format",
          new String[] {"Full", "Short", "Month", "Day"},
          draft.number("dateFormat", 0),
          i -> {
            draft.setNumber("dateFormat", i);
            refreshPreview();
          });
      toggle("24-hour clock", "24hour", false);
      toggle("Show date", "date", true);
    } else if (selectedTab == 1) {
      label("Your four-widget row");
      note("Choose useful details. Battery, date and alarms use live device data.");
      for (int i = 0; i < 4; i++) {
        final int slot = i;
        choices(
            "Widget " + (i + 1),
            LockFace.WIDGETS,
            draft.number("widget" + i, i == 0 ? 1 : i == 1 ? 2 : 0),
            value -> {
              draft.setNumber("widget" + slot, value);
              refreshPreview();
            });
      }
      EditText focus = new EditText(this);
      focus.setTextColor(Ui.TEXT);
      focus.setTextSize(15);
      focus.setHintTextColor(Ui.MUTED);
      focus.setHint("Your focus phrase");
      focus.setSingleLine(true);
      focus.setFilters(
          new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(18)});
      focus.setText(draft.text("focusText", "BE PRESENT"));
      focus.setContentDescription("Focus widget text");
      focus.addTextChangedListener(
          watcher(
              text -> {
                draft.setText("focusText", text);
                schedulePreview();
              }));
      controls.addView(focus);
    } else if (selectedTab == 2) {
      label("Make it your own");
      controls.addView(Ui.button(this, "Choose a photo", true, () -> pick(BACKGROUND)));
      space(10);
      controls.addView(
          Ui.button(
              this,
              "Use original artwork",
              false,
              () -> {
                draft.setFlag("photo", false);
                renderControls();
                refreshPreview();
              }));
      space(14);
      if (draft.flag("photo", false)) {
        slider("Photo zoom", "zoom", 100, 200, 100);
        slider("Horizontal crop", "panX", -100, 100, 0);
        slider("Vertical crop", "panY", -100, 100, 0);
        slider("Photo shade", "shade", 0, 70, 18);
      } else {
        note(
            "Switch your starting theme from the Themes collection. Every collection is original"
                + " and available offline.");
      }
    } else {
      label("A little depth. A big difference.");
      note(
          "Landscape themes place the mountain in front of your clock. For a photo, import a"
              + " transparent PNG of the foreground subject that matches your background. Automatic"
              + " subject removal is not included.");
      toggle("Foreground depth", "depth", ThemeCatalog.family(draft.wallpaper()) == 1);
      controls.addView(Ui.button(this, "Import foreground cutout", false, () -> pick(FOREGROUND)));
      space(10);
      controls.addView(
          Ui.button(
              this,
              "Remove cutout",
              false,
              () -> {
                draft.setText("foregroundFile", "");
                refreshPreview();
              }));
    }
    space(18);
    label("Name this look");
    name = new EditText(this);
    name.setTextColor(Ui.TEXT);
    name.setHintTextColor(Ui.MUTED);
    name.setTextSize(15);
    name.setSingleLine(true);
    name.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(40)});
    name.setHint("My lock screen");
    name.setText(draft.text("draftName", draft.wallpaperName()));
    name.setContentDescription("Look name");
    name.addTextChangedListener(watcher(text -> draft.setText("draftName", text)));
    controls.addView(name);
  }

  private void save() {
    if (importing) return;
    Preferences current = new Preferences(this);
    current.copyDesign(draft);
    new LookStore(this).save(name.getText().toString(), current);
    setResult(RESULT_OK);
    finish();
  }

  private void refreshPreview() {
    if (preview == null || isDestroyed()) return;
    preview.removeAllViews();
    preview.addView(new PortraitPreview(this, draft), new FrameLayout.LayoutParams(-1, -1));
  }

  private void schedulePreview() {
    handler.removeCallbacks(update);
    handler.postDelayed(update, 60);
  }

  private void slider(String label, String key, int min, int max, int fallback) {
    TextView title = Ui.bold(this, label + "  ·  " + draft.number(key, fallback), 13, Ui.TEXT);
    controls.addView(title);
    SeekBar slider = new SeekBar(this);
    slider.setContentDescription(label);
    slider.setMax(max - min);
    slider.setProgress(Math.max(0, Math.min(max - min, draft.number(key, fallback) - min)));
    slider.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int progress, boolean user) {
            if (user) {
              draft.setNumber(key, progress + min);
              title.setText(
                  String.format(java.util.Locale.getDefault(), "%s  ·  %d", label, progress + min));
              schedulePreview();
            }
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {
            handler.removeCallbacks(update);
            refreshPreview();
          }
        });
    controls.addView(slider);
    space(10);
  }

  private void choices(
      String title, String[] values, int selected, java.util.function.IntConsumer action) {
    label(title);
    Spinner spinner = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
    spinner.setAdapter(adapter);
    spinner.setSelection(Math.max(0, Math.min(values.length - 1, selected)));
    spinner.setContentDescription(title);
    spinner.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          int previous = selected;

          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position != previous) {
              previous = position;
              action.accept(position);
            }
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
    controls.addView(spinner, new LinearLayout.LayoutParams(-1, dp(48)));
    space(6);
  }

  private void toggle(String title, String key, boolean fallback) {
    Switch toggle = new Switch(this);
    toggle.setText(title);
    toggle.setTextColor(Ui.TEXT);
    toggle.setPadding(0, dp(10), 0, dp(10));
    toggle.setChecked(draft.flag(key, fallback));
    toggle.setContentDescription(title);
    toggle.setOnCheckedChangeListener(
        (v, on) -> {
          draft.setFlag(key, on);
          refreshPreview();
        });
    controls.addView(toggle);
    space(8);
  }

  private void pick(int req) {
    if (importing) return;
    try {
      startActivityForResult(
          new Intent(Intent.ACTION_OPEN_DOCUMENT)
              .setType(req == FOREGROUND ? "image/png" : "image/*")
              .addCategory(Intent.CATEGORY_OPENABLE),
          req);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "No photo picker is available.", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if ((req != BACKGROUND && req != FOREGROUND)
        || result != RESULT_OK
        || data == null
        || data.getData() == null) return;
    importing = true;
    saveButton.setEnabled(false);
    saveButton.setText("Wait…");
    io.execute(
        () -> {
          try {
            String file = PhotoStore.importImage(this, data.getData(), req == FOREGROUND);
            runOnUiThread(
                () -> {
                  if (isFinishing() || isDestroyed()) {
                    new java.io.File(getFilesDir(), file).delete();
                    return;
                  }
                  if (req == FOREGROUND) {
                    draft.setText("foregroundFile", file);
                    draft.setFlag("depth", true);
                  } else {
                    draft.setText("photoFile", file);
                    draft.setFlag("photo", true);
                    draft.setText("foregroundFile", "");
                    draft.setFlag("depth", false);
                    draft.setNumber("zoom", 100);
                    draft.setNumber("panX", 0);
                    draft.setNumber("panY", 0);
                    draft.setNumber("shade", 18);
                  }
                  finishImport();
                  renderControls();
                  refreshPreview();
                });
          } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) {
            runOnUiThread(
                () -> {
                  if (isFinishing() || isDestroyed()) return;
                  finishImport();
                  Toast.makeText(
                          this,
                          req == FOREGROUND
                              ? "Choose a transparent PNG cutout that matches your photo."
                              : "Couldn't open this photo. Try a smaller JPEG or PNG.",
                          Toast.LENGTH_LONG)
                      .show();
                });
          }
        });
  }

  private void finishImport() {
    importing = false;
    saveButton.setEnabled(true);
    saveButton.setText("Save");
  }

  private static android.text.TextWatcher watcher(java.util.function.Consumer<String> c) {
    return new android.text.TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      public void afterTextChanged(android.text.Editable e) {
        c.accept(e.toString());
      }
    };
  }

  private void label(String text) {
    controls.addView(Ui.bold(this, text, 14, Ui.TEXT));
    space(8);
  }

  private void note(String text) {
    TextView t = Ui.text(this, text, 12, Ui.MUTED);
    t.setLineSpacing(dp(4), 1);
    controls.addView(t);
    space(14);
  }

  private void space(int h) {
    Ui.space(controls, h);
  }

  private int dp(int n) {
    return Ui.dp(this, n);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacks(update);
    io.shutdown();
    super.onDestroy();
  }
}
