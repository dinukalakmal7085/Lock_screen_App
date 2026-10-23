package com.luma.lockscreen;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StudioTest {
  private Context context;

  @Before
  public void reset() {
    context = RuntimeEnvironment.getApplication();
    context.getSharedPreferences("luma", 0).edit().clear().commit();
  }

  @Test
  public void manifestRequestsOnlyWallpaperPermissionAndRegistersNoServices() throws Exception {
    PackageInfo info =
        context
            .getPackageManager()
            .getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_PERMISSIONS | PackageManager.GET_SERVICES);
    assertArrayEquals(new String[] {Manifest.permission.SET_WALLPAPER}, info.requestedPermissions);
    assertTrue(info.services == null || info.services.length == 0);
    assertEquals(
        "Luma Studio", context.getApplicationInfo().loadLabel(context.getPackageManager()));
  }

  @Test
  public void sensitiveImplementationsAreNotPartOfStudio() {
    for (String name : new String[] {"LockScreenService", "LumaNotificationListener"}) {
      try {
        Class.forName("com.luma.lockscreen." + name);
        fail("Full-edition class exists in Studio: " + name);
      } catch (ClassNotFoundException expected) {
      }
    }
    assertFalse(EditionFeatures.HAS_COMPANION);
    assertTrue(EditionFeatures.summaries().isEmpty());
  }

  @Test
  public void settingsExposePreviewControlsWithoutSpecialAccessControls() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      findDescription(c.get().getWindow().getDecorView(), "Settings tab").performClick();
      View root = c.get().getWindow().getDecorView();
      assertNotNull(findText(root, "✦   Luma Studio"));
      assertNotNull(findText(root, "24-hour clock"));
      assertNull(findText(root, "Set up lock screen"));
      assertNull(findText(root, "Allow notification access  ↗"));
      assertNull(findDescription(root, "Show notifications"));
    }
  }

  @Test
  public void updatingFromFullEditionClearsUnusedSensitiveFeaturePreferences() {
    Preferences p = new Preferences(context);
    p.setFlag("enabled", true);
    p.setFlag("notifications", true);
    p.setFlag("details", true);
    p.wallpaper(2);
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      assertFalse(p.flag("enabled", true));
      assertFalse(p.flag("notifications", true));
      assertFalse(p.flag("details", true));
      assertEquals(2, p.wallpaper());
    }
  }

  @Test
  public void wallpaperAndClockChoicesPersistAndPreviewOpens() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      findDescription(c.get().getWindow().getDecorView(), "Preview Tide theme").performClick();
      try (ActivityController<ThemePreviewActivity> theme =
          Robolectric.buildActivity(
                  ThemePreviewActivity.class, shadowOf(c.get()).getNextStartedActivity())
              .setup()) {
        findDescription(theme.get().getWindow().getDecorView(), "Use preview theme").performClick();
      }
      findDescription(c.get().getWindow().getDecorView(), "Outline clock style").performClick();
      c.recreate();
      assertEquals(2, new Preferences(c.get()).wallpaper());
      assertEquals(2, new Preferences(c.get()).clock());
      findText(c.get().getWindow().getDecorView(), "Full preview  ↗").performClick();
      Intent next = shadowOf(c.get()).getNextStartedActivity();
      assertEquals(LockScreenActivity.class.getName(), next.getComponent().getClassName());
    }
  }

  @Test
  @Config(sdk = {26, 36})
  public void previewRunsAndClosesOnMinimumAndTargetAndroid() {
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class).setup()) {
      assertNotNull(findText(c.get().getWindow().getDecorView(), "⌃   Open device"));
      findDescription(c.get().getWindow().getDecorView(), "Close preview").performClick();
      assertTrue(c.get().isFinishing());
    }
  }

  @Test
  public void rendersStudioScreens() throws Exception {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup().visible()) {
      capture(c.get(), "studio-home");
      findDescription(c.get().getWindow().getDecorView(), "Settings tab").performClick();
      capture(c.get(), "studio-settings");
    }
  }

  private void capture(Activity a, String name) throws Exception {
    shadowOf(Looper.getMainLooper()).idle();
    View decor = a.getWindow().getDecorView();
    decor.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
    decor.layout(0, 0, 1080, 2400);
    Bitmap bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
    decor.draw(new Canvas(bitmap));
    File directory = new File("build/screenshots");
    directory.mkdirs();
    try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }

  private View findDescription(View root, String label) {
    if (label.contentEquals(
        root.getContentDescription() == null ? "" : root.getContentDescription())) return root;
    if (root instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) root;
      for (int i = 0; i < g.getChildCount(); i++) {
        View v = findDescription(g.getChildAt(i), label);
        if (v != null) return v;
      }
    }
    return null;
  }

  private View findText(View root, String text) {
    if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return root;
    if (root instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) root;
      for (int i = 0; i < g.getChildCount(); i++) {
        View v = findText(g.getChildAt(i), text);
        if (v != null) return v;
      }
    }
    return null;
  }
}
