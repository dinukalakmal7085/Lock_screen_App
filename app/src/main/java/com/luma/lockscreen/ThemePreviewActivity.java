package com.luma.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Browse a private theme draft. Only Use theme or saving in the editor changes the active look. */
public final class ThemePreviewActivity extends Activity {
  private Preferences draft;
  private int[] themes;
  private int index;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    LinkedHashSet<Integer> valid = new LinkedHashSet<>();
    int[] offered = getIntent().getIntArrayExtra("themes");
    if (offered == null) offered = ThemeCatalog.DISPLAY_ORDER;
    for (int id : offered) if (id >= 0 && id < ThemeCatalog.NAMES.length) valid.add(id);
    if (valid.isEmpty()) valid.add(0);
    themes = valid.stream().mapToInt(Integer::intValue).toArray();
    int selected = getIntent().getIntExtra("theme", themes[0]);
    for (int i = 0; i < themes.length; i++) if (themes[i] == selected) index = i;
    draft = new Preferences(this, "luma_theme_preview");
    if (saved == null) resetDraft();
    else {
      index = Math.max(0, Math.min(themes.length - 1, saved.getInt("themeIndex", index)));
      try {
        draft.design(new JSONObject(saved.getString("themeDesign", "{}")));
      } catch (JSONException invalid) {
        resetDraft();
      }
    }
    render();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putInt("themeIndex", index);
    state.putString("themeDesign", draft.design().toString());
  }

  private void resetDraft() {
    draft.design(new JSONObject());
    ThemeCatalog.apply(draft, themes[index]);
  }

  private void step(int direction) {
    index = Math.floorMod(index + direction, themes.length);
    resetDraft();
    render();
  }

  private void render() {
    int id = themes[index];
    LinearLayout root = Ui.column(this);
    root.setBackgroundColor(Ui.BG);
    Ui.systemInsets(root);
    LinearLayout toolbar = Ui.row(this);
    toolbar.setPadding(dp(16), dp(8), dp(16), dp(4));
    TextView back = Ui.button(this, "‹", false, this::finish);
    back.setContentDescription("Close theme preview");
    toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
    TextView title = Ui.bold(this, ThemeCatalog.NAMES[id], 19, Ui.TEXT);
    title.setGravity(Gravity.CENTER);
    title.setMaxLines(1);
    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
    toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    Preferences active = new Preferences(this);
    TextView favorite = Ui.button(this, active.favorite(id) ? "♥" : "♡", false, () -> {});
    favorite.setContentDescription("Favorite preview theme");
    favorite.setOnClickListener(
        v -> {
          active.favorite(id, !active.favorite(id));
          favorite.setText(active.favorite(id) ? "♥" : "♡");
          favorite.setSelected(active.favorite(id));
        });
    favorite.setSelected(active.favorite(id));
    toolbar.addView(favorite, new LinearLayout.LayoutParams(dp(48), dp(48)));
    root.addView(toolbar);
    TextView category =
        Ui.text(this, ThemeCatalog.collection(id).toUpperCase(Locale.ROOT), 10, Ui.ACCENT);
    category.setGravity(Gravity.CENTER);
    category.setLetterSpacing(.16f);
    root.addView(category, new LinearLayout.LayoutParams(-1, dp(24)));
    PortraitPreview preview = new PortraitPreview(this, draft);
    LinearLayout.LayoutParams scene = new LinearLayout.LayoutParams(-1, 0, 1);
    scene.setMargins(dp(24), dp(8), dp(24), dp(8));
    root.addView(preview, scene);
    LinearLayout paging = Ui.row(this);
    paging.setPadding(dp(24), 0, dp(24), 0);
    TextView previous = Ui.button(this, "‹", false, () -> step(-1));
    previous.setContentDescription("Previous theme");
    previous.setEnabled(themes.length > 1);
    paging.addView(previous, new LinearLayout.LayoutParams(dp(52), dp(44)));
    TextView count = Ui.text(this, (index + 1) + " / " + themes.length, 12, Ui.MUTED);
    count.setGravity(Gravity.CENTER);
    paging.addView(count, new LinearLayout.LayoutParams(0, -2, 1));
    TextView next = Ui.button(this, "›", false, () -> step(1));
    next.setContentDescription("Next theme");
    next.setEnabled(themes.length > 1);
    paging.addView(next, new LinearLayout.LayoutParams(dp(52), dp(44)));
    root.addView(paging);
    if (draft.clock() == 0 || draft.clock() == 6) {
      LinearLayout finishes = Ui.row(this);
      finishes.setPadding(dp(20), dp(6), dp(20), 0);
      for (int i = 0; i < Preferences.GLASS_FINISHES.length; i++) {
        final int finish = i;
        TextView button =
            Ui.button(
                this,
                Preferences.GLASS_FINISHES[i],
                draft.number("glassFinish", 1) == i,
                () -> {
                  draft.setNumber("glassFinish", finish);
                  render();
                });
        button.setTextSize(12);
        button.setSelected(draft.number("glassFinish", 1) == i);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setContentDescription(Preferences.GLASS_FINISHES[i] + " preview glass");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        finishes.addView(button, lp);
      }
      root.addView(finishes);
    }
    if (ThemeCatalog.depth(id)) {
      Switch depth = new Switch(this);
      depth.setText("Foreground depth");
      depth.setContentDescription("Preview foreground depth");
      depth.setTextColor(Ui.TEXT);
      depth.setChecked(draft.flag("depth", false));
      depth.setPadding(dp(24), dp(8), dp(24), dp(8));
      depth.setOnCheckedChangeListener(
          (v, on) -> {
            draft.setFlag("depth", on);
            render();
          });
      root.addView(depth, new LinearLayout.LayoutParams(-1, dp(44)));
    }
    LinearLayout actions = Ui.row(this);
    actions.setPadding(dp(20), dp(6), dp(20), dp(4));
    TextView customize =
        Ui.button(
            this,
            "Customize",
            false,
            () -> {
              startActivity(
                  new Intent(this, EditorActivity.class)
                      .putExtra("themeDesign", draft.design().toString()));
              finish();
            });
    customize.setContentDescription("Customize preview theme");
    LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(52), 1);
    left.rightMargin = dp(8);
    actions.addView(customize, left);
    TextView use =
        Ui.button(
            this,
            "Use theme",
            true,
            () -> {
              new Preferences(this).copyDesign(draft);
              setResult(RESULT_OK);
              Toast.makeText(this, ThemeCatalog.NAMES[id] + " selected", Toast.LENGTH_SHORT).show();
              finish();
            });
    use.setContentDescription("Use preview theme");
    actions.addView(use, new LinearLayout.LayoutParams(0, dp(52), 1));
    root.addView(actions);
    TextView note =
        Ui.text(
            this,
            EditionFeatures.HAS_COMPANION
                ? "Use a look now, or customize it first."
                : "Studio previews custom clocks. Apply wallpaper from Studio.",
            11,
            Ui.MUTED);
    note.setGravity(Gravity.CENTER);
    note.setPadding(dp(20), dp(4), dp(20), dp(12));
    root.addView(note);
    setContentView(root);
  }

  private int dp(float value) {
    return Ui.dp(this, value);
  }
}
