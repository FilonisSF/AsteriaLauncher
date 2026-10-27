package ru.safimine.asteria.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Your own 3D models (pet, hat, item on the back, wings) imported in the launcher from Blockbench / resource packs.
 * File: {v, tex: [png base64], polys: [{t, p: [[x, y, z, u, v]...]}]} in blocks, standing on y = 0, looking at +z.
 * Own models: config/asteria-model-<slot>.json; other players' come from the Asteria server into config/asteria-models/.
 */
public final class Model {
    private Model() {}
    static final class M { File[] tex; List<float[]>[] polys; long mtime; } // polys per texture: x,y,z,u,v * n (n = 3 or 4, padded to 4)
    private static final Map<String, M> CACHE = new HashMap<>();
    private static final Map<String, Long> FETCHED = new HashMap<>();

    /** own model of a slot ("pet", "hat", "back", "wings") */
    public static File own(String slot) { return new File(V.dir(), "asteria-model-" + slot + ".json"); }
    /** another player's model (downloaded from the Asteria server, refreshed every 10 min) */
    public static File other(String name, String slot) {
        File f = new File(new File(V.dir(), "asteria-models"), name + "-" + slot + ".json"); long now = System.currentTimeMillis(); String k = name + slot; Long t = FETCHED.get(k);
        if (t == null || now - t > 600000) { FETCHED.put(k, now); final String api = System.getProperty("asteria.api");
            if (api != null && !api.isEmpty()) { Thread th = new Thread(() -> { try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(api + "/model/" + name + "/" + slot + ".json").openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(15000);
                if (c.getResponseCode() == 200) { f.getParentFile().mkdirs(); try (java.io.InputStream in = c.getInputStream()) { Files.copy(in, f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } } } catch (Throwable ignored) {} }, "asteria-model"); th.setDaemon(true); th.start(); } }
        return f;
    }
    @SuppressWarnings("unchecked")
    private static M load(File f) {
        if (f == null || !f.exists()) return null; String key = f.getAbsolutePath(); M m = CACHE.get(key); long mt = f.lastModified();
        if (m != null && m.mtime == mt) return m;
        try {
            JsonObject o = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray tex = o.getAsJsonArray("tex"); m = new M(); m.mtime = mt; m.tex = new File[tex.size()]; m.polys = new List[tex.size()];
            for (int i = 0; i < tex.size(); i++) { File png = new File(f.getParentFile(), f.getName().replace(".json", "") + "-t" + i + ".png");
                if (!png.exists() || png.lastModified() < mt) Files.write(png.toPath(), java.util.Base64.getDecoder().decode(tex.get(i).getAsString()));
                m.tex[i] = png; m.polys[i] = new ArrayList<>(); }
            for (JsonElement pe : o.getAsJsonArray("polys")) { JsonObject p = pe.getAsJsonObject(); int t = Math.max(0, Math.min(m.tex.length - 1, p.get("t").getAsInt())); JsonArray pts = p.getAsJsonArray("p");
                float[] q = new float[20]; int n = Math.min(4, pts.size());
                for (int i = 0; i < 4; i++) { JsonArray v = pts.get(Math.min(i, n - 1)).getAsJsonArray(); for (int k = 0; k < 5; k++) q[i * 5 + k] = v.get(k).getAsFloat(); }
                m.polys[t].add(q); }
            CACHE.put(key, m); return m;
        } catch (Exception e) { CACHE.put(key, null); return null; }
    }
    public interface Tf { double[] at(double x, double y, double z); }
    /** draws the model; tf maps model space (blocks) to world space */
    public static boolean draw(Fx.R3 r, File f, Tf tf, float light) {
        M m = load(f); if (m == null) return false; int col = Fx.shade(0xFFFFFFFF, light);
        for (int t = 0; t < m.tex.length; t++) { if (m.polys[t].isEmpty()) continue; Object tex = r.fileTex("am_" + Integer.toHexString(f.getAbsolutePath().hashCode()) + "_" + t + ":" + m.mtime, m.tex[t]); if (tex == null) continue;
            r.beginSkin(tex);
            for (float[] q : m.polys[t]) for (int i = 0; i < 4; i++) { double[] w = tf.at(q[i * 5], q[i * 5 + 1], q[i * 5 + 2]); r.vt(w[0], w[1], w[2], q[i * 5 + 3], q[i * 5 + 4], col); }
            r.end(); }
        return true;
    }
}
