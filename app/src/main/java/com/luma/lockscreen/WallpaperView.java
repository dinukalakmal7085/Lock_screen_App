package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.File;

/** Five original scalable art collections, plus private user photos and depth cutouts. */
public final class WallpaperView extends View {
  private static final int[][] COLORS = {
    {0xFF19122F, 0xFF6C54A3, 0xFFB3A5E9, 0xFFDB96AB},
    {0xFF271A36, 0xFF995A79, 0xFFF2AB9D, 0xFFBEA8F4},
    {0xFF081F34, 0xFF19617B, 0xFF7AD6D4, 0xFF8DACE1},
    {0xFF331723, 0xFFAC4933, 0xFFFFB479, 0xFFDD728A},
    {0xFF101F20, 0xFF395B4D, 0xFFA1B89A, 0xFFC4C69C},
    {0xFF10121B, 0xFF394154, 0xFFB9C6D2, 0xFF8C8FA8}
  };
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private int theme;
  private Bitmap photo, foreground;
  private Preferences config;

  public WallpaperView(Context c) {
    this(c, 0);
  }

  public WallpaperView(Context c, int theme) {
    super(c);
    setTheme(theme);
  }

  public WallpaperView config(Preferences p) {
    config = p;
    return this;
  }

  public void setTheme(int value) {
    theme = Math.max(0, Math.min(ThemeCatalog.NAMES.length - 1, value));
    photo = null;
    invalidate();
  }

  public void loadPhoto() {
    Preferences p = config == null ? new Preferences(getContext()) : config;
    photo = load(p.text("photoFile", "wallpaper.jpg"));
    foreground = load(p.text("foregroundFile", ""));
    invalidate();
  }

  private Bitmap load(String filename) {
    if (filename.isEmpty() || !new File(filename).getName().equals(filename)) return null;
    File f = new File(getContext().getFilesDir(), filename);
    if (!f.exists()) return null;
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(f.getPath(), o);
    o.inSampleSize = 1;
    while (Math.max(o.outWidth, o.outHeight) / o.inSampleSize > 1800) o.inSampleSize *= 2;
    o.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(f.getPath(), o);
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    render(c, getWidth(), getHeight());
  }

  private void image(Canvas c, Bitmap b, int w, int h) {
    float zoom = config == null ? 1 : Math.max(1, Math.min(2, config.number("zoom", 100) / 100f));
    float scale = Math.max(w / (float) b.getWidth(), h / (float) b.getHeight()) * zoom;
    float pw = b.getWidth() * scale, ph = b.getHeight() * scale;
    float
        x =
            config == null
                ? 0
                : Math.max(-100, Math.min(100, config.number("panX", 0))) / 100f * (pw - w) / 2,
        y =
            config == null
                ? 0
                : Math.max(-100, Math.min(100, config.number("panY", 0))) / 100f * (ph - h) / 2;
    c.drawBitmap(
        b,
        null,
        new RectF((w - pw) / 2 + x, (h - ph) / 2 + y, (w + pw) / 2 + x, (h + ph) / 2 + y),
        paint);
  }

  private void gradient(Canvas c, int w, int h, int top, int bottom) {
    paint.setShader(new LinearGradient(0, 0, w * .5f, h, top, bottom, Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, paint);
    paint.setShader(null);
  }

  private void glow(Canvas c, float x, float y, float radius, int color) {
    paint.setShader(
        new RadialGradient(
            x, y, radius, new int[] {color, color & 0xFFFFFF}, null, Shader.TileMode.CLAMP));
    c.drawCircle(x, y, radius, paint);
    paint.setShader(null);
  }

  private void render(Canvas c, int w, int h) {
    if (w <= 0 || h <= 0) return;
    int[] p = COLORS[theme % 6];
    int family = ThemeCatalog.family(theme);
    if (photo != null) {
      image(c, photo, w, h);
      int shade = config == null ? 18 : Math.max(0, Math.min(70, config.number("shade", 18)));
      gradient(c, w, h, Color.argb(shade * 2, 0, 0, 0), Color.argb(shade * 2 + 35, 0, 0, 0));
      return;
    }
    gradient(c, w, h, p[1], p[0]);
    switch (family) {
      case 0:
        glow(c, w * .18f, h * .3f, w * .9f, p[3]);
        c.save();
        c.rotate(-32, w * .5f, h * .55f);
        RectF oval = new RectF(-w * .15f, h * .25f, w * 1.15f, h * .77f);
        paint.setShader(
            new LinearGradient(
                oval.left,
                oval.top,
                oval.right,
                oval.bottom,
                new int[] {0xFFE9DCFF, p[2], p[1], p[0]},
                null,
                Shader.TileMode.CLAMP));
        c.drawOval(oval, paint);
        RectF inner =
            new RectF(
                oval.left + w * .12f,
                oval.top + h * .075f,
                oval.right - w * .12f,
                oval.bottom - h * .075f);
        paint.setShader(
            new LinearGradient(
                inner.left,
                inner.top,
                inner.right,
                inner.bottom,
                new int[] {p[0], p[1], p[3]},
                null,
                Shader.TileMode.CLAMP));
        c.drawOval(inner, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(w * .007f);
        paint.setShader(
            new LinearGradient(
                0, oval.top, w, oval.bottom, 0xEEFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawOval(oval, paint);
        c.drawOval(inner, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        c.restore();
        glow(c, w * .9f, h * .85f, w * .55f, p[2]);
        break;
      case 1:
        gradient(c, w, h, p[2], p[3]);
        glow(c, w * .72f, h * .18f, w * .35f, 0x88FFF4CD);
        paint.setColor(0xDDFDF2DC);
        c.drawCircle(w * .72f, h * .18f, w * .07f, paint);
        ridge(c, w, h, .48f, p[1]);
        ridge(c, w, h, .65f, p[0]);
        mountainForeground(c, w, h, p);
        break;
      case 2:
        gradient(c, w, h, p[0], 0xFF070D1A);
        for (int i = 0; i < 6; i++) {
          Path flow = new Path();
          flow.moveTo(-w * .2f, h * (.25f + i * .06f));
          flow.cubicTo(w * .65f, -h * .2f, w * .1f, h * 1.1f, w * 1.3f, h * (.45f + i * .06f));
          paint.setStyle(Paint.Style.STROKE);
          paint.setStrokeWidth(w * (.06f + i * .016f));
          paint.setShader(
              new LinearGradient(
                  0,
                  0,
                  w,
                  h,
                  new int[] {p[2], p[1], p[3] & 0x55FFFFFF},
                  null,
                  Shader.TileMode.CLAMP));
          c.drawPath(flow, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        stars(c, w, h);
        break;
      case 3:
        gradient(c, w, h, p[0], 0xFF070C15);
        for (int i = 3; i >= 0; i--) {
          float r = w * (.18f + i * .1f);
          glow(c, w * .55f, h * .58f, r, p[1]);
          paint.setShader(
              new RadialGradient(
                  w * .4f, h * .5f, r, new int[] {p[2], p[1], p[0]}, null, Shader.TileMode.CLAMP));
          c.drawCircle(w * .5f, h * .57f, r, paint);
          paint.setShader(null);
          paint.setStyle(Paint.Style.STROKE);
          paint.setStrokeWidth(w * .002f);
          paint.setColor(0x66FFFFFF);
          c.drawCircle(w * .5f, h * .57f, r, paint);
          paint.setStyle(Paint.Style.FILL);
        }
        stars(c, w, h);
        break;
      default:
        gradient(c, w, h, p[2], p[3]);
        for (int i = 0; i < 7; i++) {
          Path wave = new Path();
          wave.moveTo(-w * .2f, h * (.4f + i * .08f));
          wave.cubicTo(
              w * .1f,
              h * (.1f + i * .08f),
              w * .6f,
              h * (1f + i * .06f),
              w * 1.2f,
              h * (.3f + i * .08f));
          wave.lineTo(w * 1.2f, h * 1.2f);
          wave.lineTo(-w * .2f, h * 1.2f);
          wave.close();
          paint.setColor(blend(p[1], p[0], i / 7f));
          c.drawPath(wave, paint);
        }
        break;
    }
    gradient(c, w, h, 0x00000000, 0x66070A16);
  }

  private void stars(Canvas c, int w, int h) {
    paint.setColor(0x80FFFFFF);
    for (int i = 0; i < 35; i++)
      c.drawCircle(
          ((i * 67 + 29) % 997) / 997f * w,
          ((i * 131 + 31) % 991) / 991f * h,
          w * (i % 4 == 0 ? .003f : .0015f),
          paint);
  }

  private static int blend(int a, int b, float f) {
    return Color.rgb(
        (int) (Color.red(a) * (1 - f) + Color.red(b) * f),
        (int) (Color.green(a) * (1 - f) + Color.green(b) * f),
        (int) (Color.blue(a) * (1 - f) + Color.blue(b) * f));
  }

  private void ridge(Canvas c, int w, int h, float level, int color) {
    Path path = new Path();
    path.moveTo(0, h);
    path.lineTo(0, h * (level + .1f));
    path.lineTo(w * .2f, h * (level - .05f));
    path.lineTo(w * .38f, h * (level + .05f));
    path.lineTo(w * .62f, h * (level - .15f));
    path.lineTo(w * .82f, h * (level - .02f));
    path.lineTo(w, h * (level - .1f));
    path.lineTo(w, h);
    path.close();
    paint.setShader(
        new LinearGradient(
            0, h * level, w, h, color, blend(color, 0xFF070F20, .6f), Shader.TileMode.CLAMP));
    c.drawPath(path, paint);
    paint.setShader(null);
  }

  private void mountainForeground(Canvas c, int w, int h, int[] p) {
    // Anchor the summit to width as well as height so tall phones and editor previews
    // retain a small, readable clock overlap rather than stretching the subject downwards.
    float peak = Math.min(h * .48f, w * .66f);
    float stretch = (h - peak) / (h * .6f);
    c.save();
    c.translate(0, h * (1 - stretch));
    c.scale(1, stretch);
    Path path = new Path();
    path.moveTo(0, h);
    path.lineTo(0, h * .86f);
    path.lineTo(w * .28f, h * .72f);
    path.lineTo(w * .6f, h * .4f);
    path.lineTo(w * .72f, h * .48f);
    path.lineTo(w * .78f, h * .45f);
    path.lineTo(w, h * .68f);
    path.lineTo(w, h);
    path.close();
    paint.setShader(new LinearGradient(0, h * .4f, w, h, p[1], p[0], Shader.TileMode.CLAMP));
    c.drawPath(path, paint);
    paint.setShader(null);
    paint.setColor(0x88EEE6E0);
    Path snow = new Path();
    snow.moveTo(w * .51f, h * .49f);
    snow.lineTo(w * .6f, h * .4f);
    snow.lineTo(w * .69f, h * .46f);
    snow.lineTo(w * .63f, h * .445f);
    snow.lineTo(w * .6f, h * .47f);
    snow.lineTo(w * .585f, h * .44f);
    snow.close();
    c.drawPath(snow, paint);
    c.restore();
  }

  void drawForeground(Canvas c, int w, int h) {
    if (config == null || !config.flag("depth", false)) return;
    if (photo != null) {
      if (foreground != null) image(c, foreground, w, h);
    } else if (ThemeCatalog.family(theme) == 1) mountainForeground(c, w, h, COLORS[theme % 6]);
  }

  public Bitmap bitmap(int w, int h) {
    Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    render(new Canvas(b), w, h);
    return b;
  }
}
