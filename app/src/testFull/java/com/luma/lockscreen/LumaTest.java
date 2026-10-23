package com.luma.lockscreen;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LumaTest {
  private Context context;

  @Before
  public void reset() {
    context = RuntimeEnvironment.getApplication();
    context.getSharedPreferences("luma", 0).edit().clear().commit();
  }

  @Test
  public void customizationSurvivesActivityRecreation() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity a = c.get();
      findDescription(a.getWindow().getDecorView(), "Preview Tide theme").performClick();
      try (ActivityController<ThemePreviewActivity> theme =
          Robolectric.buildActivity(
                  ThemePreviewActivity.class, shadowOf(a).getNextStartedActivity())
              .setup()) {
        findDescription(theme.get().getWindow().getDecorView(), "Use preview theme").performClick();
      }
      findDescription(a.getWindow().getDecorView(), "Classic clock style").performClick();
      assertEquals(1, new Preferences(a).clock());
      assertEquals(2, new Preferences(a).wallpaper());
      c.recreate();
      assertEquals(1, new Preferences(c.get()).clock());
      assertNotNull(findText(c.get().getWindow().getDecorView(), "Tide  /  Classic"));
    }
  }

  @Test
  public void tabsAndFullScreenPreviewAreRealNavigation() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      View decor = c.get().getWindow().getDecorView();
      findDescription(decor, "Wallpapers tab").performClick();
      assertNotNull(findText(c.get().getWindow().getDecorView(), "Find your signature."));
      findDescription(c.get().getWindow().getDecorView(), "Preview Ember theme").performClick();
      try (ActivityController<ThemePreviewActivity> theme =
          Robolectric.buildActivity(
                  ThemePreviewActivity.class, shadowOf(c.get()).getNextStartedActivity())
              .setup()) {
        findDescription(theme.get().getWindow().getDecorView(), "Use preview theme").performClick();
      }
      assertEquals(3, new Preferences(c.get()).wallpaper());
      findText(c.get().getWindow().getDecorView(), "Preview selected wallpaper  ↗").performClick();
      Intent next = shadowOf(c.get()).getNextStartedActivity();
      assertEquals(LockScreenActivity.class.getName(), next.getComponent().getClassName());
      findDescription(c.get().getWindow().getDecorView(), "Settings tab").performClick();
      assertNotNull(findText(c.get().getWindow().getDecorView(), "Set up lock screen"));
    }
  }

  @Test
  public void dateAndBatteryCanBeHiddenAndClockFormatChanges() {
    Preferences p = new Preferences(context);
    p.setFlag("date", false);
    p.setFlag("battery", false);
    p.setFlag("24hour", true);
    LockFace face = new LockFace(context, false);
    assertEquals("HH:mm", face.clock.getFormat12Hour());
    assertTrue(countVisibleContaining(face, "Battery") == 0);
    p.setFlag("24hour", false);
    assertEquals("h:mm", new LockFace(context, false).clock.getFormat24Hour());
  }

  @Test
  public void artworkRendersDistinctWallpapersAndLoadsLocalPhoto() throws Exception {
    int[] pixels = new int[6];
    for (int i = 0; i < 6; i++) {
      Bitmap b = new WallpaperView(context, i).bitmap(240, 500);
      assertEquals(240, b.getWidth());
      pixels[i] = b.getPixel(100, 200);
      b.recycle();
    }
    for (int i = 1; i < 6; i++) assertNotEquals(pixels[0], pixels[i]);
    Bitmap photo = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    photo.eraseColor(Color.RED);
    try (FileOutputStream out =
        new FileOutputStream(new File(context.getFilesDir(), "wallpaper.jpg"))) {
      photo.compress(Bitmap.CompressFormat.JPEG, 95, out);
    }
    photo.recycle();
    WallpaperView view = new WallpaperView(context, 0);
    view.loadPhoto();
    Bitmap actual = view.bitmap(200, 400);
    int center = actual.getPixel(100, 200);
    assertTrue(Color.red(center) > 150);
    assertTrue(Color.blue(center) < 20);
    actual.recycle();
  }

  @Test
  public void notificationListenerFiltersSecretsAndRemovesDismissedEntries() {
    LumaNotificationListener listener =
        Robolectric.buildService(LumaNotificationListener.class).create().get();
    listener.onListenerDisconnected();
    Notification secret =
        new Notification.Builder(context, "test")
            .setContentTitle("Secret code")
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build();
    listener.onNotificationPosted(notification(1, secret));
    assertTrue(LumaNotificationListener.entries().isEmpty());
    Notification privateNote =
        new Notification.Builder(context, "test")
            .setContentTitle("Private code")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build();
    StatusBarNotification sbn = notification(2, privateNote);
    listener.onNotificationPosted(sbn);
    assertEquals("New notification", LumaNotificationListener.entries().get(0).title);
    listener.onNotificationRemoved(sbn);
    assertTrue(LumaNotificationListener.entries().isEmpty());
    for (int i = 3; i < 8; i++)
      listener.onNotificationPosted(
          notification(
              i,
              new Notification.Builder(context, "test")
                  .setContentTitle("Public " + i)
                  .setVisibility(Notification.VISIBILITY_PUBLIC)
                  .build()));
    assertEquals(3, LumaNotificationListener.entries().size());
    assertEquals("Public 7", LumaNotificationListener.entries().get(0).title);
    listener.onListenerDisconnected();
    assertTrue(LumaNotificationListener.entries().isEmpty());
  }

  @Test
  public void updatedNotificationsMoveToFrontAndSecretUpdatesRemoveCachedTitles() {
    LumaNotificationListener listener =
        Robolectric.buildService(LumaNotificationListener.class).create().get();
    listener.onListenerDisconnected();
    for (int i = 1; i <= 2; i++)
      listener.onNotificationPosted(
          notification(
              i,
              new Notification.Builder(context, "test")
                  .setContentTitle("Public " + i)
                  .setVisibility(Notification.VISIBILITY_PUBLIC)
                  .build()));
    listener.onNotificationPosted(
        notification(
            1,
            new Notification.Builder(context, "test")
                .setContentTitle("Updated first")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()));
    assertEquals("Updated first", LumaNotificationListener.entries().get(0).title);
    listener.onNotificationPosted(
        notification(
            1,
            new Notification.Builder(context, "test")
                .setContentTitle("Now secret")
                .setVisibility(Notification.VISIBILITY_SECRET)
                .build()));
    assertEquals(1, LumaNotificationListener.entries().size());
    assertEquals("Public 2", LumaNotificationListener.entries().get(0).title);
    listener.onListenerDisconnected();
  }

  @Test
  public void automaticScreenOnlyShowsForLockedWakeAndYieldsToSystemPrompts() {
    org.robolectric.android.controller.ServiceController<LockScreenService> controller =
        Robolectric.buildService(LockScreenService.class).create();
    try {
      LockScreenService service = controller.get();
      service.onServiceConnected();
      WindowManager wm = service.getSystemService(WindowManager.class);
      org.robolectric.shadows.ShadowWindowManagerImpl windows =
          org.robolectric.shadow.api.Shadow.extract(wm);
      shadowOf(service.getSystemService(android.os.PowerManager.class)).setIsInteractive(true);
      shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
      service.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(700));
      assertTrue(windows.getViews().isEmpty());
      shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
      service.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(700));
      assertEquals(1, windows.getViews().size());
      android.view.accessibility.AccessibilityEvent prompt =
          android.view.accessibility.AccessibilityEvent.obtain(
              android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
      prompt.setClassName("com.android.systemui.CredentialBouncer");
      service.onAccessibilityEvent(prompt);
      assertTrue(windows.getViews().isEmpty());
      service.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(700));
      assertEquals(1, windows.getViews().size());
      service.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));
      shadowOf(Looper.getMainLooper()).idle();
      assertTrue(windows.getViews().isEmpty());
    } finally {
      controller.destroy();
    }
  }

  @Test
  public void enablingNotificationsRequiresExplicitAndroidAccess() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      findDescription(c.get().getWindow().getDecorView(), "Settings tab").performClick();
      Switch toggle =
          (Switch) findDescription(c.get().getWindow().getDecorView(), "Show notifications");
      toggle.setChecked(true);
      assertFalse(new Preferences(context).flag("notifications", false));
      assertNotNull(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
    }
  }

  @Test
  public void previewCanBeClosedWithoutChangingDeviceSecurity() {
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class).setup()) {
      findDescription(c.get().getWindow().getDecorView(), "Close preview").performClick();
      assertTrue(c.get().isFinishing());
    }
  }

  @Test
  @Config(sdk = {26, 36})
  public void editorAndPreviewLaunchOnMinimumAndTargetAndroid() {
    try (ActivityController<MainActivity> editor =
            Robolectric.buildActivity(MainActivity.class).setup();
        ActivityController<LockScreenActivity> preview =
            Robolectric.buildActivity(LockScreenActivity.class).setup()) {
      assertNotNull(findText(editor.get().getWindow().getDecorView(), "A little more you."));
      assertNotNull(findText(preview.get().getWindow().getDecorView(), "⌃   Open device"));
    }
  }

  @Test
  public void lockedDeviceNeverShowsPublicNotificationTitles() {
    Preferences p = new Preferences(context);
    p.setFlag("notifications", true);
    p.setFlag("details", true);
    LumaNotificationListener listener =
        Robolectric.buildService(LumaNotificationListener.class).create().get();
    listener.onListenerDisconnected();
    listener.onNotificationPosted(
        notification(
            12,
            new Notification.Builder(context, "test")
                .setContentTitle("Public title")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()));
    shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(true);
    LockFace locked = new LockFace(context, false);
    assertNull(findText(locked, "Public title"));
    assertNotNull(findText(locked, "New notification"));
    shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(false);
    assertNotNull(findText(new LockFace(context, false), "Public title"));
    listener.onListenerDisconnected();
  }

  @Test
  public void accessibilityServiceCannotReadScreenOrPerformGestures() throws Exception {
    android.content.pm.ServiceInfo info =
        context
            .getPackageManager()
            .getServiceInfo(
                new ComponentName(context, LockScreenService.class),
                android.content.pm.PackageManager.GET_META_DATA);
    assertEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", info.permission);
    try (android.content.res.XmlResourceParser parser =
        info.loadXmlMetaData(context.getPackageManager(), "android.accessibilityservice")) {
      while (parser.next() != org.xmlpull.v1.XmlPullParser.START_TAG) {}
      assertFalse(
          parser.getAttributeBooleanValue(
              "http://schemas.android.com/apk/res/android", "canRetrieveWindowContent", true));
      assertFalse(
          parser.getAttributeBooleanValue(
              "http://schemas.android.com/apk/res/android", "canPerformGestures", true));
      assertFalse(
          parser.getAttributeBooleanValue(
              "http://schemas.android.com/apk/res/android", "isAccessibilityTool", true));
    }
    new Preferences(context).setFlag("enabled", true);
    EditionFeatures.stop(context);
    assertFalse(new Preferences(context).flag("enabled", true));
  }

  @Test
  public void rendersEditorAndCollectionScreenshots() throws Exception {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup().visible()) {
      capture(c.get(), "studio");
      findDescription(c.get().getWindow().getDecorView(), "Wallpapers tab").performClick();
      capture(c.get(), "wallpapers");
      findDescription(c.get().getWindow().getDecorView(), "Settings tab").performClick();
      capture(c.get(), "settings");
    }
    try (ActivityController<LockScreenActivity> c =
        Robolectric.buildActivity(LockScreenActivity.class).setup().visible()) {
      capture(c.get(), "preview");
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
    assertTrue(dir.exists() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(b.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    b.recycle();
  }

  private StatusBarNotification notification(int id, Notification n) {
    return new StatusBarNotification(
        "com.example.messages",
        "com.example.messages",
        id,
        null,
        123,
        0,
        0,
        n,
        android.os.UserHandle.getUserHandleForUid(123),
        System.currentTimeMillis());
  }

  private int countVisibleContaining(View view, String value) {
    int n =
        view instanceof TextView
                && view.getVisibility() == View.VISIBLE
                && ((TextView) view).getText().toString().contains(value)
            ? 1
            : 0;
    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      for (int i = 0; i < g.getChildCount(); i++)
        n += countVisibleContaining(g.getChildAt(i), value);
    }
    return n;
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
