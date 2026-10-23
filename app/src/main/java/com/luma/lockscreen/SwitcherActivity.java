package com.luma.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

/** A horizontal lock-screen gallery; opened after the system authenticates the user. */
public final class SwitcherActivity extends Activity {
  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    LinearLayout root = Ui.column(this);
    root.setBackgroundColor(Ui.BG);
    Ui.systemInsets(root);
    root.setPadding(Ui.dp(this, 18), Ui.dp(this, 28), Ui.dp(this, 18), Ui.dp(this, 20));
    TextView title = Ui.bold(this, "Your lock screens", 28, Ui.TEXT);
    title.setGravity(Gravity.CENTER);
    root.addView(title);
    Ui.space(root, 8);
    TextView hint = Ui.text(this, "Swipe through. Tap to make it yours.", 13, Ui.MUTED);
    hint.setGravity(Gravity.CENTER);
    root.addView(hint);
    Ui.space(root, 22);
    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout cards = Ui.row(this);
    Preferences current = new Preferences(this);
    java.util.List<LookStore.Look> savedLooks = new LookStore(this).all();
    for (LookStore.Look look : savedLooks) {
      Preferences snapshot = new Preferences(this, "luma_preview_" + look.id);
      snapshot.design(look.design);
      card(
          cards,
          look.name,
          snapshot,
          () -> {
            new LookStore(this).restore(look.id, current);
            finish();
          });
    }
    int[] themes = {current.wallpaper(), 32, 33, 34, 37, 38, 35, 36, 39, 40, 6, 0};
    java.util.Set<Integer> unique = new java.util.LinkedHashSet<>();
    for (int id : themes) unique.add(id);
    for (int id : unique) {
      Preferences p = new Preferences(this, "luma_switcher_" + id);
      ThemeCatalog.apply(p, id);
      card(
          cards,
          ThemeCatalog.NAMES[id],
          p,
          () -> {
            current.copyDesign(p);
            finish();
          });
    }
    scroll.addView(cards);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    Ui.space(root, 18);
    root.addView(
        Ui.button(
            this,
            "Customize current look",
            true,
            () -> {
              startActivity(new Intent(this, EditorActivity.class));
              finish();
            }));
    Ui.space(root, 10);
    root.addView(Ui.button(this, "Done", false, this::finish));
    setContentView(root);
  }

  private void card(LinearLayout row, String name, Preferences p, Runnable action) {
    LinearLayout card = Ui.column(this);
    card.setPadding(0, 0, Ui.dp(this, 14), 0);
    FrameLayout face = new RoundedFrameLayout(this, 30);
    face.addView(new PortraitPreview(this, p), new FrameLayout.LayoutParams(-1, -1));
    face.setOnClickListener(v -> action.run());
    face.setContentDescription("Use " + name + " lock screen");
    card.addView(face, new LinearLayout.LayoutParams(Ui.dp(this, 260), Ui.dp(this, 360)));
    Ui.space(card, 12);
    TextView label = Ui.bold(this, name, 16, Ui.TEXT);
    label.setGravity(Gravity.CENTER);
    card.addView(label);
    row.addView(card);
  }
}
