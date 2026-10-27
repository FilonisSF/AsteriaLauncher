package ru.safimine.asteria.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Version-independent part of the free AsteriaLauncher mod (no Minecraft classes here).
 * Every Minecraft version only supplies a small {@link Platform} adapter.
 *
 *  - who plays from the launcher (for the icon in front of the nick in the tab list)
 *  - skins / capes of launcher players, own skin & cape straight from local files (updated live while the game runs)
 *  - "live settings": the launcher writes options.txt and drops asteria-live.flag, the game re-reads the settings at once
 */
public final class Asteria {
    public interface Platform {
        /** schedule on the render thread */
        void onClient(Runnable r);
        /** register a PNG as a texture (render thread) and return its id (an Identifier) */
        Object registerTexture(String path, byte[] png) throws Exception;
        /** re-read options.txt and apply it (render thread) */
        void reloadOptions();
        /** "menu", "sp" or "mp:<server address>" — for Discord Rich Presence in the launcher */
        String status();
    }

    public static String API, USER, GAME_DIR;
    public static Platform P;
    public static final Set<String> ONLINE = ConcurrentHashMap.newKeySet();
    private static final Map<String, Object> SKIN = new ConcurrentHashMap<>(), CAPE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SLIM = new ConcurrentHashMap<>();
    private static final Map<String, Integer> HASH = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST = new ConcurrentHashMap<>(), SEEN = new ConcurrentHashMap<>();
    private static final Map<String, Long> LOCAL_MTIME = new ConcurrentHashMap<>();
    private static ScheduledExecutorService EX;

    private Asteria() {}

    public static void start(Platform p) {
        P = p;
        USER = System.getProperty("asteria.user");
        API = System.getProperty("asteria.api");
        GAME_DIR = System.getProperty("asteria.gamedir", new File(".").getAbsolutePath());
        if (USER != null) ONLINE.add(USER.toLowerCase());
        EX = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "Asteria"); t.setDaemon(true); return t; });
        // every task is wrapped: an exception must never cancel a repeating task
        EX.scheduleWithFixedDelay(safe(Asteria::localTextures), 3, 2, TimeUnit.SECONDS);
        EX.scheduleWithFixedDelay(safe(Asteria::liveOptions), 3, 1, TimeUnit.SECONDS);
        EX.scheduleWithFixedDelay(safe(Asteria::writeStatus), 5, 3, TimeUnit.SECONDS);
        if (API != null && !API.isEmpty()) EX.scheduleWithFixedDelay(safe(Asteria::poll), 2, 3, TimeUnit.SECONDS); // others see a new skin / cape within ~3 s (change stamps)
    }

    private static Runnable safe(Runnable r) { return () -> { try { r.run(); } catch (Throwable ignored) {} }; }

    /** true if this player plays from AsteriaLauncher (we always count ourselves) */
    public static boolean isLauncherPlayer(String name) {
        return name != null && ONLINE.contains(name.toLowerCase());
    }
    public static Object skin(String name) { return name == null ? null : SKIN.get(name.toLowerCase()); }
    /** the cape texture; an animated cape (GIF from the launcher) gives its current frame */
    public static Object cape(String name) {
        Object o = name == null ? null : CAPE.get(name.toLowerCase());
        if (o instanceof Object[]) { Object[] f = (Object[]) o; return f.length == 0 ? null : f[(int) ((System.currentTimeMillis() / 90) % f.length)]; }
        return o;
    }
    public static boolean slim(String name) { return name != null && Boolean.TRUE.equals(SLIM.get(name.toLowerCase())); }

    // ---------------------------------------------------------------- own skin / cape from the launcher's files
    private static void localTextures() {
        if (USER == null) return;
        String key = USER.toLowerCase();
        SLIM.put(key, "1".equals(System.getProperty("asteria.slim")) || new File(dir("skins"), "slim.flag").exists());
        for (String kind : new String[]{"skin", "cape"}) {
            String path = System.getProperty("asteria." + kind);
            File f = path != null ? new File(path) : new File(dir("skins"), kind.equals("skin") ? "current.png" : "cape.png");
            long m = f.exists() ? f.lastModified() : -1;
            Long old = LOCAL_MTIME.get(kind);
            if (old != null && old == m) continue;
            if (m < 0) { (kind.equals("skin") ? SKIN : CAPE).remove(key); LOCAL_MTIME.put(kind, m); continue; } // removed in the launcher
            try { register(key, kind, Files.readAllBytes(f.toPath()), () -> LOCAL_MTIME.put(kind, m)); } catch (Exception ignored) {}
        }
    }
    private static File dir(String sub) {
        String base = System.getProperty("asteria.root");
        return new File(base != null ? base : GAME_DIR, sub);
    }

    // ---------------------------------------------------------------- other launcher players (backend)
    private static final Map<String, Long> REV = new ConcurrentHashMap<>();
    private static boolean revsOk = true; private static long onlineAt;
    private static void poll() {
        long t0 = System.currentTimeMillis();
        if (revsOk) try { // {"nick": stamp, ...} of everyone online: re-download those whose stamp changed
            String s = new String(get(API + "/revs"), StandardCharsets.UTF_8).replaceAll("[{}\" ]", "");
            if (!s.isEmpty()) for (String kv : s.split(",")) { int i = kv.lastIndexOf(':'); if (i <= 0) continue; String n = kv.substring(0, i).toLowerCase(); long r; try { r = Long.parseLong(kv.substring(i + 1)); } catch (Exception e) { continue; }
                SEEN.put(n, t0); Long old = REV.put(n, r); if (old != null && old != r) LAST.remove(n); }
        } catch (Exception e) { revsOk = false; } // an old backend without /revs: the plain 10 s / 30 s polling
        if (!revsOk && t0 - onlineAt < 10_000) return; onlineAt = t0;
        try {
            String s = new String(get(API + "/online"), StandardCharsets.UTF_8).replaceAll("[\\[\\]\" ]", "");
            long now = System.currentTimeMillis();
            if (!s.isEmpty()) for (String n : s.split(",")) SEEN.put(n.toLowerCase(), now);
            SEEN.values().removeIf(t -> now - t > 180_000); // a player stays marked for 3 min: a short backend hiccup must not hide the AL icon
            ONLINE.retainAll(SEEN.keySet()); ONLINE.addAll(SEEN.keySet());
            if (USER != null) ONLINE.add(USER.toLowerCase());
        } catch (Exception ignored) {}
        for (String n : ONLINE) if (USER == null || !n.equals(USER.toLowerCase())) refresh(n);
    }
    private static void refresh(String key) {
        long now = System.currentTimeMillis();
        Long last = LAST.get(key);
        if (last != null && now - last < (revsOk ? 300_000 : 30_000)) return;
        LAST.put(key, now);
        for (String kind : new String[]{"skin", "cape"}) {
            try {
                byte[] png = get(API + "/" + kind + "/" + key + ".png");
                int h = Arrays.hashCode(png);
                Integer old = HASH.put(kind + key, h);
                if (old == null || old != h) register(key, kind, png, null);
            } catch (Exception e) { (kind.equals("skin") ? SKIN : CAPE).remove(key); HASH.remove(kind + key); }
        }
        try { SLIM.put(key, new String(get(API + "/skinmeta/" + key), StandardCharsets.UTF_8).contains("true")); } catch (Exception ignored) {}
    }

    private static int gen = 0;
    /** registers on the render thread; onDone runs only if it worked (so a failed attempt is retried later) */
    private static void register(String key, String kind, byte[] png, Runnable onDone) {
        final String path = kind + "/" + key.replaceAll("[^a-z0-9_]", "_") + "_" + (gen++);
        // animated cape: frames of width x width/2 stacked from top to bottom -> one texture per frame
        final java.util.List<byte[]> frames = kind.equals("cape") ? splitFrames(png) : null;
        P.onClient(() -> {
            try {
                if (frames != null && frames.size() > 1) { Object[] ids = new Object[frames.size()]; for (int i = 0; i < ids.length; i++) ids[i] = P.registerTexture(path + "_f" + i, frames.get(i)); CAPE.put(key, ids); if (onDone != null) onDone.run(); return; }
                (kind.equals("skin") ? SKIN : CAPE).put(key, P.registerTexture(path, png));
                if (onDone != null) onDone.run();
            } catch (Throwable ignored) {}
        });
    }

    /** null / one element = a normal picture; otherwise the PNG of every frame (at most 24) */
    private static java.util.List<byte[]> splitFrames(byte[] png) {
        try {
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png)); if (im == null) return null;
            int w = im.getWidth(), fh = Math.max(1, w / 2), n = Math.min(24, Math.round(im.getHeight() / (float) fh)); if (n < 2) return null;
            java.util.List<byte[]> out = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) { java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(im.getSubimage(0, i * fh, w, fh), "png", o); out.add(o.toByteArray()); }
            return out;
        } catch (Throwable e) { return null; }
    }

    // ---------------------------------------------------------------- live settings from the launcher
    private static void liveOptions() {
        File flag = new File(GAME_DIR, "asteria-live.flag");
        if (!flag.exists()) return;
        if (!flag.delete()) return;
        P.onClient(() -> { try { P.reloadOptions(); } catch (Throwable ignored) {} });
    }

    // ---------------------------------------------------------------- where the player is (read by the launcher for Discord)
    private static String lastStatus = "";
    private static long lastStatusAt = 0;
    private static void writeStatus() {
        String s = P.status();
        long now = System.currentTimeMillis();
        if (s == null || (s.equals(lastStatus) && now - lastStatusAt < 10000)) return; // rewritten every 10 s: the launcher knows it is fresh
        lastStatus = s; lastStatusAt = now;
        String json = "{\"state\":\"" + s.replace("\\", "").replace("\"", "") + "\",\"ts\":" + System.currentTimeMillis() + "}";
        try { Files.write(new File(GAME_DIR, "asteria-status.json").toPath(), json.getBytes(StandardCharsets.UTF_8)); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- http
    public static byte[] get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(5000); c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "AsteriaMod");
        if (c.getResponseCode() != 200) throw new Exception("http " + c.getResponseCode());
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int r;
            while ((r = in.read(b)) > 0) o.write(b, 0, r);
            return o.toByteArray();
        }
    }
    public static void post(String u, String json) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(5000); c.setReadTimeout(5000);
            c.setRequestProperty("content-type", "application/json");
            try (OutputStream o = c.getOutputStream()) { o.write(json.getBytes(StandardCharsets.UTF_8)); }
            c.getResponseCode();
        } catch (Exception ignored) {}
    }
}
