package com.luma.lockscreen;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ThemePreviewTest {
  private Context context;

  @Before
  public void reset() {
    context = RuntimeEnvironment.getApplication();
    for (String name :
        new String[] {"luma", "luma_looks", "luma_theme_preview", "luma_editor_draft"})
      context.getSharedPreferences(name, 0).edit().clear().commit();
  }

  private Intent theme() {
    return new Intent(context, ThemePreviewActivity.class)
        .putExtra("theme", 32)
        .putExtra("themes", new int[] {32, 33});
  }

  @Test
  public void browsingAndCancellingDoNotChangeCurrentLook() {
    Preferences p = new Preferences(context);
    ThemeCatalog.apply(p, 2);
    p.clock(1);
    try (ActivityController<ThemePreviewActivity> c =
        Robolectric.buildActivity(ThemePreviewActivity.class, theme()).setup()) {
      find(c.get(), "Next theme").performClick();
      find(c.get(), "Favorite preview theme").performClick();
      assertEquals(2, p.wallpaper());
      assertEquals(1, p.clock());
      assertTrue(p.favorite(33));
      find(c.get(), "Close theme preview").performClick();
      assertTrue(c.get().isFinishing());
      assertEquals(2, p.wallpaper());
    }
  }

  @Test
  public void useCommitsOnlySelectedAppearanceAndExplicitDepthChoice() {
    Preferences p = new Preferences(context);
    p.setFlag("enabled", false);
    p.setFlag("notifications", false);
    try (ActivityController<ThemePreviewActivity> c =
        Robolectric.buildActivity(ThemePreviewActivity.class, theme()).setup()) {
      find(c.get(), "Frosted preview glass").performClick();
      ((Switch) find(c.get(), "Preview foreground depth")).setChecked(false);
      assertEquals(0, p.wallpaper());
      find(c.get(), "Use preview theme").performClick();
      assertEquals(32, p.wallpaper());
      assertEquals(6, p.clock());
      assertEquals(1, p.number("glassFinish", -1));
      assertFalse(p.flag("depth", true));
      assertFalse(p.flag("enabled", true));
      assertFalse(p.flag("notifications", true));
    }
  }

  @Test
  public void previewRotationRetainsIndexFinishAndDepthDraft() {
    try (ActivityController<ThemePreviewActivity> c =
        Robolectric.buildActivity(ThemePreviewActivity.class, theme()).setup()) {
      find(c.get(), "Next theme").performClick();
      find(c.get(), "Solid preview glass").performClick();
      ((Switch) find(c.get(), "Preview foreground depth")).setChecked(false);
      c.recreate();
      assertTrue(find(c.get(), "Solid preview glass").isSelected());
      assertFalse(((Switch) find(c.get(), "Preview foreground depth")).isChecked());
      assertEquals(0, new Preferences(context).wallpaper());
      find(c.get(), "Use preview theme").performClick();
      assertEquals(33, new Preferences(context).wallpaper());
    }
  }

  @Test
  public void customizeUsesPreviewDraftAndOnlyEditorSaveAppliesIt() {
    Intent edit;
    try (ActivityController<ThemePreviewActivity> c =
        Robolectric.buildActivity(ThemePreviewActivity.class, theme()).setup()) {
      find(c.get(), "Frosted preview glass").performClick();
      find(c.get(), "Customize preview theme").performClick();
      edit = shadowOf(c.get()).getNextStartedActivity();
      assertEquals(EditorActivity.class.getName(), edit.getComponent().getClassName());
    }
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class, edit).setup()) {
      assertEquals(32, new Preferences(context, "luma_editor_draft").wallpaper());
      find(c.get(), "Cancel editing").performClick();
      assertEquals(0, new Preferences(context).wallpaper());
    }
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class, edit).setup().visible()) {
      shadowOf(Looper.getMainLooper()).idle();
      ((Spinner) find(c.get(), "Glass finish")).setSelection(2);
      shadowOf(Looper.getMainLooper()).idle();
      find(c.get(), "Save look").performClick();
      assertEquals(32, new Preferences(context).wallpaper());
      assertEquals(2, new Preferences(context).number("glassFinish", -1));
      assertEquals(1, new LookStore(context).all().size());
    }
  }

  @Test
  public void galleryKeepsSearchCollectionAndScrollDuringRotation() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup().visible()) {
      find(c.get(), "Wallpapers tab").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      HorizontalScrollView filters =
          (HorizontalScrollView) find(c.get(), "Theme collection filters");
      filters.scrollTo(900, 0);
      find(c.get(), "Fantasy collection").performClick();
      ((EditText) find(c.get(), "Search themes")).setText("Guardian");
      shadowOf(Looper.getMainLooper()).idle();
      find(c.get(), "Preview Eclipse Guardian theme").performClick();
      Intent opened = shadowOf(c.get()).getNextStartedActivity();
      assertEquals(ThemePreviewActivity.class.getName(), opened.getComponent().getClassName());
      assertArrayEquals(new int[] {36}, opened.getIntArrayExtra("themes"));
      assertEquals(0, new Preferences(context).wallpaper());
      c.recreate();
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals("Guardian", ((EditText) find(c.get(), "Search themes")).getText().toString());
      assertTrue(find(c.get(), "Fantasy collection").isSelected());
      assertTrue(
          ((HorizontalScrollView) find(c.get(), "Theme collection filters")).getScrollX() > 0);
      assertNotNull(find(c.get(), "Preview Eclipse Guardian theme"));
      assertNull(find(c.get(), "Preview Abyss theme"));
    }
  }

  @Test
  public void glassFinishesAndWidthRenderDifferentlyAndSurviveNamedLooks() {
    Preferences p = new Preferences(context);
    ThemeCatalog.apply(p, 37);
    p.setNumber("glassFinish", 0);
    Bitmap clear = clock(p);
    p.setNumber("glassFinish", 1);
    Bitmap frosted = clock(p);
    int changed = 0;
    for (int y = 0; y < clear.getHeight(); y += 3)
      for (int x = 0; x < clear.getWidth(); x += 3)
        if (clear.getPixel(x, y) != frosted.getPixel(x, y)) changed++;
    assertTrue("Clear and frosted must visibly differ", changed > 200);
    p.setNumber("glassFinish", 2);
    p.setNumber("clockWidth", 60);
    Bitmap narrow = clock(p);
    assertTrue(inkWidth(narrow) < inkWidth(clear) * .7f);
    LookStore looks = new LookStore(context);
    looks.save("Narrow solid", p);
    p.setNumber("clockWidth", 100);
    p.setNumber("glassFinish", 0);
    looks.restore(looks.all().get(0).id, p);
    assertEquals(60, p.number("clockWidth", 0));
    assertEquals(2, p.number("glassFinish", -1));
    clear.recycle();
    frosted.recycle();
    narrow.recycle();
  }

  @Test
  @Config(sdk = {26, 36})
  public void themePreviewLaunchesOnMinimumAndTargetAndroid() {
    try (ActivityController<ThemePreviewActivity> c =
        Robolectric.buildActivity(ThemePreviewActivity.class, theme()).setup()) {
      assertNotNull(find(c.get(), "Use preview theme"));
      find(c.get(), "Use preview theme").performClick();
      assertEquals(32, new Preferences(context).wallpaper());
    }
  }

  @Test
  public void rendersThemeBrowserAndThreeGlassFinishes() throws Exception {
    try (ActivityController<ThemePreviewActivity> c =
        Robolectric.buildActivity(
                ThemePreviewActivity.class,
                theme().putExtra("themes", new int[] {32, 33, 34, 37, 38, 35, 36, 39, 40}))
            .setup()
            .visible()) {
      capture(c.get(), "theme-browser-clear");
      find(c.get(), "Frosted preview glass").performClick();
      capture(c.get(), "theme-browser-frosted");
      find(c.get(), "Solid preview glass").performClick();
      capture(c.get(), "theme-browser-solid");
    }
  }

  private Bitmap clock(Preferences p) {
    LockFace face = new LockFace(context, false, p);
    face.clock.setTimeZone("UTC");
    face.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
    face.layout(0, 0, 1080, 2400);
    Bitmap bitmap =
        Bitmap.createBitmap(face.clock.getWidth(), face.clock.getHeight(), Bitmap.Config.ARGB_8888);
    face.clock.draw(new Canvas(bitmap));
    return bitmap;
  }

  private int inkWidth(Bitmap bitmap) {
    int min = bitmap.getWidth(), max = 0;
    for (int y = 0; y < bitmap.getHeight(); y += 3)
      for (int x = 0; x < bitmap.getWidth(); x++)
        if (Color.alpha(bitmap.getPixel(x, y)) > 100) {
          min = Math.min(min, x);
          max = Math.max(max, x);
        }
    return max - min;
  }

  private void capture(Activity a, String name) throws Exception {
    shadowOf(Looper.getMainLooper()).idle();
    View v = a.getWindow().getDecorView();
    v.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
    v.layout(0, 0, 1080, 2400);
    Bitmap b = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
    v.draw(new Canvas(b));
    File dir = new File("build/screenshots");
    dir.mkdirs();
    try (FileOutputStream out =
        new FileOutputStream(new File(dir, BuildConfig.FLAVOR + "-" + name + ".png"))) {
      assertTrue(b.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    b.recycle();
  }

  private View find(Activity a, String label) {
    return find(a.getWindow().getDecorView(), label);
  }

  private View find(View root, String label) {
    if (label.contentEquals(
        root.getContentDescription() == null ? "" : root.getContentDescription())) return root;
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        View v = find(((ViewGroup) root).getChildAt(i), label);
        if (v != null) return v;
      }
    return null;
  }
}
