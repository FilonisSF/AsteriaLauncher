package ru.safimine.asteria.visuals;

import com.google.gson.*;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * State of Asteria Visuals: modules and their settings (from schema.json, shared with the launcher), theme, HUD positions,
 * licence. Settings live in config/asteria-visuals.json; the launcher and the in-game menu both edit that file and both
 * pick up the other side's changes within a second.
 */
public final class V {
    private V() {}

    // ------------------------------------------------------------------ model
    public static final class S {
        public String id, name, type; public String[] options; public double min, max, step;
        public double num; public boolean bool; public int mode; public int color; public int key;
        Object def;
        String save() { switch (type) { case "bool": return String.valueOf(bool); case "num": return String.valueOf(num); case "mode": return String.valueOf(mode); case "color": return hex(color); default: return String.valueOf(key); } }
    }
    public static final class Mod {
        public String id, cat, name, desc; public boolean on, defOn; public int bind; public float[] hud; public float[] defHud;
        public final List<S> s = new ArrayList<>();
        public S get(String sid) { for (S x : s) if (x.id.equals(sid)) return x; return null; }
    }
    public static final class Cat { public String id, name, icon; }
    public static final List<Mod> MODS = new ArrayList<>();
    public static final List<Cat> CATS = new ArrayList<>();
    private static final Map<String, Mod> BY = new HashMap<>();

    // theme
    public static int accent = 0xFF22D3EE, accent2 = 0xFF3B82F6;
    public static boolean chroma = false, shadow = true, blur = true;
    public static float bg = 0.6f, scale = 1f;
    /** HUD look: 0 = crystal panels, 1 = Asteria lines (thin lines, minimal backing) */
    public static int hudStyle = 0;
    /** a light sheen running over the HUD panels now and then */
    public static boolean glare = false;
    /** font of all Asteria text: 0 = Minecraft pixel font, 1.. = smooth TTF fonts (assets/asteria_visuals/font/f<N>.json) */
    public static int font = 2;
    /** Asteria style for Minecraft's own menus (buttons, logo, background) */
    public static boolean menu = true;
    public static final String[] FONTS = {"Minecraft", "Inter", "Montserrat Alt", "Unbounded", "Comfortaa", "Glitch (дед инсайд)", "Ruslan", "Russo One"};

    public static Mod mod(String id) { return BY.get(id); }
    public static boolean on(String id) { Mod m = BY.get(id); return m != null && m.on && licensed(); }
    public static double num(String id, String s) { S x = BY.get(id).get(s); return x == null ? 0 : x.num; }
    public static boolean bool(String id, String s) { S x = BY.get(id).get(s); return x != null && x.bool; }
    public static int mode(String id, String s) { S x = BY.get(id).get(s); return x == null ? 0 : x.mode; }
    public static int color(String id, String s) { S x = BY.get(id).get(s); return x == null ? -1 : x.color; }

    // ------------------------------------------------------------------ licence
    public static long licensedUntil = -1;
    private static long lastLic;
    public static String user = "";
    /** visuals are free for everyone (no subscription) */
    public static boolean licensed() { return true; }

    // ------------------------------------------------------------------ load / save
    private static File file, presetsFile;
    /** named configs made in the launcher (config/asteria-visuals-presets.json); the in-game menu can switch them */
    public static final class Preset { public String id, name; JsonObject cfg; }
    public static final List<Preset> PRESETS = new ArrayList<>();
    public static String activePreset = "";
    private static long presetsMtime = -2;
    private static long mtime = -2, ownWrite = -1, dirtyAt = 0;
    public static Runnable onChange = () -> {};

    public static void init(String username) {
        user = username == null ? "" : username;
        try (Reader r = new InputStreamReader(V.class.getResourceAsStream("/asteria/schema.json"), StandardCharsets.UTF_8)) {
            JsonObject o = new JsonParser().parse(r).getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("categories")) { JsonObject c = e.getAsJsonObject(); Cat k = new Cat(); k.id = str(c, "id"); k.name = str(c, "name"); k.icon = str(c, "icon"); CATS.add(k); }
            for (JsonElement e : o.getAsJsonArray("modules")) {
                JsonObject j = e.getAsJsonObject(); Mod m = new Mod();
                m.id = str(j, "id"); m.cat = str(j, "cat"); m.name = str(j, "name"); m.desc = str(j, "desc"); m.on = m.defOn = j.get("on").getAsBoolean();
                if (j.has("hud")) { JsonArray h = j.getAsJsonArray("hud"); m.hud = new float[]{h.get(0).getAsFloat(), h.get(1).getAsFloat()}; m.defHud = m.hud.clone(); }
                for (JsonElement se : j.getAsJsonArray("s")) {
                    JsonObject sj = se.getAsJsonObject(); S s = new S(); s.id = str(sj, "id"); s.name = str(sj, "name"); s.type = str(sj, "type");
                    if (sj.has("options")) { JsonArray a = sj.getAsJsonArray("options"); s.options = new String[a.size()]; for (int i = 0; i < a.size(); i++) s.options[i] = a.get(i).getAsString(); }
                    if (sj.has("min")) { s.min = sj.get("min").getAsDouble(); s.max = sj.get("max").getAsDouble(); s.step = sj.get("step").getAsDouble(); }
                    s.def = sj.get("def"); reset(s); m.s.add(s);
                }
                MODS.add(m); BY.put(m.id, m);
            }
        } catch (Exception ex) { throw new RuntimeException("Asteria: schema.json", ex); }
        String dir = System.getProperty("asteria.gamedir", new File(".").getAbsolutePath());
        file = new File(new File(dir, "config"), "asteria-visuals.json");
        presetsFile = new File(file.getParentFile(), "asteria-visuals-presets.json");
        reload(true); reloadPresets();
    }
    static void reset(S s) {
        JsonElement d = (JsonElement) s.def;
        switch (s.type) { case "bool": s.bool = d.getAsBoolean(); break; case "num": s.num = d.getAsDouble(); break; case "mode": s.mode = d.getAsInt(); break; case "color": s.color = parse(d.getAsString(), -1); break; default: s.key = d.getAsInt(); }
    }

    /** called every frame: re-reads the file when the launcher changed it; writes our own changes a moment after the last edit */
    private static long lastCheck;
    public static void tick() {
        long now = System.currentTimeMillis();
        if (dirtyAt > 0 && now - dirtyAt > 400) { dirtyAt = 0; write(); }
        if (now - lastCheck < 700) return;
        lastCheck = now;
        reload(false); reloadPresets();
    }
    public static void changed() { dirtyAt = System.currentTimeMillis(); onChange.run(); }

    private static void reload(boolean force) {
        long m = file.exists() ? file.lastModified() : -1;
        if (!force && (m == mtime || m == ownWrite)) { mtime = m; return; }
        mtime = m;
        if (m < 0) return;
        try { apply(new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject(), true); } catch (Exception ignored) {}
    }
    private static void reloadPresets() {
        long m = presetsFile.exists() ? presetsFile.lastModified() : -1;
        if (m == presetsMtime) return;
        presetsMtime = m; PRESETS.clear(); activePreset = "";
        if (m < 0) return;
        try {
            JsonObject o = new JsonParser().parse(new String(Files.readAllBytes(presetsFile.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            activePreset = str(o, "active");
            for (JsonElement e : o.getAsJsonArray("list")) { JsonObject j = e.getAsJsonObject(); Preset p = new Preset(); p.id = str(j, "id"); p.name = str(j, "name"); p.cfg = j.getAsJsonObject("cfg"); PRESETS.add(p); }
        } catch (Exception ignored) {}
    }
    /** switches to a launcher preset: applies it now and remembers it as active (the launcher sees both files change) */
    public static void usePreset(Preset p) {
        try {
            apply(p.cfg, true); write();
            JsonObject o = new JsonParser().parse(new String(Files.readAllBytes(presetsFile.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            o.addProperty("active", p.id); activePreset = p.id;
            for (JsonElement e : o.getAsJsonArray("list")) if (p.id.equals(str(e.getAsJsonObject(), "id"))) p.cfg = e.getAsJsonObject().getAsJsonObject("cfg");
            Files.write(presetsFile.toPath(), new Gson().toJson(o).getBytes(StandardCharsets.UTF_8)); presetsMtime = presetsFile.lastModified();
        } catch (Exception ignored) {}
    }
    private static void apply(JsonObject o, boolean binds) {
        {
            if (o.has("theme")) { JsonObject t = o.getAsJsonObject("theme");
                accent = parse(str(t, "accent"), accent); accent2 = parse(str(t, "accent2"), accent2);
                chroma = b(t, "chroma", chroma); shadow = b(t, "shadow", shadow); blur = b(t, "blur", blur); bg = (float) d(t, "bg", bg); scale = (float) d(t, "scale", scale); hudStyle = (int) d(t, "hud", hudStyle); glare = b(t, "glare", glare); if (t.has("font")) { try { font = t.get("font").isJsonPrimitive() && t.get("font").getAsJsonPrimitive().isBoolean() ? (t.get("font").getAsBoolean() ? 2 : 0) : Math.max(0, Math.min(FONTS.length - 1, t.get("font").getAsInt())); } catch (Exception ignored) {} } menu = b(t, "menu", menu); }
            JsonObject mods = o.has("modules") ? o.getAsJsonObject("modules") : new JsonObject();
            for (Mod md : MODS) {
                if (!mods.has(md.id)) continue;
                JsonObject j = mods.getAsJsonObject(md.id);
                md.on = b(j, "on", md.on); if (binds && j.has("bind")) md.bind = j.get("bind").getAsInt();
                if (j.has("hud") && md.hud != null) { JsonArray h = j.getAsJsonArray("hud"); md.hud[0] = h.get(0).getAsFloat(); md.hud[1] = h.get(1).getAsFloat(); }
                JsonObject sv = j.has("s") ? j.getAsJsonObject("s") : new JsonObject();
                for (S s : md.s) if (sv.has(s.id)) { JsonElement e = sv.get(s.id);
                    try { switch (s.type) { case "bool": s.bool = e.getAsBoolean(); break; case "num": s.num = Math.max(s.min, Math.min(s.max, e.getAsDouble())); break; case "mode": s.mode = Math.max(0, Math.min(s.options.length - 1, e.getAsInt())); break; case "color": s.color = parse(e.getAsString(), s.color); break; default: s.key = e.getAsInt(); } } catch (Exception ignored) {} }
            }
            onChange.run();
        }
    }
    private static void write() {
        try {
            JsonObject o = new JsonObject(), t = new JsonObject(), mods = new JsonObject();
            t.addProperty("accent", hex(accent)); t.addProperty("accent2", hex(accent2)); t.addProperty("chroma", chroma); t.addProperty("shadow", shadow); t.addProperty("blur", blur); t.addProperty("bg", bg); t.addProperty("scale", scale); t.addProperty("hud", hudStyle); t.addProperty("glare", glare); t.addProperty("font", font); t.addProperty("menu", menu);
            o.add("theme", t);
            for (Mod m : MODS) {
                JsonObject j = new JsonObject(), sv = new JsonObject(); j.addProperty("on", m.on); j.addProperty("bind", m.bind);
                if (m.hud != null) { JsonArray h = new JsonArray(); h.add(new JsonPrimitive(m.hud[0])); h.add(new JsonPrimitive(m.hud[1])); j.add("hud", h); }
                for (S s : m.s) switch (s.type) { case "bool": sv.addProperty(s.id, s.bool); break; case "num": sv.addProperty(s.id, s.num); break; case "mode": sv.addProperty(s.id, s.mode); break; case "color": sv.addProperty(s.id, hex(s.color)); break; default: sv.addProperty(s.id, s.key); }
                j.add("s", sv); mods.add(m.id, j);
            }
            o.add("modules", mods);
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(o).getBytes(StandardCharsets.UTF_8));
            ownWrite = file.lastModified(); mtime = ownWrite;
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ helpers
    /** the game's config folder */
    public static File dir() { return file.getParentFile(); }
    static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    static boolean b(JsonObject o, String k, boolean d) { try { return o.has(k) ? o.get(k).getAsBoolean() : d; } catch (Exception e) { return d; } }
    static double d(JsonObject o, String k, double d) { try { return o.has(k) ? o.get(k).getAsDouble() : d; } catch (Exception e) { return d; } }
    public static int parse(String s, int d) { try { s = s.replace("#", ""); return 0xFF000000 | Integer.parseInt(s.substring(0, 6), 16); } catch (Exception e) { return d; } }
    public static String hex(int c) { return String.format("#%06x", c & 0xFFFFFF); }

    /** theme colours (animated in chroma mode) */
    public static int a1() { return chroma ? hsv(((System.currentTimeMillis() % 6000) / 6000f), 0.62f, 1f) : accent; }
    public static int a2() { return chroma ? hsv((((System.currentTimeMillis() + 1800) % 6000) / 6000f), 0.62f, 1f) : accent2; }
    /** colour along the theme gradient, t in 0..1 (repeats), animated slowly */
    public static int grad(float t) {
        float f = (float) ((Math.sin((t + System.currentTimeMillis() / 2600.0) * Math.PI * 2) + 1) / 2);
        return D.mix(a1(), a2(), f);
    }
    public static int hsv(float h, float s, float v) { return 0xFF000000 | java.awt.Color.HSBtoRGB(h, s, v); }
}
