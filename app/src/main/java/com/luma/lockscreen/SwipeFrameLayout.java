package com.luma.lockscreen;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Swipe and accessibility-click share the same dismissal action. */
class SwipeFrameLayout extends FrameLayout {
  public SwipeFrameLayout(Context context) {
    this(context, () -> {});
  }

  private final Runnable action;
  private float downY;
  private float downX;
  private final android.os.Handler handler =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private boolean held;
  private final Runnable longPress =
      () -> {
        held = performLongClick();
      };

  SwipeFrameLayout(Context context, Runnable action) {
    super(context);
    this.action = action;
    setClickable(true);
    setContentDescription("Continue to device");
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      downY = event.getY();
      downX = event.getX();
      held = false;
    }
    // A swipe starting on a shortcut must still reach the common unlock action.
    return event.getActionMasked() == MotionEvent.ACTION_MOVE
        && downY - event.getY() > Ui.dp(getContext(), 12)
        && downY - event.getY() > Math.abs(downX - event.getX());
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      downY = event.getY();
      downX = event.getX();
      held = false;
      handler.postDelayed(longPress, android.view.ViewConfiguration.getLongPressTimeout());
      return true;
    }
    if (event.getAction() == MotionEvent.ACTION_MOVE
        && (Math.abs(event.getY() - downY) > Ui.dp(getContext(), 12)
            || Math.abs(event.getX() - downX) > Ui.dp(getContext(), 12)))
      handler.removeCallbacks(longPress);
    if (event.getAction() == MotionEvent.ACTION_CANCEL) handler.removeCallbacks(longPress);
    if (event.getAction() == MotionEvent.ACTION_UP) {
      handler.removeCallbacks(longPress);
      if (!held
          && downY - event.getY() > Ui.dp(getContext(), 80)
          && downY - event.getY() > Math.abs(downX - event.getX())) performClick();
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

  @Override
  protected void onDetachedFromWindow() {
    handler.removeCallbacks(longPress);
    super.onDetachedFromWindow();
  }
}
