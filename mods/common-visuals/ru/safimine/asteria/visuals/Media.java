package ru.safimine.asteria.visuals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** "Now playing": the launcher writes config/asteria-media.json (Windows media controls) every ~1.5 s while the game runs. */
public final class Media {
    private Media() {}
    public static String title = "", artist = "", cover = "";
    public static boolean playing;
    private static double pos, dur; private static long at, checked, mtime;

    /** fresh track info, or false when nothing plays / the launcher stopped updating */
    public static boolean active() {
        long now = System.currentTimeMillis();
        if (now - checked > 100) { checked = now; read(); }
        return playing && !title.isEmpty() && now - at < 8000;
    }
    /** playback position in seconds, moving smoothly between file updates */
    public static double pos() { return Math.min(dur, pos + (playing ? (System.currentTimeMillis() - at) / 1000.0 : 0)); }
    public static double dur() { return dur; }

    /** cover art written by the launcher (128x128 png), null when the track has none */
    public static File coverFile() { File f = new File(V.dir(), "asteria-media-cover.png"); return cover.isEmpty() || !f.exists() ? null : f; }
    /** asks the launcher to play/pause, skip or go back ("toggle" | "next" | "prev") */
    public static void cmd(String c) {
        try { Files.write(new File(V.dir(), "asteria-media-cmd.txt").toPath(), c.getBytes(StandardCharsets.UTF_8)); } catch (Exception ignored) {}
        if (c.equals("toggle")) playing = !playing; // instant feedback until the next update
        if (c.equals("next") || c.equals("prev")) { pos = 0; checked = 0; }
    }
    /** 83 -> "1:23" */
    public static String time(double s) { int t = (int) Math.max(0, s); return (t / 60) + ":" + String.format("%02d", t % 60); }
    /** shown even when paused (the launcher still reports the track) */
    public static boolean present() { long now = System.currentTimeMillis(); if (now - checked > 100) { checked = now; read(); } return !title.isEmpty() && now - at < 8000; }
    private static void read() {
        try {
            File f = new File(V.dir(), "asteria-media.json"); if (!f.exists()) { playing = false; title = ""; return; }
            long m = f.lastModified(); if (m == mtime) return; mtime = m;
            JsonObject o = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            title = V.str(o, "title"); artist = V.str(o, "artist"); playing = V.b(o, "playing", false); cover = V.str(o, "cover");
            pos = V.d(o, "pos", 0); dur = V.d(o, "dur", 0); at = (long) V.d(o, "at", System.currentTimeMillis());
        } catch (Exception e) { playing = false; }
    }
}
