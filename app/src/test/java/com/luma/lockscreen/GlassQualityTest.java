package com.luma.lockscreen;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GlassQualityTest {
  @Test
  public void allDigitsHaveStableTabularMetricsAndDistinctShapes() {
    ClockNumerals numerals = new ClockNumerals();
    RectF metrics = new RectF();
    long[] shapes = new long[10];
    float width = -1;
    for (int n = 0; n < 10; n++) {
      Path path = new Path();
      assertTrue(numerals.build("" + n + n + ":" + n + n, path));
      numerals.bounds(metrics);
      if (width < 0) width = metrics.width();
      assertEquals(width, metrics.width(), 0);
      assertEquals(240, metrics.height(), 0);
      Bitmap bitmap = Bitmap.createBitmap(260, 250, Bitmap.Config.ARGB_8888);
      Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
      pen.setColor(Color.WHITE);
      new Canvas(bitmap).drawPath(path, pen);
      long hash = 17;
      for (int y = 0; y < 245; y += 2)
        for (int x = 0; x < 255; x += 2) hash = hash * 31 + bitmap.getPixel(x, y);
      shapes[n] = hash;
      for (int i = 0; i < n; i++)
        assertNotEquals("Digit " + n + " must be distinct", shapes[i], hash);
      bitmap.recycle();
    }
    assertFalse(numerals.build("٠٩:٤١", new Path())); // Platform shaping handles these.
  }

  @Test
  public void glassHasLitBevelAndRetainsBackdropInsteadOfOpaqueFill() {
    Path path = new Path();
    path.addRoundRect(15, 15, 85, 185, 20, 20, Path.Direction.CW);
    int[] dark = new int[100 * 200], bright = new int[100 * 200];
    java.util.Arrays.fill(dark, Color.rgb(12, 24, 36));
    java.util.Arrays.fill(bright, Color.rgb(200, 220, 240));
    Bitmap a =
        GlassMaterial.render(
            path, 100, 200, 150, dark, 100, 200, 100, 200, 0, 0, Color.WHITE, 0, 2);
    Bitmap b =
        GlassMaterial.render(
            path, 100, 200, 150, bright, 100, 200, 100, 200, 0, 0, Color.WHITE, 0, 2);
    int core = brightness(a.getPixel(75, 150));
    int edge = brightness(a.getPixel(25, 150));
    assertTrue("The directional bevel must stand above the body", edge > core + 20);
    assertTrue(
        "The backdrop must remain visible through glass",
        brightness(b.getPixel(75, 150)) > core + 65);
    assertEquals(0, Color.alpha(a.getPixel(0, 0)));
    assertTrue(Color.alpha(a.getPixel(75, 150)) > 240);
    a.recycle();
    b.recycle();
  }

  @Test
  public void actualRendererProducesPhoneProofsForThreeScenesAndAllFinishes() throws Exception {
    Context c = RuntimeEnvironment.getApplication();
    File dir = new File("build/screenshots");
    dir.mkdirs();
    int[] themes = {32, 33, 36};
    String[] times = {"09:41", "16:28", "23:57"};
    for (int finish = 0; finish < 3; finish++)
      for (int i = 0; i < themes.length; i++) {
        Preferences p = new Preferences(c, "glass_proof");
        ThemeCatalog.apply(p, themes[i]);
        p.setNumber("glassFinish", finish);
        LockFace face = new LockFace(c, false, p);
        face.hideHint();
        face.clock.setFormat12Hour("'" + times[i] + "'");
        face.clock.setFormat24Hour("'" + times[i] + "'");
        face.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(720, 1073741824),
            android.view.View.MeasureSpec.makeMeasureSpec(1600, 1073741824));
        face.layout(0, 0, 720, 1600);
        Bitmap bitmap = Bitmap.createBitmap(720, 1600, Bitmap.Config.ARGB_8888);
        face.draw(new Canvas(bitmap));
        Bitmap material = org.robolectric.util.ReflectionHelpers.getField(face.clock, "surface");
        face.draw(new Canvas(bitmap));
        assertSame(
            "Unchanged frames must reuse material",
            material,
            org.robolectric.util.ReflectionHelpers.getField(face.clock, "surface"));
        try (FileOutputStream out =
            new FileOutputStream(
                new File(
                    dir, BuildConfig.FLAVOR + "-glass-" + themes[i] + "-" + finish + ".png"))) {
          assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        }
        face.clock.setFormat12Hour("'10:06'");
        face.clock.setFormat24Hour("'10:06'");
        face.draw(new Canvas(bitmap));
        if (finish != 2)
          assertNotSame(
              material, org.robolectric.util.ReflectionHelpers.getField(face.clock, "surface"));
        bitmap.recycle();
      }
  }

  private int brightness(int color) {
    return (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
  }
}
