package com.luma.lockscreen;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CinematicTest {
  private Context context;

  @Before
  public void reset() {
    context = RuntimeEnvironment.getApplication();
    context.getSharedPreferences("luma", 0).edit().clear().commit();
    context.getSharedPreferences("luma_looks", 0).edit().clear().commit();
  }

  @Test
  public void allOriginalArtworkIsBundledAndPresetPersistsWithoutChangingAccess() {
    Set<Integer> ids = new HashSet<>();
    for (int id : ThemeCatalog.DISPLAY_ORDER) assertTrue(ids.add(id));
    assertEquals(ThemeCatalog.NAMES.length, ids.size());
    Preferences p = new Preferences(context);
    p.setFlag("enabled", true);
    p.setFlag("notifications", true);
    p.setNumber("zoom", 190);
    p.setNumber("panX", 70);
    for (int id = 32; id < 41; id++) {
      Bitmap art = ThemeAssets.bitmap(context, id, 360);
      assertNotNull(ThemeCatalog.NAMES[id], art);
      assertTrue(art.getWidth() >= 300);
      assertTrue(art.getHeight() > art.getWidth());
      assertTrue(art.getWidth() < 600); // Gallery decoding is bounded, not nine full images.
      ThemeCatalog.apply(p, id);
      assertEquals(6, p.clock());
      assertEquals(100, p.number("zoom", 0));
      assertEquals(0, p.number("panX", -1));
      assertEquals("HH:mm", new LockFace(context, false, p).clock.getFormat24Hour());
      assertTrue(p.flag("enabled", false));
      assertTrue(p.flag("notifications", false));
    }
    ThemeCatalog.apply(p, 32);
    LookStore looks = new LookStore(context);
    looks.save("Ocean glass", p);
    ThemeCatalog.apply(p, 2);
    looks.restore(looks.all().get(0).id, p);
    assertEquals(32, p.wallpaper());
    assertEquals(6, p.clock());
    assertTrue(p.flag("depth", false));
    assertEquals(0, p.number("widget0", -1));
  }

  @Test
  public void depthDrawsOnlyTheAuthoredSubjectAndCanBeDisabled() {
    Preferences p = new Preferences(context);
    ThemeCatalog.apply(p, 32);
    WallpaperView art = new WallpaperView(context, 32).config(p);
    Bitmap layer = Bitmap.createBitmap(420, 936, Bitmap.Config.ARGB_8888);
    art.drawForeground(new Canvas(layer), 420, 936);
    assertTrue(Color.alpha(layer.getPixel(148, 290)) > 200);
    assertEquals(0, Color.alpha(layer.getPixel(380, 290)));
    assertEquals(0, Color.alpha(layer.getPixel(148, 60)));
    layer.eraseColor(Color.TRANSPARENT);
    p.setFlag("depth", false);
    art.drawForeground(new Canvas(layer), 420, 936);
    assertEquals(0, Color.alpha(layer.getPixel(148, 290)));
    layer.recycle();
  }

  @Test
  public void glassClockRedrawsActualTimeWhenTimeZoneChanges() {
    Preferences p = new Preferences(context);
    ThemeCatalog.apply(p, 37);
    LockFace face = new LockFace(context, false, p);
    face.clock.setTimeZone("UTC");
    Bitmap before = render(face);
    String first = face.clock.getText().toString();
    face.clock.setTimeZone("GMT+03:00");
    String second = face.clock.getText().toString();
    assertNotEquals(first, second);
    Bitmap after = render(face);
    int changed = 0;
    for (int y = 300; y < 1100; y += 3)
      for (int x = 60; x < 1020; x += 3)
        if (before.getPixel(x, y) != after.getPixel(x, y)) changed++;
    assertTrue("Visible glyphs must change with live time", changed > 100);
    assertEquals(before.getPixel(50, 1800), after.getPixel(50, 1800));
    before.recycle();
    after.recycle();
  }

  @Test
  public void cinematicCollectionAndFullScreenThemesRender() throws Exception {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup().visible()) {
      find(c.get().getWindow().getDecorView(), "Wallpapers tab").performClick();
      find(c.get().getWindow().getDecorView(), "Cinematic collection").performClick();
      assertNotNull(find(c.get().getWindow().getDecorView(), "Select Abyss wallpaper"));
      assertNull(find(c.get().getWindow().getDecorView(), "Select Iris wallpaper"));
      capture(c.get(), "cinematic-gallery");
    }
    for (int id : new int[] {32, 33, 34, 35, 36, 37, 38, 39, 40}) {
      ThemeCatalog.apply(new Preferences(context), id);
      try (ActivityController<LockScreenActivity> c =
          Robolectric.buildActivity(LockScreenActivity.class).setup().visible()) {
        capture(c.get(), "theme-" + id);
      }
    }
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup().visible()) {
      assertNotNull(find(c.get().getWindow().getDecorView(), "Glass XL editor clock"));
      capture(c.get(), "cinematic-editor");
    }
  }

  private Bitmap render(View view) {
    view.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
    view.layout(0, 0, 1080, 2400);
    Bitmap bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
    view.draw(new Canvas(bitmap));
    return bitmap;
  }

  private void capture(Activity activity, String name) throws Exception {
    shadowOf(Looper.getMainLooper()).idle();
    Bitmap bitmap = render(activity.getWindow().getDecorView());
    File directory = new File("build/screenshots");
    directory.mkdirs();
    try (FileOutputStream out =
        new FileOutputStream(new File(directory, BuildConfig.FLAVOR + "-" + name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }

  private View find(View root, String label) {
    if (label.contentEquals(
        root.getContentDescription() == null ? "" : root.getContentDescription())) return root;
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        View found = find(((ViewGroup) root).getChildAt(i), label);
        if (found != null) return found;
      }
    return null;
  }
}
