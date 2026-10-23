package com.luma.lockscreen;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.util.UUID;

final class PhotoStore {
  static String importImage(Context c, Uri uri, boolean foreground) throws IOException {
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inJustDecodeBounds = true;
    try (InputStream in = c.getContentResolver().openInputStream(uri)) {
      BitmapFactory.decodeStream(in, null, o);
    }
    if (o.outWidth < 1 || o.outHeight < 1) throw new IOException("Unsupported image");
    o.inSampleSize = 1;
    while (Math.max(o.outWidth, o.outHeight) / o.inSampleSize > 2400) o.inSampleSize *= 2;
    o.inJustDecodeBounds = false;
    Bitmap bitmap;
    try (InputStream in = c.getContentResolver().openInputStream(uri)) {
      bitmap = BitmapFactory.decodeStream(in, null, o);
    }
    if (bitmap == null) throw new IOException("Image couldn't be decoded");
    File target =
        new File(c.getFilesDir(), "photo-" + UUID.randomUUID() + (foreground ? ".png" : ".jpg"));
    try {
      try (InputStream in = c.getContentResolver().openInputStream(uri)) {
        ExifInterface exif = new ExifInterface(in);
        Matrix m = new Matrix();
        switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
          case 2:
            m.setScale(-1, 1);
            break;
          case 3:
            m.setRotate(180);
            break;
          case 4:
            m.setScale(1, -1);
            break;
          case 5:
            m.setRotate(90);
            m.postScale(-1, 1);
            break;
          case 6:
            m.setRotate(90);
            break;
          case 7:
            m.setRotate(-90);
            m.postScale(-1, 1);
            break;
          case 8:
            m.setRotate(-90);
            break;
          default:
            break;
        }
        Bitmap rotated =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        if (rotated != bitmap) {
          bitmap.recycle();
          bitmap = rotated;
        }
      } catch (IOException ignored) {
      }
      if (foreground && !hasTransparency(bitmap))
        throw new IOException("Choose a transparent PNG cutout for the foreground layer.");
      try (FileOutputStream out = new FileOutputStream(target)) {
        if (!bitmap.compress(
            foreground ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, out))
          throw new IOException("Couldn't save image");
      }
      return target.getName();
    } catch (IOException | RuntimeException e) {
      target.delete();
      throw e;
    } finally {
      bitmap.recycle();
    }
  }

  private static boolean hasTransparency(Bitmap b) {
    if (!b.hasAlpha()) return false;
    for (int y = 0; y < b.getHeight(); y += Math.max(1, b.getHeight() / 100))
      for (int x = 0; x < b.getWidth(); x += Math.max(1, b.getWidth() / 100))
        if (Color.alpha(b.getPixel(x, y)) < 200) return true;
    return false;
  }
}
