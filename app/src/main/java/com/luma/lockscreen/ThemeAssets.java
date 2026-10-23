package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.util.LruCache;

/** Original bundled artwork with authored subject boundaries, never user-photo segmentation. */
final class ThemeAssets {
  private static final int[] RESOURCES = {
    R.drawable.theme_abyss, R.drawable.theme_cloudwalker, R.drawable.theme_velocity,
    R.drawable.theme_stillwater, R.drawable.theme_eclipse_guardian, R.drawable.theme_emerald_halo,
    R.drawable.theme_helio_dunes, R.drawable.theme_soft_orbit, R.drawable.theme_stadium_flight
  };
  private static final LruCache<String, Bitmap> CACHE =
      new LruCache<String, Bitmap>(16 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
          return value.getByteCount();
        }
      };

  static Bitmap bitmap(Context c, int id, int width) {
    if (!ThemeCatalog.cinematic(id)) return null;
    int bucket = width <= 480 ? 420 : 1200;
    String key = id + ":" + bucket;
    Bitmap cached = CACHE.get(key);
    if (cached != null) return cached;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(c.getResources(), RESOURCES[id - 32], options);
    options.inSampleSize = 1;
    while (options.outWidth / (options.inSampleSize * 2) >= bucket * .85f)
      options.inSampleSize *= 2;
    options.inJustDecodeBounds = false;
    Bitmap decoded = BitmapFactory.decodeResource(c.getResources(), RESOURCES[id - 32], options);
    if (decoded != null) CACHE.put(key, decoded);
    return decoded;
  }

  static Path subject(int id) {
    // These normalized paths follow the supplied artwork. The same bitmap is redrawn
    // through the boundary, so the background and foreground never drift apart.
    switch (id) {
      case 32:
        return polygon(
            .342f, .275f, .359f, .273f, .379f, .282f, .401f, .302f, .429f, .336f, .45f, .37f, .48f,
            .406f, .52f, .44f, .59f, .49f, .64f, .53f, .78f, .61f, .82f, .66f, .72f, .67f, .60f,
            .60f, .65f, .83f, .93f, .96f, .7f, .94f, .51f, .86f, .34f, .65f, .15f, .72f, .145f,
            .66f, .19f, .55f, .25f, .46f, .28f, .40f, .292f, .34f, .31f, .30f);
      case 33:
        return polygon(
            .386f, .45f, .394f, .423f, .414f, .405f, .451f, .394f, .495f, .395f, .548f, .405f,
            .582f, .424f, .604f, .451f, .60f, .48f, .665f, .52f, .76f, .63f, .82f, .72f, .7f, .75f,
            .7f, .99f, .3f, .99f, .29f, .68f, .11f, .66f, .13f, .60f, .24f, .52f, .30f, .49f);
      case 34:
        return polygon(
            .419f, .466f, .424f, .453f, .451f, .443f, .481f, .440f, .514f, .440f, .547f, .449f,
            .559f, .46f, .562f, .48f, .55f, .493f, .606f, .50f, .638f, .52f, .66f, .61f, .67f, .65f,
            .59f, .69f, .6f, .91f, .37f, .91f, .35f, .70f, .28f, .65f, .30f, .58f, .333f, .518f,
            .37f, .499f, .43f, .486f);
      case 35:
        return polygon(
            .174f, .463f, .193f, .443f, .26f, .424f, .36f, .409f, .5f, .396f, .64f, .390f, .78f,
            .389f, 1f, .392f, 1f, 1f, .18f, 1f);
      case 36:
        return polygon(
            .554f, .356f, .558f, .347f, .575f, .337f, .604f, .331f, .622f, .329f, .644f, .336f,
            .653f, .349f, .657f, .363f, .705f, .37f, .738f, .40f, .81f, .45f, 1f, .50f, 1f, .8f,
            .72f, .8f, .68f, 1f, .45f, 1f, .46f, .58f, .49f, .50f, .52f, .43f, .52f, .40f, .55f,
            .38f);
      case 40:
        return polygon(
            .48f, .307f, .49f, .297f, .51f, .289f, .54f, .286f, .58f, .287f, .605f, .295f, .615f,
            .307f, .631f, .315f, .625f, .33f, .625f, .35f, .667f, .36f, .754f, .369f, .766f, .341f,
            .76f, .315f, .779f, .307f, .797f, .306f, .814f, .315f, .841f, .37f, .835f, .40f, .75f,
            .41f, .73f, .47f, .71f, .59f, .73f, .65f, .64f, .69f, .4f, .72f, .34f, .68f, .4f, .60f,
            .48f, .55f, .47f, .49f, .46f, .45f, .40f, .43f, .33f, .49f, .30f, .52f, .276f, .515f,
            .28f, .50f, .35f, .43f, .40f, .39f, .43f, .37f, .518f, .36f, .52f, .35f);
      default:
        return null;
    }
  }

  private static Path polygon(float... points) {
    Path path = new Path();
    path.moveTo(points[0], points[1]);
    for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
    path.close();
    return path;
  }
}
