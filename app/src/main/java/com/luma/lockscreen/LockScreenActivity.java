package com.luma.lockscreen;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

/** Full-screen design preview and the user-initiated system-authentication bridge. */
public final class LockScreenActivity extends Activity {
  private boolean authenticating;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (android.os.Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
    else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
    getWindow().setNavigationBarColor(0xFF11101C);
  }

  @Override
  protected void onResume() {
    super.onResume();
    render();
    if (getIntent().getBooleanExtra("authenticate", false) && !authenticating) {
      authenticating = true;
      getWindow().getDecorView().post(this::unlock);
    }
  }

  private void render() {
    FrameLayout root = new SwipeFrameLayout(this, this::unlock);
    root.setOnLongClickListener(
        v -> {
          unlockThen(() -> startActivity(new Intent(this, SwitcherActivity.class)));
          return true;
        });
    root.addView(new LockFace(this, false), new FrameLayout.LayoutParams(-1, -1));
    TextView close = Ui.button(this, "×", false, this::finish);
    close.setContentDescription("Close preview");
    close.setBackground(Ui.bordered(0x28000000, 0x25FFFFFF, 24, this));
    close.setAlpha(.85f);
    FrameLayout.LayoutParams cp =
        new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.TOP | Gravity.END);
    cp.setMargins(0, Ui.dp(this, 30), Ui.dp(this, 16), 0);
    root.addView(close, cp);
    TextView unlock = Ui.button(this, "⌃   Open device", false, this::unlock);
    unlock.setBackground(Ui.bordered(0x25FFFFFF, 0x55FFFFFF, 28, this));
    FrameLayout.LayoutParams up =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 190), Ui.dp(this, 54), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    up.bottomMargin = Ui.dp(this, 50);
    root.addView(unlock, up);
    TextView camera = Ui.button(this, "◉", false, () -> unlockThen(this::openCamera));
    camera.setContentDescription("Open camera after unlocking");
    camera.setBackground(Ui.bordered(0x44000000, 0x30FFFFFF, 26, this));
    FrameLayout.LayoutParams cameraP =
        new FrameLayout.LayoutParams(
            Ui.dp(this, 52), Ui.dp(this, 52), Gravity.BOTTOM | Gravity.END);
    cameraP.setMargins(0, 0, Ui.dp(this, 20), Ui.dp(this, 52));
    root.addView(camera, cameraP);
    if (EditionFeatures.HAS_COMPANION) {
      TextView torch =
          Ui.button(
              this,
              "ϟ",
              false,
              () ->
                  Toast.makeText(this, EditionFeatures.flashlight(this), Toast.LENGTH_SHORT)
                      .show());
      torch.setContentDescription("Toggle flashlight");
      torch.setBackground(Ui.bordered(0x44000000, 0x30FFFFFF, 26, this));
      FrameLayout.LayoutParams tp =
          new FrameLayout.LayoutParams(
              Ui.dp(this, 52), Ui.dp(this, 52), Gravity.BOTTOM | Gravity.START);
      tp.setMargins(Ui.dp(this, 20), 0, 0, Ui.dp(this, 52));
      root.addView(torch, tp);
    }
    TextView info = Ui.text(this, "Hold to switch looks", 11, 0xCCFFFFFF);
    info.setGravity(Gravity.CENTER);
    FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
    ip.bottomMargin = Ui.dp(this, 112);
    root.addView(info, ip);
    setContentView(root);
  }

  private void unlock() {
    String destination = getIntent().getStringExtra("destination");
    unlockThen(
        () -> {
          if ("switcher".equals(destination))
            startActivity(new Intent(this, SwitcherActivity.class));
          else if ("camera".equals(destination)) openCamera();
          finish();
        });
  }

  private void unlockThen(Runnable after) {
    KeyguardManager keyguard = getSystemService(KeyguardManager.class);
    if (keyguard != null && keyguard.isKeyguardLocked())
      keyguard.requestDismissKeyguard(
          this,
          new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
              after.run();
            }
          });
    else after.run();
  }

  private void openCamera() {
    try {
      startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
    } catch (ActivityNotFoundException | SecurityException e) {
      Toast.makeText(this, "A camera app couldn't be opened.", Toast.LENGTH_LONG).show();
    }
  }
}
