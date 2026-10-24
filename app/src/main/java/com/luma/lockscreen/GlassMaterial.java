package com.luma.lockscreen;

import android.graphics.*;

/** Cached glass surface: exact distance-field bevel lighting and wallpaper refraction. */
final class GlassMaterial {
  static Bitmap render(
      Path glyphs,
      int viewWidth,
      int viewHeight,
      int rasterWidth,
      int[] backdrop,
      int backdropWidth,
      int backdropHeight,
      int sourceWidth,
      int sourceHeight,
      int left,
      int top,
      int ink,
      int finish,
      float density) {
    float scale = rasterWidth / (float) viewWidth;
    int height = Math.max(1, Math.round(viewHeight * scale));
    Bitmap result = Bitmap.createBitmap(rasterWidth, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(result);
    canvas.scale(scale, scale);
    Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
    mask.setColor(Color.WHITE);
    canvas.drawPath(glyphs, mask);
    int[] pixels = new int[rasterWidth * height];
    result.getPixels(pixels, 0, rasterWidth, 0, 0, rasterWidth, height);
    float[] distance = distance(pixels, rasterWidth, height);
    float sx = backdropWidth / (float) sourceWidth;
    float sy = backdropHeight / (float) sourceHeight;
    float bevel = Math.max(1.4f, density * 2.1f * scale);
    int ir = Color.red(ink), ig = Color.green(ink), ib = Color.blue(ink);
    for (int y = 0; y < height; y++) {
      float vertical = y / (float) height;
      float reflection = Math.max(0, 1 - Math.abs(vertical - .18f) / .12f) * .08f;
      float tint = (finish == 0 ? .26f : .43f) + .14f * (1 - vertical) + reflection;
      for (int x = 0; x < rasterWidth; x++) {
        int at = y * rasterWidth + x;
        int alpha = Color.alpha(pixels[at]);
        if (alpha == 0) continue;
        float d = Math.max(0, distance[at] - .5f);
        float nx = 0, ny = 0, edge = 0;
        if (d < bevel) {
          nx =
              distance[y * rasterWidth + Math.max(0, x - 1)]
                  - distance[y * rasterWidth + Math.min(rasterWidth - 1, x + 1)];
          ny =
              distance[Math.max(0, y - 1) * rasterWidth + x]
                  - distance[Math.min(height - 1, y + 1) * rasterWidth + x];
          float length = (float) Math.sqrt(nx * nx + ny * ny);
          if (length > .001f) {
            nx /= length;
            ny /= length;
          }
          edge = 1 - d / bevel;
        }
        float px = (left + x / scale + density * (1.6f + nx * edge * 3.5f)) * sx;
        float py = (top + y / scale + density * (1f + ny * edge * 3.5f)) * sy;
        int sampled = sample(backdrop, backdropWidth, backdropHeight, px, py);
        if (finish == 1) {
          int a = sample(backdrop, backdropWidth, backdropHeight, px - 2, py - 2);
          int b = sample(backdrop, backdropWidth, backdropHeight, px + 2, py + 2);
          sampled =
              Color.rgb(
                  (Color.red(sampled) * 2 + Color.red(a) + Color.red(b)) / 4,
                  (Color.green(sampled) * 2 + Color.green(a) + Color.green(b)) / 4,
                  (Color.blue(sampled) * 2 + Color.blue(a) + Color.blue(b)) / 4);
        }
        float r = Color.red(sampled) * (1 - tint) + ir * tint;
        float g = Color.green(sampled) * (1 - tint) + ig * tint;
        float b = Color.blue(sampled) * (1 - tint) + ib * tint;
        if (edge > 0) {
          float z = (float) Math.sqrt(1 - edge * edge);
          float light = (-.6f * nx - .8f * ny) * edge + .45f * z;
          float highlight =
              Math.min(.97f, .1f + .76f * Math.max(0, light) + .22f * edge * edge * edge);
          highlight *= edge * edge * (3 - 2 * edge);
          float shade = 1 - .16f * Math.max(0, -light);
          r = r * shade * (1 - highlight) + 255 * highlight;
          g = g * shade * (1 - highlight) + 255 * highlight;
          b = b * shade * (1 - highlight) + 255 * highlight;
        }
        pixels[at] = Color.argb(alpha, Math.round(r), Math.round(g), Math.round(b));
      }
    }
    result.setPixels(pixels, 0, rasterWidth, 0, 0, rasterWidth, height);
    return result;
  }

  private static int sample(int[] image, int w, int h, float x, float y) {
    x = Math.max(0, Math.min(w - 1, x));
    y = Math.max(0, Math.min(h - 1, y));
    int ix = (int) x, iy = (int) y, x1 = Math.min(w - 1, ix + 1), y1 = Math.min(h - 1, iy + 1);
    float fx = x - ix, fy = y - iy;
    int a = image[iy * w + ix], b = image[iy * w + x1];
    int c = image[y1 * w + ix], d = image[y1 * w + x1];
    int r = channel(a, b, c, d, 16, fx, fy);
    int g = channel(a, b, c, d, 8, fx, fy);
    int blue = channel(a, b, c, d, 0, fx, fy);
    return 0xFF000000 | r << 16 | g << 8 | blue;
  }

  private static int channel(int a, int b, int c, int d, int shift, float x, float y) {
    float top = ((a >> shift) & 255) * (1 - x) + ((b >> shift) & 255) * x;
    float bottom = ((c >> shift) & 255) * (1 - x) + ((d >> shift) & 255) * x;
    return Math.round(top * (1 - y) + bottom * y);
  }

  // Separable squared Euclidean distance transform. Linear cost, independent of bevel radius.
  private static float[] distance(int[] mask, int w, int h) {
    float[] distance = new float[mask.length];
    for (int i = 0; i < mask.length; i++) distance[i] = Color.alpha(mask[i]) < 128 ? 0 : 1e9f;
    int size = Math.max(w, h);
    float[] input = new float[size], output = new float[size], cuts = new float[size + 1];
    int[] sites = new int[size];
    for (int y = 0; y < h; y++) {
      System.arraycopy(distance, y * w, input, 0, w);
      line(input, output, w, sites, cuts);
      System.arraycopy(output, 0, distance, y * w, w);
    }
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) input[y] = distance[y * w + x];
      line(input, output, h, sites, cuts);
      for (int y = 0; y < h; y++) distance[y * w + x] = (float) Math.sqrt(output[y]);
    }
    return distance;
  }

  private static void line(float[] input, float[] output, int count, int[] sites, float[] cuts) {
    int k = 0;
    sites[0] = 0;
    cuts[0] = Float.NEGATIVE_INFINITY;
    cuts[1] = Float.POSITIVE_INFINITY;
    for (int q = 1; q < count; q++) {
      float s;
      do {
        int v = sites[k];
        s = ((input[q] + (float) q * q) - (input[v] + (float) v * v)) / (2f * (q - v));
        if (s > cuts[k]) break;
        k--;
      } while (k >= 0);
      k++;
      sites[k] = q;
      cuts[k] = s;
      cuts[k + 1] = Float.POSITIVE_INFINITY;
    }
    k = 0;
    for (int q = 0; q < count; q++) {
      while (cuts[k + 1] < q) k++;
      int delta = q - sites[k];
      output[q] = delta * delta + input[sites[k]];
    }
  }
}
