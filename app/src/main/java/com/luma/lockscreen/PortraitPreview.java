package com.luma.lockscreen;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** Scales the actual portrait face, keeping editor composition consistent with full screen. */
final class PortraitPreview extends FrameLayout {
  private final RoundedFrameLayout face;
  private final int faceWidth, faceHeight;

  public PortraitPreview(Context context) {
    this(context, new Preferences(context));
  }

  PortraitPreview(Context context, Preferences prefs) {
    this(context, prefs, false);
  }

  PortraitPreview(Context context, Preferences prefs, boolean sample) {
    super(context);
    android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
    faceWidth = Math.min(metrics.widthPixels, metrics.heightPixels);
    faceHeight = Math.max(metrics.widthPixels, metrics.heightPixels);
    face = new RoundedFrameLayout(context, 32);
    LockFace design = new LockFace(context, false, prefs, 480);
    design.hideHint();
    if (sample) {
      design.clock.setFormat12Hour("'09:41'");
      design.clock.setFormat24Hour("'09:41'");
    }
    face.addView(design, new LayoutParams(-1, -1));
    face.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    addView(face);
    setContentDescription("Portrait preview of " + prefs.wallpaperName() + " lock screen");
    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    setMeasuredDimension(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));
    face.measure(
        MeasureSpec.makeMeasureSpec(faceWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(faceHeight, MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    face.layout(0, 0, faceWidth, faceHeight);
    float scale = Math.min(getWidth() / (float) faceWidth, getHeight() / (float) faceHeight);
    face.setPivotX(0);
    face.setPivotY(0);
    face.setScaleX(scale);
    face.setScaleY(scale);
    face.setTranslationX((getWidth() - faceWidth * scale) / 2);
    face.setTranslationY((getHeight() - faceHeight * scale) / 2);
  }
}
