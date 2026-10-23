package com.luma.lockscreen;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Swipe and accessibility-click share the same dismissal action. */
final class SwipeFrameLayout extends FrameLayout {
  public SwipeFrameLayout(Context context) {
    this(context, () -> {});
  }

  private final Runnable action;
  private float downY;

  SwipeFrameLayout(Context context, Runnable action) {
    super(context);
    this.action = action;
    setClickable(true);
    setContentDescription("Continue to device");
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      downY = event.getY();
      return true;
    }
    if (event.getAction() == MotionEvent.ACTION_UP) {
      if (downY - event.getY() > Ui.dp(getContext(), 80)) performClick();
      return true;
    }
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    action.run();
    return true;
  }
}
