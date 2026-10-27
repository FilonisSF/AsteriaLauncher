package ru.safimine.asteria.visuals;

import java.util.function.Function;

/**
 * Asteria style for Minecraft's own menus: neon buttons with icons, the AsteriaLauncher logo instead of
 * "Minecraft Java Edition", and the Asteria background on every menu screen. The version mixins only call these.
 */
public final class Menu {
    private Menu() {}
    /** translates a vanilla key (set by the version code), used to recognise buttons by their text */
    public static Function<String, String> tr = k -> k;

    public static boolean on() { return V.menu; }
    /** small icon buttons (language, accessibility, Realms news) keep their own look: our text would cover the icon */
    public static boolean styled(int w) { return V.menu && w >= 40; }

    /** icon on the left of well-known buttons, by their (translated) text; null = text only */
    static String iconFor(String label) {
        String[][] map = {{"menu.singleplayer", "play"}, {"menu.multiplayer", "users"}, {"menu.options", "gear"}, {"menu.quit", "exit"},
            {"options.language", "globe"}, {"gui.done", "check"}, {"gui.back", "back"}, {"gui.cancel", "close"}, {"menu.returnToMenu", "exit"},
            {"menu.disconnect", "exit"}, {"menu.returnToGame", "play"}, {"selectWorld.select", "play"}, {"selectWorld.create", "plus"}};
        for (String[] e : map) if (label.equals(tr.apply(e[0]))) return e[1];
        if (label.equals(tr.apply("selectServer.select"))) return "play";
        if (label.equals(tr.apply("selectServer.add"))) return "plus";
        return null;
    }

    /** one button: dark glass, neon rim (brighter on hover), an icon cell on the left for known buttons */
    public static void button(Gfx g, float x, float y, float w, float h, boolean hover, boolean active, String label) {
        D.g = g;
        float r = Math.min(5, h / 3.5f); int a1 = V.a1(), a2 = V.a2();
        float glow = D.anim("btn." + (int) x + "." + (int) y, hover && active ? 1 : 0, 14);
        if (glow > 0.02f) for (int i = 3; i >= 1; i--) D.round(x - i, y - i, x + w + i, y + h + i, r + i, D.a(a1, 0.10f * glow / i), D.a(a2, 0.10f * glow / i));
        D.round(x, y, x + w, y + h, r, D.a(a1, active ? 0.55f + 0.45f * glow : 0.25f), D.a(a2, active ? 0.55f + 0.45f * glow : 0.25f)); // rim
        D.round(x + 1, y + 1, x + w - 1, y + h - 1, Math.max(0, r - 1), D.alpha(0x061A22, active ? 225 : 160));                  // body
        D.round(x + 1, y + 1, x + w - 1, y + h - 1, Math.max(0, r - 1), D.a(a1, 0.10f + 0.12f * glow), D.a(a2, 0.03f));            // tint
        D.round(x + r, y + 1, x + w - r, y + 1.8f, 0.4f, D.alpha(0xFFFFFF, 0), D.alpha(0xFFFFFF, active ? 110 : 40));              // top light
        String ic = h >= 16 ? iconFor(label) : null; float tx0 = x + 4, tw = w - 8;
        int tc = active ? (hover ? 0xFFFFFFFF : 0xFFE6FBFF) : 0xFF6F8B99;
        if (ic != null) {
            float cw = h; // square icon cell with a divider
            D.round(x + 1, y + 1, x + cw, y + h - 1, Math.max(0, r - 1), D.a(a1, 0.18f + 0.15f * glow), D.a(a2, 0.10f));
            D.rect(x + cw, y + 3, x + cw + 0.6f, y + h - 3, D.a(a1, 0.7f));
            float is = Math.min(12, h - 8); D.icon(ic, x + (cw - is) / 2, y + (h - is) / 2, is, tc);
            tx0 = x + cw + 4; tw = w - cw - 8;
        }
        String s = label; while (D.w(s) > tw && s.length() > 2) s = s.substring(0, s.length() - 2); if (!s.equals(label)) s = s + "…";
        g.text(s, tx0 + (tw - D.w(s)) / 2f, y + (h - 8) / 2f + 0.5f, tc, true);
    }

    /** AsteriaLauncher logo where the Minecraft logo was: the AL mark and the name with the theme gradient */
    public static void logo(Gfx g, int screenW, float y) {
        D.g = g;
        String a = "Asteria", b = "Launcher"; float sc = 2.6f, sp = 4, tw = (D.w(a) + sp + D.w(b)) * sc, mark = 58, gap = 6, total = mark + gap + tw, x = (screenW - total) / 2f;
        g.texture("textures/gui/logo.png", 512, 512, x, y - 12, mark, mark);
        g.push(x + mark + gap, y + 17 - 4.5f * sc, sc); D.gradText(a, 0, 0, true); D.gradText(b, D.w(a) + sp, 0, true); g.pop(); // "Asteria Launcher": two words with a small gap
    }

    // ------------------------------------------------------------------ our loading screen (instead of the red Mojang one): only shapes, the textures may not be loaded yet
    public static void splash(Gfx g, int w, int h, float progress, float alpha) {
        if (alpha <= 0.01f) return; D.g = g; double t = System.currentTimeMillis() / 1000.0; float a = Math.min(1, alpha);
        D.vgrad(0, 0, w, h, D.alpha(0x04101A, (int) (255 * a)), D.alpha(0x061C28, (int) (255 * a)));
        float cx = w / 2f, cy = h * 0.42f;
        for (int i = 7; i >= 1; i--) D.circle(cx, cy, 30 + i * 9, D.a(V.a1(), 0.025f * a));
        for (int k = 0; k < 3; k++) { double an = t * (1.2 + k * 0.4) + k * 2.1; float rr = 46 + k * 9; D.ring(cx, cy, rr, rr + 1.4f, D.a(V.grad(k * 0.3f), 0.55f * a), D.a(V.grad(k * 0.3f + 0.2f), 0.1f * a));
            D.circle(cx + (float) Math.cos(an) * (rr + 0.7f), cy + (float) Math.sin(an) * (rr + 0.7f), 2.2f, D.a(0xFFFFFFFF, 0.9f * a)); }
        D.round(cx - 26, cy - 26, cx + 26, cy + 26, 14, D.a(V.a1(), a), D.a(V.a2(), a));
        D.round(cx - 24, cy - 24, cx + 24, cy + 24, 12, D.alpha(0x04101A, (int) (235 * a)));
        // "AL" drawn with shapes: A = two legs + bar, L = stem + foot
        int c = D.a(0xFFFFFFFF, a); D.g.poly(new float[]{cx - 15, cy + 12, cx - 9, cy - 13, cx - 5, cy - 13, cx - 11, cy + 12}, new int[]{c, c, c, c});
        D.g.poly(new float[]{cx - 5, cy - 13, cx - 1, cy - 13, cx + 5, cy + 12, cx + 1, cy + 12}, new int[]{c, c, c, c}); D.rect(cx - 10, cy + 2, cx, cy + 5.5f, c);
        D.rect(cx + 7, cy - 13, cx + 11, cy + 12, D.a(V.a2(), a)); D.rect(cx + 7, cy + 8.5f, cx + 18, cy + 12, D.a(V.a2(), a));
        float bw = Math.min(260, w * 0.5f), by = cy + 78; D.round(cx - bw / 2, by, cx + bw / 2, by + 5, 2.5f, D.alpha(0xFFFFFF, (int) (30 * a)));
        D.round(cx - bw / 2, by, cx - bw / 2 + Math.max(5, bw * Math.min(1, progress)), by + 5, 2.5f, D.a(V.a1(), a), D.a(V.a2(), a));
        D.g.text("AsteriaLauncher", cx - D.w("AsteriaLauncher") / 2f, by + 12, D.a(0xFFE6FBFF, a * 0.9f), false);
    }
    // ------------------------------------------------------------------ music card on the pause menu (ESC): track, time, controls, volume
    private static final java.util.List<float[]> PB = new java.util.ArrayList<>(); private static final java.util.List<String> PBC = new java.util.ArrayList<>(); private static boolean wasDown;
    public static void pauseMusic(Gfx g, int W, int H, float mx, float my, boolean down) {
        D.g = g; PB.clear(); PBC.clear(); if (!Media.present()) { wasDown = down; return; }
        float w = 190, h = 74, x = W - w - 8, y = 8;
        D.panel(x, y, x + w, y + h, 8);
        java.io.File cv = Media.coverFile(); if (cv == null || !g.image(Media.cover, cv, x + 6, y + 6, 36, 36)) { D.round(x + 6, y + 6, x + 42, y + 42, 7, V.a1(), V.a2()); D.icon("music", x + 16, y + 16, 16, 0xFFFFFFFF); }
        String t = Media.title; while (D.w(t) > w - 56 && t.length() > 3) t = t.substring(0, t.length() - 2); if (!t.equals(Media.title)) t += "…";
        g.text(t, x + 48, y + 8, 0xFFFFFFFF, true); D.textS(Media.artist.isEmpty() ? (Media.active() ? "Играет" : "Пауза") : Media.artist, x + 48, y + 20, 0.8f, 0xFF9FB3C0);
        double d = Media.dur(), p = Media.pos(); float f = d > 0 ? (float) Math.min(1, p / d) : 0;
        D.round(x + 48, y + 33, x + w - 8, y + 35, 1, 0x30FFFFFF); D.round(x + 48, y + 33, x + 48 + (w - 56) * f, y + 35, 1, V.a1(), V.a2());
        if (d > 0) D.textS(Media.time(p) + " / " + Media.time(d), x + 48, y + 37, 0.65f, 0xFF9FB3C0);
        String[][] b = {{"prev", "prev"}, {Media.active() ? "pause" : "play", "toggle"}, {"next", "next"}, {"vol-", "voldown"}, {"mute", "mute"}, {"vol+", "volup"}};
        float bx = x + 8, by = y + 50, bw = (w - 16) / b.length;
        for (String[] k : b) { boolean hov = mx >= bx && mx < bx + bw - 3 && my >= by && my < by + 18;
            D.round(bx, by, bx + bw - 3, by + 18, 6, hov ? D.a(V.a1(), 0.5f) : 0x22FFFFFF, hov ? D.a(V.a2(), 0.5f) : 0x22FFFFFF);
            if (k[0].startsWith("vol")) { String s = k[0].equals("vol+") ? "+" : "−"; g.text(s, bx + (bw - 3 - D.w(s)) / 2, by + 5, 0xFFFFFFFF, false); }
            else D.icon(k[0].equals("mute") ? "volume" : k[0], bx + (bw - 3 - 10) / 2, by + 4, 10, 0xFFFFFFFF);
            PB.add(new float[]{bx, by, bx + bw - 3, by + 18}); PBC.add(k[1]); bx += bw; }
        if (down && !wasDown) for (int i = 0; i < PB.size(); i++) { float[] r = PB.get(i); if (mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3]) { Media.cmd(PBC.get(i)); break; } }
        wasDown = down;
    }
    /** the Asteria background on menu screens (cover fit) with a soft dark veil for readable text */
    /** the title screen is being drawn (set by the version mixins): the own picture stays sharp there */
    public static boolean onTitle;
    private static int[] imgSize(java.io.File f) { try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(f))) { in.skipBytes(16); return new int[]{in.readInt(), in.readInt()}; } catch (Exception e) { return null; } }
    public static void background(Gfx g, int w, int h) {
        D.g = g;
        java.io.File own = new java.io.File(V.dir(), "asteria-bg.png"), blur = new java.io.File(V.dir(), "asteria-bg-blur.png");
        if (own.exists()) { java.io.File f = !onTitle && V.bgBlur && blur.exists() ? blur : own; int[] sz = imgSize(f);
            if (sz != null && sz[0] > 0 && sz[1] > 0) { float s = Math.max(w / (float) sz[0], h / (float) sz[1]), bw = sz[0] * s, bh = sz[1] * s;
                if (g.image((f == blur ? "bgb:" : "bg:") + f.lastModified(), f, (w - bw) / 2, (h - bh) / 2, bw, bh)) { D.vgrad(0, 0, w, h, D.alpha(0x02070A, onTitle ? 30 : 90), D.alpha(0x02070A, onTitle ? 90 : 150)); return; } } }
        if (V.menuBg == 1) { float s = Math.max(w / 1280f, h / 720f), bw = 1280 * s, bh = 720 * s;
            g.texture("textures/gui/menu_bg.png", 1280, 720, (w - bw) / 2, (h - bh) / 2, bw, bh);
            D.vgrad(0, 0, w, h, D.alpha(0x02070A, 70), D.alpha(0x02070A, 140)); return; }
        // smooth HD background: deep gradient, slow aurora, twinkling stars, the theme glow — no pixels at any resolution
        double t = System.currentTimeMillis() / 1000.0;
        D.vgrad(0, 0, w, h, 0xFF040B16, 0xFF071A26);
        for (int k = 0; k < 3; k++) { float cx = (float) (w * (0.25 + 0.25 * k) + Math.sin(t * 0.13 + k * 2) * w * 0.08), cy = (float) (h * (0.32 + 0.12 * Math.sin(t * 0.1 + k))), rr = h * (0.55f + 0.1f * k);
            int c = k == 1 ? V.a2() : V.a1(); for (int i = 8; i >= 1; i--) D.circle(cx, cy, rr * i / 8f, D.a(c, 0.022f)); }
        java.util.Random rn = new java.util.Random(7);
        for (int i = 0; i < 140; i++) { float x = rn.nextFloat() * w, y = rn.nextFloat() * h * 0.85f, tw = (float) (0.5 + 0.5 * Math.sin(t * (0.6 + rn.nextFloat() * 1.4) + i)), sz = 0.35f + rn.nextFloat() * 0.9f;
            D.circle(x, y, sz, D.alpha(0xFFFFFF, (int) (50 + 170 * tw))); }
        D.vgrad(0, h * 0.62f, w, h, D.alpha(0x02070A, 0), D.alpha(0x02070A, 200));
    }
}
