package com.luma.lockscreen;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.KeyguardManager;
import android.content.*;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LockScreenPresentationTest {
  private Context context;

  @Before
  public void reset() {
    context = RuntimeEnvironment.getApplication();
    context.getSharedPreferences("luma", 0).edit().clear().commit();
  }

  @Test
  @Config(sdk = {26, 35})
  public void previewClosesOnSleepAndRequestsFullScreen() {
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class).setup().visible()) {
      assertTrue(
          (c.get().getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN)
              != 0);
      assertNotEquals(
          0,
          c.get().getWindow().getDecorView().getSystemUiVisibility()
              & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
      c.get().sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));
      shadowOf(Looper.getMainLooper()).idle();
      assertTrue(
          "A preview must not survive sleep and reappear over keyguard", c.get().isFinishing());
    }
  }

  @Test
  public void authenticationCancellationClosesBridgeWithoutOpeningDestination() {
    shadowOf(context.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
    Intent intent =
        new Intent(context, LockScreenActivity.class)
            .putExtra("authenticate", true)
            .putExtra("destination", "camera");
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class, intent).setup().visible()) {
      shadowOf(Looper.getMainLooper()).idle();
      assertFalse(c.get().isFinishing());
      shadowOf(c.get().getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
      assertTrue(c.get().isFinishing());
      assertNull(shadowOf(c.get()).getNextStartedActivity());
    }
  }

  @Test
  public void swipingFromShortcutUnlocksOnceWithoutLaunchingCamera() {
    AtomicInteger unlocks = new AtomicInteger(), cameras = new AtomicInteger();
    LockScreenLayout root =
        new LockScreenLayout(
            context,
            false,
            unlocks::incrementAndGet,
            cameras::incrementAndGet,
            () -> {},
            () -> {},
            () -> {});
    root.prepare(720, 1600);
    View camera = find(root, "Open camera after unlocking");
    Rect bounds = new Rect();
    camera.getDrawingRect(bounds);
    root.offsetDescendantRectToMyCoords(camera, bounds);
    float x = bounds.centerX(), y = bounds.centerY();
    touch(root, MotionEvent.ACTION_DOWN, x, y, 0);
    touch(root, MotionEvent.ACTION_MOVE, x, y - 80, 20);
    touch(root, MotionEvent.ACTION_UP, x, y - 240, 40);
    assertEquals(1, unlocks.get());
    assertEquals(0, cameras.get());
    touch(root, MotionEvent.ACTION_DOWN, x, y, 100);
    touch(root, MotionEvent.ACTION_MOVE, x - 250, y - 5, 120);
    touch(root, MotionEvent.ACTION_UP, x - 300, y - 10, 140);
    assertEquals(1, unlocks.get());
  }

  @Test
  public void controlsStayInsideNavigationInsetsAndCloudwalkerRendersAtPhoneResolution()
      throws Exception {
    ThemeCatalog.apply(new Preferences(context), 33);
    new Preferences(context).setFlag("date", true);
    LockScreenLayout root =
        new LockScreenLayout(context, false, () -> {}, () -> {}, () -> {}, () -> {}, () -> {});
    root.dispatchApplyWindowInsets(
        new WindowInsets.Builder().setSystemWindowInsets(Insets.of(0, 60, 0, 48)).build());
    root.prepare(720, 1600);
    root.face.clock.setFormat12Hour("'05:27'");
    root.face.clock.setFormat24Hour("'05:27'");
    for (String label :
        new String[] {"Toggle flashlight", "Open camera after unlocking", "Open device"}) {
      View v = find(root, label);
      Rect rect = new Rect();
      v.getDrawingRect(rect);
      root.offsetDescendantRectToMyCoords(v, rect);
      assertTrue(label, rect.bottom <= 1552);
      assertTrue(label, rect.left >= 0 && rect.right <= 720);
    }
    Bitmap bitmap = Bitmap.createBitmap(720, 1600, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(bitmap));
    File dir = new File("build/screenshots");
    dir.mkdirs();
    try (FileOutputStream out =
        new FileOutputStream(new File(dir, BuildConfig.FLAVOR + "-redmi-cloudwalker.png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }

  @Test
  public void clearGlassHasVisibleFillOnDarkSky() {
    Preferences p = new Preferences(context);
    ThemeCatalog.apply(p, 33);
    LockFace face = new LockFace(context, false, p);
    face.measure(
        View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY));
    face.layout(0, 0, 720, 1600);
    Bitmap clock =
        Bitmap.createBitmap(face.clock.getWidth(), face.clock.getHeight(), Bitmap.Config.ARGB_8888);
    face.clock.draw(new Canvas(clock));
    int filled = 0;
    long brightness = 0;
    // The top third sits against the dark blue sky, away from bright edges and foreground.
    for (int y = 30; y < clock.getHeight() / 3; y += 2)
      for (int x = 10; x < clock.getWidth() - 10; x += 2) {
        int pixel = clock.getPixel(x, y);
        if (Color.alpha(pixel) > 240) {
          brightness += (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
          filled++;
        }
      }
    assertTrue(filled > 300);
    assertTrue("Clock fill must remain readable against the sky", brightness / filled > 100);
    clock.recycle();
  }

  private void touch(View view, int action, float x, float y, long time) {
    MotionEvent e = MotionEvent.obtain(0, time, action, x, y, 0);
    view.dispatchTouchEvent(e);
    e.recycle();
  }

  private View find(View view, String label) {
    if (label.contentEquals(
        view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View found = find(((ViewGroup) view).getChildAt(i), label);
        if (found != null) return found;
      }
    return null;
  }
}
