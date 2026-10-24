package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** Shared full-screen composition, with real vector shortcuts and one unlock affordance. */
@android.annotation.SuppressLint("ViewConstructor") // Built in code with explicit action callbacks.
final class LockScreenLayout extends SwipeFrameLayout {
  final LockFace face;
  private final View close;
  private final View handle;
  private final LinearLayout bottom;

  LockScreenLayout(
      Context c,
      boolean preview,
      Runnable unlock,
      Runnable camera,
      Runnable torch,
      Runnable switchLooks,
      Runnable dismiss) {
    super(c, unlock);
    setOnLongClickListener(
        v -> {
          switchLooks.run();
          return true;
        });
    face = new LockFace(c, false);
    face.hideHint();
    addView(face, new LayoutParams(-1, -1));
    View shade = new View(c);
    shade.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    shade.setBackground(
        new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {0x00000000, 0x70030912}));
    addView(shade, new LayoutParams(-1, Ui.dp(c, 190), Gravity.BOTTOM));

    bottom = Ui.column(c);
    LinearLayout row = Ui.row(c);
    row.setGravity(Gravity.CENTER_VERTICAL);
    if (torch != null)
      row.addView(
          shortcut(c, 0, "Toggle flashlight", torch),
          new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
    else row.addView(new View(c), new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
    View spacer = new View(c);
    row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
    row.addView(
        shortcut(c, 1, "Open camera after unlocking", camera),
        new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
    bottom.addView(row, new LinearLayout.LayoutParams(-1, -2));
    TextView swipe = Ui.text(c, "Swipe up to unlock", 13, 0xF2FFFFFF);
    swipe.setGravity(Gravity.CENTER);
    swipe.setShadowLayer(4, 0, 1, 0xAA000000);
    swipe.setContentDescription("Open device");
    swipe.setOnClickListener(v -> unlock.run());
    bottom.addView(swipe, new LinearLayout.LayoutParams(-1, Ui.dp(c, 48)));
    handle = new View(c);
    handle.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    handle.setBackground(Ui.bordered(0xE6FFFFFF, 0, 3, c));
    LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(Ui.dp(c, 110), Ui.dp(c, 4));
    hp.gravity = Gravity.CENTER_HORIZONTAL;
    bottom.addView(handle, hp);
    addView(bottom, new LayoutParams(-1, -2, Gravity.BOTTOM));
    close = new Icon(c, 2);
    close.setContentDescription(preview ? "Close preview" : "Close Luma lock screen");
    close.setOnClickListener(v -> dismiss.run());
    addView(close, new LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48), Gravity.TOP | Gravity.END));
    positionControls(0, 0, 0, 0);
    setOnApplyWindowInsetsListener(
        (v, insets) -> {
          int left = insets.getSystemWindowInsetLeft(), right = insets.getSystemWindowInsetRight();
          int top = insets.getSystemWindowInsetTop(),
              bottomInset = insets.getSystemWindowInsetBottom();
          if (android.os.Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
            android.view.DisplayCutout cutout = insets.getDisplayCutout();
            left = Math.max(left, cutout.getSafeInsetLeft());
            right = Math.max(right, cutout.getSafeInsetRight());
            top = Math.max(top, cutout.getSafeInsetTop());
            bottomInset = Math.max(bottomInset, cutout.getSafeInsetBottom());
          }
          positionControls(left, top, right, bottomInset);
          return insets;
        });
  }

  private void positionControls(int left, int top, int right, int bottomInset) {
    Context c = getContext();
    // Do not draw a second home indicator above Android's visible navigation area.
    handle.setVisibility(bottomInset > 0 ? GONE : VISIBLE);
    bottom.setPadding(
        Ui.dp(c, 26) + left,
        0,
        Ui.dp(c, 26) + right,
        Math.max(Ui.dp(c, 12), bottomInset + Ui.dp(c, 6)));
    LayoutParams cp = (LayoutParams) close.getLayoutParams();
    cp.topMargin = top + Ui.dp(c, 8);
    cp.rightMargin = right + Ui.dp(c, 12);
    close.setLayoutParams(cp);
  }

  private View shortcut(Context c, int kind, String label, Runnable action) {
    View icon = new Icon(c, kind);
    icon.setContentDescription(label);
    icon.setBackground(Ui.bordered(0x55090E18, 0x35FFFFFF, 24, c));
    icon.setOnClickListener(v -> action.run());
    return icon;
  }

  void prepare(int width, int height) {
    measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    layout(0, 0, width, height);
    face.wallpaper.preload(width);
    ((GlassClock) face.clock).prepareTexture();
  }

  private static final class Icon extends View {
    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline = new Path();
    private final int kind;

    Icon(Context c, int kind) {
      super(c);
      this.kind = kind;
      setFocusable(true);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
      return Button.class.getName();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.save();
      canvas.translate(getWidth() / 2f, getHeight() / 2f);
      float scale = Ui.dp(getContext(), 24) / 24f;
      canvas.scale(scale, scale);
      pen.setColor(isPressed() ? 0xFFB7DAFF : 0xFFF6F8FF);
      pen.setStyle(Paint.Style.STROKE);
      pen.setStrokeWidth(1.7f);
      pen.setStrokeJoin(Paint.Join.ROUND);
      pen.setStrokeCap(Paint.Cap.ROUND);
      if (kind == 0) {
        Path p = outline;
        p.reset();
        p.moveTo(-6, -9);
        p.lineTo(6, -9);
        p.lineTo(6, -5);
        p.lineTo(3, -1);
        p.lineTo(3, 9);
        p.lineTo(-3, 9);
        p.lineTo(-3, -1);
        p.lineTo(-6, -5);
        p.close();
        canvas.drawPath(p, pen);
        canvas.drawLine(-5, -5, 5, -5, pen);
        canvas.drawLine(0, 1, 0, 4, pen);
      } else if (kind == 1) {
        Path p = outline;
        p.reset();
        p.moveTo(-9, -5);
        p.lineTo(-4, -5);
        p.lineTo(-2, -8);
        p.lineTo(3, -8);
        p.lineTo(5, -5);
        p.lineTo(9, -5);
        p.quadTo(10, -5, 10, -3);
        p.lineTo(10, 6);
        p.quadTo(10, 8, 8, 8);
        p.lineTo(-8, 8);
        p.quadTo(-10, 8, -10, 6);
        p.lineTo(-10, -3);
        p.close();
        canvas.drawPath(p, pen);
        canvas.drawCircle(0, 1, 4, pen);
      } else {
        canvas.drawLine(-4, -4, 4, 4, pen);
        canvas.drawLine(-4, 4, 4, -4, pen);
      }
      canvas.restore();
    }
  }
}
