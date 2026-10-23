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
    "Ink",
    "Abyss",
    "Cloudwalker",
    "Velocity",
    "Stillwater",
    "Eclipse Guardian",
    "Emerald Halo",
    "Helio Dunes",
    "Soft Orbit",
    "Stadium Flight"
  };
  private static final String[] BASE_COLLECTIONS = {
    "Liquid", "Landscape", "Aurora", "Orbit", "Paper"
  };
  public static final String[] COLLECTIONS = {
    "All",
    "Cinematic",
    "Ocean",
    "Space",
    "Sport",
    "Architecture",
    "Fantasy",
    "Abstract",
    "Liquid",
    "Landscape",
    "Aurora",
    "Orbit",
    "Paper",
    "Favorites"
  };
  public static final int[] FEATURED = {32, 33, 34, 37, 38, 35, 36, 39, 40, 2, 6};
  public static final int[] DISPLAY_ORDER = {
    32, 33, 34, 37, 38, 35, 36, 39, 40, 6, 0, 14, 20, 26, 7, 1, 15, 21, 27, 8, 2, 16, 22, 28, 9, 3,
    17, 23, 29, 10, 4, 18, 24, 30, 11, 5, 19, 25, 31, 12, 13
  };

  public static int family(int id) {
    return id >= 32 ? 5 : id < 6 ? 0 : id < 14 ? 1 : id < 20 ? 2 : id < 26 ? 3 : 4;
  }

  public static String collection(int id) {
    if (!cinematic(id)) return BASE_COLLECTIONS[family(id)];
    switch (id) {
      case 32:
        return "Ocean";
      case 33:
        return "Space";
      case 34:
      case 40:
        return "Sport";
      case 35:
        return "Architecture";
      case 36:
        return "Fantasy";
      case 38:
        return "Landscape";
      default:
        return "Abstract";
    }
  }

  public static boolean cinematic(int id) {
    return id >= 32 && id < NAMES.length;
  }

  public static boolean depth(int id) {
    return family(id) == 1 || (cinematic(id) && id <= 36) || id == 40;
  }

  public static void apply(Preferences p, int id) {
    p.wallpaper(id);
    if (cinematic(id)) {
      p.setNumber("zoom", 100);
      p.setNumber("panX", 0);
      p.setNumber("panY", 0);
      p.setNumber("alignment", 1);
      p.setNumber("glassFinish", 0);
      p.setNumber("clockWidth", 100);
    }
    p.clock(cinematic(id) ? 6 : family(id) == 1 ? 4 : family(id) == 4 ? 5 : 0);
    p.setNumber("clockSize", cinematic(id) ? 126 : family(id) == 1 ? 104 : 94);
    p.setNumber("clockOffset", id == 34 ? 55 : cinematic(id) ? 12 : family(id) == 1 ? 58 : 0);
    p.setNumber("ink", family(id) == 4 ? 1 : 0);
    p.setFlag("depth", depth(id));
    p.setFlag("date", !cinematic(id));
    p.setFlag("24hour", cinematic(id));
    for (int i = 0; i < 4; i++)
      p.setNumber("widget" + i, cinematic(id) ? 0 : i == 0 ? 1 : i == 1 ? 2 : 0);
    p.setFlag("battery", true);
  }
}
