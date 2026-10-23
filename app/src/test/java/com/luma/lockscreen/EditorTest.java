package com.luma.lockscreen;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
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
public class EditorTest {
  private Context context;

  @Before
  public void reset() {
    context = RuntimeEnvironment.getApplication();
    context.getSharedPreferences("luma", 0).edit().clear().commit();
    context.getSharedPreferences("luma_looks", 0).edit().clear().commit();
    context.getSharedPreferences("luma_editor_draft", 0).edit().clear().commit();
  }

  @Test
  public void cancelLeavesAppearanceUntouched() {
    Preferences p = new Preferences(context);
    p.clock(1);
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup()) {
      find(c.get(), "Outline editor clock").performClick();
      assertEquals(1, p.clock());
      find(c.get(), "Cancel editing").performClick();
      assertTrue(c.get().isFinishing());
      assertEquals(1, p.clock());
      assertTrue(new LookStore(context).all().isEmpty());
    }
  }

  @Test
  public void saveCommitsOnlyAppearanceAndCreatesRestorableNamedLook() {
    Preferences p = new Preferences(context);
    p.setFlag("enabled", true);
    p.setFlag("notifications", true);
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup()) {
      find(c.get(), "Editorial editor clock").performClick();
      ((EditText) find(c.get(), "Look name")).setText("Evening ritual");
      find(c.get(), "Save look").performClick();
      assertEquals(4, p.clock());
      assertTrue(p.flag("enabled", false));
      assertTrue(p.flag("notifications", false));
      LookStore store = new LookStore(context);
      assertEquals("Evening ritual", store.all().get(0).name);
      String id = store.all().get(0).id;
      p.clock(0);
      p.setFlag("enabled", false);
      store.restore(id, p);
      assertEquals(4, p.clock());
      assertFalse(p.flag("enabled", true));
      store.delete(id);
      assertTrue(store.all().isEmpty());
    }
  }

  @Test
  public void librarySwitchAndFavoritesPersistIndependently() {
    Preferences p = new Preferences(context);
    ThemeCatalog.apply(p, 6);
    p.setNumber("widget3", 4);
    p.setText("focusText", "REST");
    p.favorite(6, true);
    LookStore store = new LookStore(context);
    store.save("Mountains", p);
    ThemeCatalog.apply(p, 14);
    p.setNumber("widget3", 0);
    store.restore(store.all().get(0).id, p);
    assertEquals(6, p.wallpaper());
    assertEquals(4, p.number("widget3", 0));
    assertEquals("REST", p.text("focusText", ""));
    assertTrue(p.favorite(6));
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      find(c.get(), "Saved tab").performClick();
      assertNotNull(text(c.get().getWindow().getDecorView(), "Mountains"));
    }
  }

  @Test
  public void editorDraftSurvivesRotationButDoesNotChangeCurrentLook() {
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup()) {
      find(c.get(), "Mono editor clock").performClick();
      c.recreate();
      assertEquals(5, new Preferences(context, "luma_editor_draft").clock());
      assertEquals(0, new Preferences(context).clock());
      find(c.get(), "Save look").performClick();
      assertEquals(5, new Preferences(context).clock());
    }
  }

  @Test
  public void backgroundImportWaitsForDecodeAndClearsPreviousPhotoCutout() throws Exception {
    Preferences p = new Preferences(context);
    p.setText("foregroundFile", "old-cutout.png");
    p.setFlag("depth", true);
    Bitmap image = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888);
    image.eraseColor(Color.BLUE);
    File input = new File(context.getCacheDir(), "background.png");
    try (FileOutputStream out = new FileOutputStream(input)) {
      image.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    image.recycle();
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup().visible()) {
      c.get().onActivityResult(40, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(input)));
      assertFalse(find(c.get(), "Save look").isEnabled());
      java.util.concurrent.ExecutorService executor =
          org.robolectric.util.ReflectionHelpers.getField(c.get(), "io");
      executor.submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS);
      shadowOf(Looper.getMainLooper()).idle();
      assertTrue(find(c.get(), "Save look").isEnabled());
      find(c.get(), "Save look").performClick();
      assertTrue(p.flag("photo", false));
      assertFalse(p.flag("depth", true));
      assertEquals("", p.text("foregroundFile", "missing"));
      assertTrue(new File(context.getFilesDir(), p.text("photoFile", "")).isFile());
    }
  }

  @Test
  public void transparentForegroundImportIsPrivateAndOpaqueImageIsRejected() throws Exception {
    Bitmap image = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888);
    image.eraseColor(Color.TRANSPARENT);
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    new Canvas(image).drawCircle(40, 60, 25, paint);
    File input = new File(context.getCacheDir(), "cutout.png");
    try (FileOutputStream out = new FileOutputStream(input)) {
      image.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    image.recycle();
    String imported = PhotoStore.importImage(context, Uri.fromFile(input), true);
    assertTrue(new File(context.getFilesDir(), imported).isFile());
    assertTrue(
        BitmapFactory.decodeFile(new File(context.getFilesDir(), imported).getPath()).hasAlpha());
    Bitmap opaque = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888);
    opaque.eraseColor(Color.RED);
    try (FileOutputStream out = new FileOutputStream(input)) {
      opaque.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    opaque.recycle();
    try {
      PhotoStore.importImage(context, Uri.fromFile(input), true);
      fail("Opaque foreground must be rejected");
    } catch (IOException expected) {
    }
  }

  @Test
  public void allCollectionsRenderWithoutNetworkAndSearchFiltersThemes() {
    assertEquals(32, ThemeCatalog.NAMES.length);
    for (int i = 0; i < ThemeCatalog.NAMES.length; i++) {
      Bitmap b = new WallpaperView(context, i).bitmap(80, 160);
      assertTrue(Color.alpha(b.getPixel(40, 80)) > 0);
      b.recycle();
    }
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      find(c.get(), "Wallpapers tab").performClick();
      ((EditText) find(c.get(), "Search themes")).setText("Alpine");
      assertNotNull(find(c.get(), "Select Alpine wallpaper"));
      assertNull(find(c.get(), "Select Iris wallpaper"));
    }
  }

  @Test
  public void themeSwitchGalleryIsAnActualActivity() {
    try (ActivityController<SwitcherActivity> c =
        Robolectric.buildActivity(SwitcherActivity.class).setup()) {
      find(c.get(), "Use Alpine lock screen").performClick();
      assertEquals(6, new Preferences(context).wallpaper());
      assertTrue(c.get().isFinishing());
    }
  }

  @Test
  public void widgetSelectionAndFocusTextSaveFromVisibleEditor() {
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup().visible()) {
      find(c.get(), "Widgets editor tab").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      ((Spinner) find(c.get(), "Widget 3")).setSelection(3);
      ((Spinner) find(c.get(), "Widget 4")).setSelection(4);
      shadowOf(Looper.getMainLooper()).idle();
      ((EditText) find(c.get(), "Focus widget text")).setText("MAKE SPACE");
      find(c.get(), "Save look").performClick();
      Preferences p = new Preferences(context);
      assertEquals(3, p.number("widget2", 0));
      assertEquals(4, p.number("widget3", 0));
      assertEquals("MAKE SPACE", p.text("focusText", ""));
    }
  }

  @Test
  public void lockedPreviewRequiresAndroidUnlockBeforeOpeningLookSwitcher() {
    shadowOf(context.getSystemService(android.app.KeyguardManager.class)).setKeyguardLocked(true);
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class).setup().visible()) {
      ViewGroup content = c.get().findViewById(android.R.id.content);
      content.getChildAt(0).performLongClick();
      assertNull(shadowOf(c.get()).getNextStartedActivity());
      assertFalse(c.get().isFinishing());
      shadowOf(context.getSystemService(android.app.KeyguardManager.class))
          .setKeyguardLocked(false);
      content.getChildAt(0).performLongClick();
      assertEquals(
          SwitcherActivity.class.getName(),
          shadowOf(c.get()).getNextStartedActivity().getComponent().getClassName());
    }
  }

  @Test
  public void rendersDepthClockAndLiveEditor() throws Exception {
    ThemeCatalog.apply(new Preferences(context), 6);
    try (ActivityController<EditorActivity> c =
        Robolectric.buildActivity(EditorActivity.class).setup().visible()) {
      capture(c.get(), "editor");
      find(c.get(), "Widgets editor tab").performClick();
      capture(c.get(), "widgets");
    }
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class).setup().visible()) {
      capture(c.get(), "depth");
    }
    for (int style : new int[] {0, 3}) {
      new Preferences(context).clock(style);
      try (ActivityController<LockScreenActivity> c =
          Robolectric.buildActivity(LockScreenActivity.class).setup().visible()) {
        capture(c.get(), Preferences.CLOCKS[style].toLowerCase(java.util.Locale.ROOT));
      }
    }
  }

  private void capture(Activity a, String name) throws Exception {
    shadowOf(Looper.getMainLooper()).idle();
    View decor = a.getWindow().getDecorView();
    decor.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
    decor.layout(0, 0, 1080, 2400);
    Bitmap b = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
    decor.draw(new Canvas(b));
    File dir = new File("build/screenshots");
    dir.mkdirs();
    try (FileOutputStream out =
        new FileOutputStream(new File(dir, BuildConfig.FLAVOR + "-" + name + ".png"))) {
      assertTrue(b.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    b.recycle();
  }

  private View find(Activity a, String desc) {
    return description(a.getWindow().getDecorView(), desc);
  }

  private View description(View root, String value) {
    if (value.contentEquals(
        root.getContentDescription() == null ? "" : root.getContentDescription())) return root;
    if (root instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) root;
      for (int i = 0; i < g.getChildCount(); i++) {
        View v = description(g.getChildAt(i), value);
        if (v != null) return v;
      }
    }
    return null;
  }

  private View text(View root, String value) {
    if (root instanceof TextView && value.contentEquals(((TextView) root).getText())) return root;
    if (root instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) root;
      for (int i = 0; i < g.getChildCount(); i++) {
        View v = text(g.getChildAt(i), value);
        if (v != null) return v;
      }
    }
    return null;
  }
}
