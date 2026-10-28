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

    // ------------------------------------------------------------------ our loading screen (instead of the red Mojang one)
    /** a picture of the mod as a file (the loading screen cannot read the mod's resources yet): copied once to config/asteria-cache */
    static java.io.File res(String name) {
        java.io.File f = new java.io.File(new java.io.File(V.dir(), "asteria-cache"), name);
        try (java.io.InputStream in = Menu.class.getResourceAsStream("/assets/asteria_visuals/textures/gui/" + name)) {
            if (in == null) return null; byte[] b = readAll(in); if (!f.exists() || f.length() != b.length) { f.getParentFile().mkdirs(); java.nio.file.Files.write(f.toPath(), b); } return f;
        } catch (Exception e) { return f.exists() ? f : null; }
    }
    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException { java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); byte[] buf = new byte[16384]; int r; while ((r = in.read(buf)) > 0) o.write(buf, 0, r); return o.toByteArray(); }
    // the loading screen cannot draw text or shapes yet: the logo (in the chosen font and theme colours), the bar and the shading are drawn once into pngs with Java2D
    private static final String[] FONT_FILE = {"ubuntu", "ubuntu", "montserrat", "balsamiq", "pangolin", "neucha", "marmelad", "pacifico", "russo", "philosopher", "unbounded"};
    private static final float[] FONT_SIZE = {9, 9, 8.5f, 9, 9.5f, 10, 9, 9, 8.5f, 9, 7.5f};
    private static java.io.File cache(String name) { java.io.File d = new java.io.File(V.dir(), "asteria-cache"); d.mkdirs(); return new java.io.File(d, name); }
    private static java.awt.Color col(int argb, int alpha) { return new java.awt.Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, alpha); }
    /** "AL" mark + "Asteria Launcher" exactly like the main menu logo (58 px high), 4x resolution */
    static java.io.File logoPng() {
        int fi = Math.max(0, Math.min(10, V.font)), c1 = V.a1(), c2 = V.a2();
        java.io.File f = cache("splash-logo-" + fi + "-" + Integer.toHexString(c1 & 0xFFFFFF) + "-" + Integer.toHexString(c2 & 0xFFFFFF) + ".png"); if (f.exists()) return f;
        try {
            int K = 4, mark = 58 * K, gap = 6 * K, sp = Math.round(4 * 2.6f * K);
            java.awt.Font font; try (java.io.InputStream in = Menu.class.getResourceAsStream("/assets/asteria_visuals/font/" + FONT_FILE[fi] + ".ttf")) { font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in); }
            font = font.deriveFont(FONT_SIZE[fi] * 2.6f * K * 1.12f);
            java.awt.image.BufferedImage probe = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_ARGB); java.awt.Graphics2D pg = probe.createGraphics(); pg.setFont(font);
            java.awt.FontMetrics fm = pg.getFontMetrics(); int wa = fm.stringWidth("Asteria"), wb = fm.stringWidth("Launcher"); pg.dispose();
            int W = mark + gap + wa + sp + wb + 12, H = mark;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_ARGB); java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON); g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC); g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            try (java.io.InputStream in = Menu.class.getResourceAsStream("/assets/asteria_visuals/textures/gui/logo.png")) { java.awt.image.BufferedImage logo = javax.imageio.ImageIO.read(in); g.drawImage(logo, 0, 0, mark, mark, null); }
            g.setFont(font); int base = (H + fm.getAscent() - fm.getDescent()) / 2 + K, x = mark + gap;
            for (int w = 0; w < 2; w++) { String t = w == 0 ? "Asteria" : "Launcher"; int tw = w == 0 ? wa : wb;
                g.setColor(new java.awt.Color(4, 14, 22, 140)); g.drawString(t, x + K, base + K);                                       // soft shadow, like the menu text
                g.setPaint(new java.awt.GradientPaint(x, 0, col(c1, 255), x + tw, 0, col(c2, 255))); g.drawString(t, x, base); x += tw + sp; }
            g.dispose(); javax.imageio.ImageIO.write(img, "png", f); return f;
        } catch (Throwable t) { return res("splash_logo.png"); }
    }
    /** a rounded bar (track or theme gradient fill) and the soft shading over the background */
    static java.io.File barPng(boolean fill) {
        int c1 = V.a1(), c2 = V.a2(); java.io.File f = cache("splash-bar-" + (fill ? Integer.toHexString(c1 & 0xFFFFFF) + "-" + Integer.toHexString(c2 & 0xFFFFFF) : "track") + ".png"); if (f.exists()) return f;
        try { int W = 512, H = 16; java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_ARGB); java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            if (fill) g.setPaint(new java.awt.GradientPaint(0, 0, col(c1, 255), W, 0, col(c2, 255))); else g.setColor(new java.awt.Color(255, 255, 255, 46));
            g.fillRoundRect(0, 0, W, H, H, H); if (fill) { g.setColor(new java.awt.Color(255, 255, 255, 70)); g.fillRoundRect(2, 1, W - 4, H / 2 - 1, H / 2, H / 2); }
            g.dispose(); javax.imageio.ImageIO.write(img, "png", f); return f; } catch (Throwable t) { return null; }
    }
    static java.io.File shadePng() {
        java.io.File f = cache("splash-shade.png"); if (f.exists()) return f;
        try { java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(4, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 256; y++) { int al = (int) (40 + 120 * Math.pow(y / 255.0, 1.6)); for (int x = 0; x < 4; x++) img.setRGB(x, y, (al << 24) | 0x02070A); }
            javax.imageio.ImageIO.write(img, "png", f); return f; } catch (Throwable t) { return null; }
    }
    private static java.io.File splashBg, splashLogo, barTrack, barFill, shade; private static int[] bgSize, logoSize; private static long splashFrom;
    /** the loading screen looks like the main menu: the same picture (own one from the launcher, or ours), the same logo at the same place, and our bar */
    public static void splash(Gfx g, int w, int h, float alpha, float progress) {
        if (alpha <= 0.01f) return; float a = Math.min(1, alpha);
        if (splashLogo == null || System.currentTimeMillis() - splashFrom > 3000) { splashFrom = System.currentTimeMillis(); // the theme / font may change while the game runs (resource reload)
            java.io.File own = new java.io.File(V.dir(), "asteria-bg.png"); splashBg = own.exists() ? own : res("menu_bg.png"); splashLogo = logoPng(); barTrack = barPng(false); barFill = barPng(true); shade = shadePng();
            bgSize = splashBg == null ? null : imgSize(splashBg); logoSize = splashLogo == null ? null : imgSize(splashLogo); }
        if (splashBg != null && bgSize != null && bgSize[0] > 0) { float s = Math.max(w / (float) bgSize[0], h / (float) bgSize[1]), bw = bgSize[0] * s, bh = bgSize[1] * s;
            g.imageA("splbg:" + splashBg.lastModified(), splashBg, (w - bw) / 2, (h - bh) / 2, bw, bh, a); }
        if (shade != null) g.imageA("splshade:1", shade, 0, 0, w, h, a);
        if (splashLogo != null && logoSize != null && logoSize[0] > 0) { float lh = 58, lw = lh * logoSize[0] / logoSize[1]; if (lw > w * 0.9f) { lw = w * 0.9f; lh = lw * logoSize[1] / logoSize[0]; } // the main menu logo: 58 px high, 30 px from the top
            g.imageA("spllogo:" + splashLogo.getName(), splashLogo, (w - lw) / 2, 18, lw, lh, a); }
        float bw = Math.min(w * 0.42f, 240), bx = (w - bw) / 2, by = h * 0.82f, bh = 5;
        if (barTrack != null) g.imageA("spltrack:1", barTrack, bx, by, bw, bh, a);
        if (barFill != null) g.imageA("splfill:" + barFill.getName(), barFill, bx, by, Math.max(bh, bw * Math.max(0, Math.min(1, progress))), bh, a);
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
    private static long dbgBg, dbgBg2;
    /** an own picture was chosen in the launcher (the base mod draws our art in the main menu otherwise) */
    public static boolean ownBg() { return new java.io.File(V.dir(), "asteria-bg.png").exists(); }
    public static void background(Gfx g, int w, int h) {
        if (Fx.DEBUG && System.currentTimeMillis() - dbgBg2 > 4000) { dbgBg2 = System.currentTimeMillis(); System.out.println("[Asteria debug] background() own=" + new java.io.File(V.dir(), "asteria-bg.png").getAbsolutePath()); }
        D.g = g;
        java.io.File own = new java.io.File(V.dir(), "asteria-bg.png"), blur = new java.io.File(V.dir(), "asteria-bg-blur.png");
        if (own.exists()) { java.io.File f = !onTitle && V.bgBlur && blur.exists() ? blur : own; int[] sz = imgSize(f);
            if (Fx.DEBUG && System.currentTimeMillis() - dbgBg > 4000) { dbgBg = System.currentTimeMillis(); System.out.println("[Asteria debug] bg title=" + onTitle + " file=" + f + " size=" + java.util.Arrays.toString(sz)); }
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
