package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.widget.TextClock;

/** Wallpaper-sampled glass fill with a light rim. Other styles use the platform text renderer. */
final class GlassClock extends TextClock {
  private final Preferences prefs;
  private final WallpaperView source;
  private Bitmap texture;
  private BitmapShader shader;
  private final Matrix matrix = new Matrix();
  private final PorterDuffColorFilter highlight =
      new PorterDuffColorFilter(0x66FFFFFF, PorterDuff.Mode.SRC_ATOP);

  public GlassClock(Context c) {
    this(c, new Preferences(c), new WallpaperView(c));
  }

  GlassClock(Context c, Preferences p, WallpaperView source) {
    super(c);
    prefs = p;
    this.source = source;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    Paint paint = getPaint();
    if (prefs.clock() == 0 && source.getWidth() > 0 && source.getHeight() > 0) {
      ensureTexture();
      matrix.setScale(
          source.getWidth() / (float) texture.getWidth(),
          source.getHeight() / (float) texture.getHeight());
      matrix.postTranslate(
          -getLeft() + 18, -getTop() - ((android.view.View) getParent()).getTop() + 22);
      shader.setLocalMatrix(matrix);
      paint.setShader(shader);
      paint.setColorFilter(highlight);
      super.onDraw(canvas);
      paint.setShader(null);
      paint.setColorFilter(null);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(Ui.dp(getContext(), 1.1f));
      super.onDraw(canvas);
      paint.setStyle(Paint.Style.FILL);
    } else if (prefs.clock() == 2) {
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(Ui.dp(getContext(), 1.3f));
      super.onDraw(canvas);
      paint.setStyle(Paint.Style.FILL);
    } else super.onDraw(canvas);
  }

  private void ensureTexture() {
    if (texture == null) {
      int width = Math.min(480, source.getWidth());
      texture =
          source.bitmap(
              width,
              Math.max(1, Math.round(width * source.getHeight() / (float) source.getWidth())));
      shader = new BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (texture != null) {
      texture.recycle();
      texture = null;
      shader = null;
    }
    super.onDetachedFromWindow();
  }
}
