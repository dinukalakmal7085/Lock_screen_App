package com.luma.lockscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.FrameLayout;

/** Clips artwork consistently in hardware rendering and software screenshots. */
final class RoundedFrameLayout extends FrameLayout {
  private final Path path = new Path();
  private final float radius;

  public RoundedFrameLayout(Context context) {
    this(context, 28);
  }

  RoundedFrameLayout(Context context, float radius) {
    super(context);
    this.radius = Ui.dp(context, radius);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    path.reset();
    path.addRoundRect(new RectF(0, 0, w, h), radius, radius, Path.Direction.CW);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    int save = canvas.save();
    canvas.clipPath(path);
    super.dispatchDraw(canvas);
    canvas.restoreToCount(save);
  }
}
