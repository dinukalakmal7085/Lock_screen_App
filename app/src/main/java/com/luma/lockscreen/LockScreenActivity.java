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
    configurePresentation();
    IntentFilter off = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    if (android.os.Build.VERSION.SDK_INT >= 33)
      registerReceiver(screenOff, off, Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(screenOff, off);
  }

  private final BroadcastReceiver screenOff =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          finish();
        }
      };

  private void configurePresentation() {
    boolean bridge = getIntent().getBooleanExtra("authenticate", false);
    // A normal preview must never reappear above keyguard on the next wake.
    if (android.os.Build.VERSION.SDK_INT >= 27) setShowWhenLocked(bridge);
    else if (bridge) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    LockScreenWindow.configure(getWindow());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    authenticating = false;
    configurePresentation();
  }

  @Override
  public void onWindowFocusChanged(boolean focused) {
    super.onWindowFocusChanged(focused);
    if (focused) LockScreenWindow.immersive(getWindow().getDecorView());
  }

  @Override
  protected void onDestroy() {
    unregisterReceiver(screenOff);
    super.onDestroy();
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
    LockScreenLayout root =
        new LockScreenLayout(
            this,
            !getIntent().getBooleanExtra("authenticate", false),
            this::unlock,
            () -> unlockThen(this::openCamera),
            EditionFeatures.HAS_COMPANION
                ? () ->
                    Toast.makeText(this, EditionFeatures.flashlight(this), Toast.LENGTH_SHORT)
                        .show()
                : null,
            () -> unlockThen(() -> startActivity(new Intent(this, SwitcherActivity.class))),
            this::finish);
    setContentView(root);
    LockScreenWindow.immersive(getWindow().getDecorView());
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
              finish();
            }

            @Override
            public void onDismissCancelled() {
              finish();
            }

            @Override
            public void onDismissError() {
              finish();
            }
          });
    else {
      after.run();
      finish();
    }
  }

  private void openCamera() {
    try {
      startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
    } catch (ActivityNotFoundException | SecurityException e) {
      Toast.makeText(this, "A camera app couldn't be opened.", Toast.LENGTH_LONG).show();
    }
  }
}
