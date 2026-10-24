package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.TextClock;

/** Live clock with cached, wallpaper-aware glass. No timer or per-frame material generation. */
final class GlassClock extends TextClock {
  private final Preferences prefs;
  private final WallpaperView source;
  private final ClockNumerals numerals = new ClockNumerals();
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Path glyphs = new Path(), rawGlyphs = new Path();
  private final RectF bounds = new RectF(), destination = new RectF(), bitmapBounds = new RectF();
  private final Matrix matrix = new Matrix();
  private final android.text.TextPaint lettering =
      new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
  private Bitmap surface;
  private int[] backdrop;
  private int backdropWidth, backdropHeight, sourceWidth, sourceHeight;
  private String drawnText = "";
  private int drawnWidth, drawnHeight, drawnStyle = -1, drawnLeft, drawnTop, drawnInk, drawnWeight;
  private final int finish;

  public GlassClock(Context c) {
    this(c, new Preferences(c), new WallpaperView(c));
  }

  GlassClock(Context c, Preferences p, WallpaperView wallpaper) {
    super(c);
    prefs = p;
    source = wallpaper;
    finish = Math.max(0, Math.min(2, p.number("glassFinish", 1)));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int style = prefs.clock();
    if ((style != 0 && style != 6) || source.getWidth() == 0 || source.getHeight() == 0) {
      if (style == 2) {
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(Ui.dp(getContext(), 1.3f));
      }
      super.onDraw(canvas);
      getPaint().setStyle(Paint.Style.FILL);
      return;
    }
    prepareSurface(style);
    if (finish == 2) {
      paint.setColor(prefs.ink());
      canvas.drawPath(glyphs, paint);
    } else if (surface != null) {
      bitmapBounds.set(0, 0, getWidth(), getHeight());
      canvas.drawBitmap(surface, null, bitmapBounds, paint);
    }
  }

  private void prepareSurface(int style) {
    int left = getLeft(), top = getTop();
    android.view.ViewParent ancestor = getParent();
    while (ancestor instanceof View && ancestor != source.getParent()) {
      left += ((View) ancestor).getLeft();
      top += ((View) ancestor).getTop();
      ancestor = ancestor.getParent();
    }
    String text = getText().toString();
    int weight = Math.max(60, Math.min(100, prefs.number("clockWidth", 100)));
    if (text.equals(drawnText)
        && drawnWidth == getWidth()
        && drawnHeight == getHeight()
        && drawnStyle == style
        && drawnLeft == left
        && drawnTop == top
        && drawnInk == prefs.ink()
        && drawnWeight == weight
        && sourceWidth == source.getWidth()
        && sourceHeight == source.getHeight()
        && (finish == 2 || surface != null)) return;
    drawnText = text;
    drawnWidth = getWidth();
    drawnHeight = getHeight();
    drawnStyle = style;
    drawnLeft = left;
    drawnTop = top;
    drawnInk = prefs.ink();
    drawnWeight = weight;
    rawGlyphs.reset();
    boolean custom = style == 6 && numerals.build(text, rawGlyphs);
    if (!custom) {
      // Preserve platform shaping for other styles and localized non-Latin numerals.
      lettering.setTypeface(
          Typeface.create(
              style == 6 ? "sans-serif-condensed-light" : "sans-serif-black", Typeface.NORMAL));
      lettering.setTextSize(100);
      lettering.getTextPath(text, 0, text.length(), 0, 100, rawGlyphs);
    }
    if (custom) numerals.bounds(bounds);
    else rawGlyphs.computeBounds(bounds, true);
    glyphs.reset();
    if (bounds.isEmpty() || getWidth() <= 0 || getHeight() <= 0) return;
    float available = Math.max(1, getWidth() - Ui.dp(getContext(), 6));
    float height =
        style == 6 ? getHeight() * .90f : Math.min(getHeight() * .84f, getTextSize() * .80f);
    float width =
        style == 6
            ? available * .90f
            : Math.min(available, bounds.width() * height / bounds.height());
    width *= weight / 100f;
    int align = prefs.number("alignment", 1);
    float x = align == 0 ? 0 : align == 2 ? getWidth() - width : (getWidth() - width) / 2;
    float y = (getHeight() - height) / 2;
    destination.set(x, y, x + width, y + height);
    matrix.setRectToRect(bounds, destination, Matrix.ScaleToFit.FILL);
    rawGlyphs.transform(matrix, glyphs);
    if (surface != null) {
      surface.recycle();
      surface = null;
    }
    if (finish == 2) {
      sourceWidth = source.getWidth();
      sourceHeight = source.getHeight();
      return;
    }
    ensureBackdrop();
    int rasterWidth =
        Math.min(Math.round(getWidth() * 1.5f), Math.round(source.glassTextureWidth() * 2.25f));
    // Full faces retain native pixels; gallery material buffers stay small.
    rasterWidth = Math.min(rasterWidth, (int) Math.sqrt(1200000f * getWidth() / getHeight()));
    surface =
        GlassMaterial.render(
            glyphs,
            getWidth(),
            getHeight(),
            Math.max(1, rasterWidth),
            backdrop,
            backdropWidth,
            backdropHeight,
            sourceWidth,
            sourceHeight,
            left,
            top,
            prefs.ink(),
            finish,
            getResources().getDisplayMetrics().density);
  }

  private void ensureBackdrop() {
    if (backdrop != null && sourceWidth == source.getWidth() && sourceHeight == source.getHeight())
      return;
    sourceWidth = source.getWidth();
    sourceHeight = source.getHeight();
    backdropWidth = source.glassTextureWidth();
    backdropHeight = Math.max(1, Math.round(backdropWidth * sourceHeight / (float) sourceWidth));
    Bitmap image = source.bitmap(backdropWidth, backdropHeight);
    backdrop = new int[backdropWidth * backdropHeight];
    image.getPixels(backdrop, 0, backdropWidth, 0, 0, backdropWidth, backdropHeight);
    image.recycle();
  }

  void prepareTexture() {
    if (source.getWidth() > 0
        && source.getHeight() > 0
        && getWidth() > 0
        && getHeight() > 0
        && (prefs.clock() == 0 || prefs.clock() == 6)) prepareSurface(prefs.clock());
  }

  @Override
  protected void onDetachedFromWindow() {
    if (surface != null) {
      surface.recycle();
      surface = null;
    }
    backdrop = null;
    super.onDetachedFromWindow();
  }
}
