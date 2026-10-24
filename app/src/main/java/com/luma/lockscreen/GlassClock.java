package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.widget.TextClock;

/** Live TextClock time drawn as refracted glass paths; no time is baked into the artwork. */
final class GlassClock extends TextClock {
  private final Preferences prefs;
  private final WallpaperView source;
  private final Paint material = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path glyphs = new Path();
  private final Path rawGlyphs = new Path();
  private final RectF bounds = new RectF(), destination = new RectF();
  private final Matrix matrix = new Matrix();
  private final android.text.TextPaint lettering =
      new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
  private Bitmap texture;
  private BitmapShader shader;
  private Shader edgeShader;
  private int textureWidth, textureHeight;
  private String drawnText = "";
  private int drawnWidth, drawnHeight, drawnStyle = -1;
  private final int finish;

  public GlassClock(Context c) {
    this(c, new Preferences(c), new WallpaperView(c));
  }

  GlassClock(Context c, Preferences p, WallpaperView wallpaper) {
    super(c);
    prefs = p;
    source = wallpaper;
    finish = Math.max(0, Math.min(2, p.number("glassFinish", 1)));
    material.setColorFilter(
        new PorterDuffColorFilter(
            (p.ink() & 0xffffff) | (finish == 0 ? 0x62000000 : 0xA0000000),
            PorterDuff.Mode.SRC_ATOP));
    material.setFilterBitmap(true);
    material.setDither(true);
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
    updateGlyphs(style);
    if (finish == 2) {
      material.setShader(null);
      material.setColorFilter(null);
      material.setColor(prefs.ink());
    } else {
      ensureTexture();
      matrix.setScale(
          source.getWidth() / (float) texture.getWidth(),
          source.getHeight() / (float) texture.getHeight());
      matrix.postTranslate(
          -getLeft() + Ui.dp(getContext(), 9),
          -getTop() - ((android.view.View) getParent()).getTop() + Ui.dp(getContext(), 6));
      shader.setLocalMatrix(matrix);
      material.setShader(shader);
    }
    material.setStyle(Paint.Style.FILL);
    canvas.drawPath(glyphs, material);
    if (finish == 2) return;
    canvas.drawPath(glyphs, gloss);
    rim.setShader(null);
    rim.setColor(0x30070D18);
    rim.setStyle(Paint.Style.STROKE);
    rim.setStrokeWidth(Ui.dp(getContext(), 2f));
    canvas.drawPath(glyphs, rim);
    rim.setShader(edgeShader);
    rim.setStrokeWidth(Ui.dp(getContext(), 1.05f));
    canvas.drawPath(glyphs, rim);
  }

  private void updateGlyphs(int style) {
    String text = getText().toString();
    if (text.equals(drawnText)
        && drawnWidth == getWidth()
        && drawnHeight == getHeight()
        && drawnStyle == style) return;
    drawnText = text;
    drawnWidth = getWidth();
    drawnHeight = getHeight();
    drawnStyle = style;
    lettering.setTypeface(
        Typeface.create(style == 6 ? "sans-serif-condensed" : "sans-serif-black", Typeface.NORMAL));
    lettering.setTextSize(100);
    rawGlyphs.reset();
    lettering.getTextPath(text, 0, text.length(), 0, 100, rawGlyphs);
    rawGlyphs.computeBounds(bounds, true);
    if (bounds.isEmpty()) {
      glyphs.reset();
      return;
    }
    float available = Math.max(1, getWidth() - Ui.dp(getContext(), 6));
    float height =
        style == 6 ? getHeight() * .90f : Math.min(getHeight() * .84f, getTextSize() * .80f);
    float width =
        style == 6 ? available : Math.min(available, bounds.width() * height / bounds.height());
    width *= Math.max(60, Math.min(100, prefs.number("clockWidth", 100))) / 100f;
    int align = prefs.number("alignment", 1);
    float left = align == 0 ? 0 : align == 2 ? getWidth() - width : (getWidth() - width) / 2;
    float top = (getHeight() - height) / 2;
    destination.set(left, top, left + width, top + height);
    matrix.setRectToRect(bounds, destination, Matrix.ScaleToFit.FILL);
    rawGlyphs.transform(matrix, glyphs);
    int ink = prefs.ink() & 0xffffff;
    edgeShader =
        new LinearGradient(
            0,
            0,
            getWidth() * .7f,
            getHeight(),
            new int[] {ink | 0xFF000000, ink | 0x70000000, ink | 0xD0000000},
            new float[] {0, .55f, 1},
            Shader.TileMode.CLAMP);
    gloss.setShader(
        new LinearGradient(
            0,
            0,
            getWidth() * .35f,
            getHeight(),
            finish == 0
                ? new int[] {ink | 0x85000000, ink | 0x25000000, ink | 0x12000000, ink | 0x65000000}
                : new int[] {
                  ink | 0xBB000000, ink | 0x55000000, ink | 0x28000000, ink | 0x99000000
                },
            new float[] {0, .3f, .65f, 1},
            Shader.TileMode.CLAMP));
  }

  private void ensureTexture() {
    if (texture != null && textureWidth == source.getWidth() && textureHeight == source.getHeight())
      return;
    if (texture != null) texture.recycle();
    textureWidth = source.getWidth();
    textureHeight = source.getHeight();
    int width = source.glassTextureWidth();
    texture =
        source.bitmap(
            width, Math.max(1, Math.round(width * source.getHeight() / (float) source.getWidth())));
    shader = new BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
  }

  void prepareTexture() {
    if (source.getWidth() > 0
        && source.getHeight() > 0
        && (prefs.clock() == 0 || prefs.clock() == 6)
        && finish != 2) ensureTexture();
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
