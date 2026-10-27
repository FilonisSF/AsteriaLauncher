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

    /** the Asteria background on menu screens (cover fit) with a soft dark veil for readable text */
    public static void background(Gfx g, int w, int h) {
        D.g = g;
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
