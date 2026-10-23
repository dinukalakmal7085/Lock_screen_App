package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.File;

/** Original procedural artwork; scalable and entirely offline. */
public final class WallpaperView extends View {
  public WallpaperView(Context context) {
    this(context, 0);
  }

  private static final int[][] COLORS = {
    {0xFF19122F, 0xFF6C54A3, 0xFFB3A5E9, 0xFFDB96AB},
    {0xFF271A36, 0xFF995A79, 0xFFF2AB9D, 0xFFBEA8F4},
    {0xFF081F34, 0xFF19617B, 0xFF7AD6D4, 0xFF8DACE1},
    {0xFF331723, 0xFFAC4933, 0xFFFFB479, 0xFFDD728A},
    {0xFF101F20, 0xFF395B4D, 0xFFA1B89A, 0xFFC4C69C},
    {0xFF10121B, 0xFF394154, 0xFFB9C6D2, 0xFF8C8FA8}
  };
  private final Paint paint = new Paint(3);
  private int theme;
  private Bitmap photo;

  public WallpaperView(Context c, int theme) {
    super(c);
    this.theme = Math.max(0, Math.min(5, theme));
  }

  public void setTheme(int value) {
    theme = Math.max(0, Math.min(5, value));
    photo = null;
    invalidate();
  }

  public void loadPhoto() {
    File f = new File(getContext().getFilesDir(), "wallpaper.jpg");
    if (f.exists()) {
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(f.getPath(), o);
      o.inSampleSize = Math.max(1, Math.max(o.outWidth, o.outHeight) / 1800);
      o.inJustDecodeBounds = false;
      photo = BitmapFactory.decodeFile(f.getPath(), o);
      invalidate();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    render(canvas, getWidth(), getHeight());
  }

  private void render(Canvas c, int w, int h) {
    if (w == 0 || h == 0) return;
    if (photo != null) {
      float scale = Math.max(w / (float) photo.getWidth(), h / (float) photo.getHeight());
      float pw = photo.getWidth() * scale, ph = photo.getHeight() * scale;
      c.drawBitmap(
          photo, null, new RectF((w - pw) / 2, (h - ph) / 2, (w + pw) / 2, (h + ph) / 2), paint);
      paint.setShader(
          new LinearGradient(0, 0, 0, h, 0x30000000, 0x77000000, Shader.TileMode.CLAMP));
      c.drawRect(0, 0, w, h, paint);
      paint.setShader(null);
      return;
    }
    int[] p = COLORS[theme];
    paint.setShader(
        new LinearGradient(
            0,
            0,
            w,
            h,
            new int[] {p[0], p[1], p[0]},
            new float[] {0, .55f, 1},
            Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, paint);
    paint.setShader(
        new RadialGradient(
            w * .18f,
            h * .35f,
            w * .9f,
            new int[] {p[3], 0x00000000},
            null,
            Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, paint);
    c.save();
    c.rotate(-32, w * .5f, h * .55f);
    RectF oval = new RectF(-w * .15f, h * .25f, w * 1.15f, h * .77f);
    paint.setShader(
        new LinearGradient(
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            new int[] {0xDDE9DCFF, p[2], p[1], p[0]},
            new float[] {0, .3f, .65f, 1},
            Shader.TileMode.CLAMP));
    c.drawOval(oval, paint);
    RectF inside =
        new RectF(
            oval.left + w * .12f,
            oval.top + h * .075f,
            oval.right - w * .12f,
            oval.bottom - h * .075f);
    paint.setShader(
        new LinearGradient(
            inside.left,
            inside.top,
            inside.right,
            inside.bottom,
            new int[] {p[0], p[1], p[3]},
            null,
            Shader.TileMode.CLAMP));
    c.drawOval(inside, paint);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(w * .009f);
    paint.setShader(
        new LinearGradient(
            0, oval.top, w, oval.bottom, 0xEFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    c.drawOval(oval, paint);
    paint.setStrokeWidth(w * .005f);
    c.drawOval(inside, paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(null);
    c.restore();
    paint.setShader(
        new RadialGradient(
            w * .88f,
            h * .82f,
            w * .6f,
            new int[] {p[2], 0x00000000},
            null,
            Shader.TileMode.CLAMP));
    c.drawCircle(w * .88f, h * .82f, w * .6f, paint);
    paint.setShader(null);
    paint.setShader(
        new LinearGradient(
            0,
            0,
            0,
            h,
            new int[] {0x22000000, 0x00000000, 0x99070A16},
            new float[] {0, .5f, 1},
            Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, paint);
    paint.setShader(null);
  }

  public Bitmap bitmap(int width, int height) {
    Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    render(new Canvas(b), width, height);
    return b;
  }
}
