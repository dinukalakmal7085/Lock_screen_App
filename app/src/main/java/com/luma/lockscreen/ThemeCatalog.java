package com.luma.lockscreen;

/** Original on-device art collections. No stock imagery or network catalog. */
public final class ThemeCatalog {
  public static final String[] NAMES = {
    "Iris",
    "Dusk",
    "Tide",
    "Ember",
    "Moss",
    "Graphite",
    "Alpine",
    "Dawnline",
    "Blue Hour",
    "Sandstone",
    "Highlands",
    "Glacier",
    "Moonrise",
    "Wildlight",
    "Aurora",
    "Afterglow",
    "Boreal",
    "Midnight",
    "Solar",
    "Nebula",
    "Orbit",
    "Halo",
    "Eclipse",
    "Daybreak",
    "Stardust",
    "Pulse",
    "Paper",
    "Mauve",
    "Matcha",
    "Chalk",
    "Sienna",
    "Ink"
  };
  public static final String[] COLLECTIONS = {
    "All", "Liquid", "Landscape", "Aurora", "Orbit", "Paper", "Favorites"
  };
  public static final int[] FEATURED = {6, 0, 14, 20, 26, 2};
  public static final int[] DISPLAY_ORDER = {
    6, 0, 14, 20, 26, 7, 1, 15, 21, 27, 8, 2, 16, 22, 28, 9,
    3, 17, 23, 29, 10, 4, 18, 24, 30, 11, 5, 19, 25, 31, 12, 13
  };

  public static int family(int id) {
    return id < 6 ? 0 : id < 14 ? 1 : id < 20 ? 2 : id < 26 ? 3 : 4;
  }

  public static String collection(int id) {
    return COLLECTIONS[family(id) + 1];
  }

  public static void apply(Preferences p, int id) {
    p.wallpaper(id);
    p.clock(family(id) == 1 ? 4 : family(id) == 4 ? 5 : 0);
    p.setNumber("clockSize", family(id) == 1 ? 104 : 94);
    p.setNumber("clockOffset", family(id) == 1 ? 58 : 0);
    p.setNumber("ink", family(id) == 4 ? 1 : 0);
    p.setFlag("depth", family(id) == 1);
    p.setFlag("date", true);
    p.setFlag("battery", true);
  }
}
