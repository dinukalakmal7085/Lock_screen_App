package com.luma.lockscreen;

import android.content.Context;
import java.util.*;
import org.json.*;

/** Named looks remain on-device. Snapshotting excludes permission/activation preferences. */
final class LookStore {
  static final class Look {
    final String id, name;
    final JSONObject design;

    Look(String id, String name, JSONObject design) {
      this.id = id;
      this.name = name;
      this.design = design;
    }
  }

  private final android.content.SharedPreferences storage;

  LookStore(Context c) {
    storage = c.getSharedPreferences("luma_looks", 0);
  }

  List<Look> all() {
    List<Look> result = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(storage.getString("looks", "[]"));
      for (int i = 0; i < a.length(); i++) {
        JSONObject o = a.getJSONObject(i);
        result.add(new Look(o.getString("id"), o.getString("name"), o.getJSONObject("design")));
      }
    } catch (JSONException ignored) {
    }
    return result;
  }

  void save(String name, Preferences p) {
    List<Look> list = all();
    list.add(
        0,
        new Look(
            UUID.randomUUID().toString(),
            name.trim().isEmpty()
                ? "My look"
                : name.trim().substring(0, Math.min(40, name.trim().length())),
            p.design()));
    if (list.size() > 20) list = list.subList(0, 20);
    write(list);
  }

  void delete(String id) {
    List<Look> list = all();
    list.removeIf(l -> l.id.equals(id));
    write(list);
  }

  void restore(String id, Preferences p) {
    for (Look l : all())
      if (l.id.equals(id)) {
        p.design(l.design);
        return;
      }
  }

  private void write(List<Look> list) {
    JSONArray a = new JSONArray();
    try {
      for (Look l : list)
        a.put(new JSONObject().put("id", l.id).put("name", l.name).put("design", l.design));
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    storage.edit().putString("looks", a.toString()).apply();
  }
}
