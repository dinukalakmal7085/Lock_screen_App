package com.luma.lockscreen;

import android.graphics.*;

/** Original tabular numerals authored for a tall clock, independent of Android's system font. */
final class ClockNumerals {
  private static final float HEIGHT = 240, WIDTH = 52, GAP = 8, COLON = 14;
  private final Path[] digits = new Path[10];
  private final Path positioned = new Path();
  private final Matrix translation = new Matrix();
  private float advance;

  ClockNumerals() {
    Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    stroke.setStyle(Paint.Style.STROKE);
    stroke.setStrokeWidth(11.2f);
    stroke.setStrokeCap(Paint.Cap.ROUND);
    stroke.setStrokeJoin(Paint.Join.ROUND);
    for (int i = 0; i < 10; i++) {
      Path line = new Path();
      switch (i) {
        case 0:
          line.addRoundRect(6, 6, 46, 234, 20, 22, Path.Direction.CW);
          break;
        case 1:
          line.moveTo(8, 31);
          line.lineTo(36, 4);
          line.lineTo(39, 4);
          line.lineTo(39, 236);
          break;
        case 2:
          line.moveTo(4, 39);
          line.lineTo(4, 28);
          line.cubicTo(4, -4, 48, -4, 48, 28);
          line.lineTo(48, 48);
          line.cubicTo(48, 78, 5, 160, 4, 206);
          line.lineTo(4, 236);
          line.lineTo(48, 236);
          break;
        case 3:
          line.moveTo(4, 24);
          line.cubicTo(4, -4, 48, -4, 48, 28);
          line.lineTo(48, 86);
          line.cubicTo(48, 109, 38, 120, 22, 120);
          line.cubicTo(38, 120, 48, 131, 48, 154);
          line.lineTo(48, 212);
          line.cubicTo(48, 244, 4, 244, 4, 216);
          break;
        case 4:
          line.moveTo(40, 236);
          line.lineTo(40, 4);
          line.lineTo(4, 168);
          line.lineTo(49, 168);
          break;
        case 5:
          line.moveTo(48, 4);
          line.lineTo(5, 4);
          line.lineTo(5, 111);
          line.cubicTo(11, 93, 48, 95, 48, 123);
          line.lineTo(48, 212);
          line.cubicTo(48, 244, 4, 244, 4, 216);
          break;
        case 6:
          line.moveTo(48, 26);
          line.cubicTo(48, -4, 4, -4, 4, 28);
          line.lineTo(4, 212);
          line.cubicTo(4, 244, 48, 244, 48, 212);
          line.lineTo(48, 133);
          line.cubicTo(48, 101, 4, 101, 4, 133);
          break;
        case 7:
          line.moveTo(4, 4);
          line.lineTo(48, 4);
          line.lineTo(17, 236);
          break;
        case 8:
          line.addRoundRect(6, 6, 46, 120, 20, 22, Path.Direction.CW);
          line.addRoundRect(6, 120, 46, 234, 20, 22, Path.Direction.CW);
          break;
        default:
          // A rotated six gives nine the same optical weight and corner geometry.
          Matrix rotate = new Matrix();
          rotate.setRotate(180, 26, 120);
          digits[6].transform(rotate, line);
          digits[i] = line;
          continue;
      }
      digits[i] = new Path();
      stroke.getFillPath(line, digits[i]);
    }
  }

  boolean build(String text, Path output) {
    for (int i = 0; i < text.length(); i++)
      if ((text.charAt(i) < '0' || text.charAt(i) > '9') && text.charAt(i) != ':') return false;
    output.reset();
    float x = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == ':') {
        output.addCircle(x + COLON / 2, HEIGHT * .25f, 4.8f, Path.Direction.CW);
        output.addCircle(x + COLON / 2, HEIGHT * .79f, 4.8f, Path.Direction.CW);
        x += COLON + GAP;
      } else {
        translation.setTranslate(x, 0);
        digits[c - '0'].transform(translation, positioned);
        output.addPath(positioned);
        x += WIDTH + GAP;
      }
    }
    advance = Math.max(0, x - GAP);
    return true;
  }

  void bounds(RectF result) {
    result.set(0, 0, advance, HEIGHT);
  }
}
