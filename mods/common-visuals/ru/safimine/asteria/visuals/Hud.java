package ru.safimine.asteria.visuals;

import java.text.SimpleDateFormat;
import java.util.*;

/** All 2D visuals: HUD elements, notifications, custom crosshair. Positions are fractions of the screen (shared with the launcher). */
public final class Hud {
    private Hud() {}
    public static final class Box { public final V.Mod mod; public float x, y, w, h; Box(V.Mod m) { mod = m; } }
    /** boxes drawn this frame (used by the HUD editor for dragging) */
    public static final List<Box> BOXES = new ArrayList<>();
    public static boolean editing = false;

    private static int frames, fps, bossBars; private static long fpsAt;
    private static final ArrayDeque<Long> LMB = new ArrayDeque<>(), RMB = new ArrayDeque<>();
    private static boolean lmbWas, rmbWas;
    private static double lx, lz, speed; private static long speedAt;
    private static final SimpleDateFormat HM = new SimpleDateFormat("HH:mm");
    private static final long START = System.currentTimeMillis();

    public static int fps() { return fps; }

    public static void render(Gfx g, Game game) {
        D.g = g; D.frame(); V.tick();
        long now = System.currentTimeMillis();
        frames++; if (now - fpsAt >= 1000) { fps = frames; frames = 0; fpsAt = now; }
        BOXES.clear();
        if (!V.licensed()) { if (now - START < 25000 && !game.hudHidden()) locked(game); return; }
        if (game.hudHidden() && !editing) return;
        if (game.screenOpen() && !game.chatOpen() && !editing) return;
        if (game.tabOpen() && !editing) return; // the player list is open: nothing of ours on top of it // under ESC / inventory / any menu the HUD would sit on top of it
        // clicks per second
        boolean lmb = game.key(6) && !game.screenOpen(), rmb = game.key(7) && !game.screenOpen();
        if (lmb && !lmbWas) LMB.add(now); if (rmb && !rmbWas) RMB.add(now); lmbWas = lmb; rmbWas = rmb;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.pollFirst();
        if (now - speedAt >= 250) { double dx = game.x() - lx, dz = game.z() - lz; double inst = speedAt == 0 ? 0 : Math.sqrt(dx * dx + dz * dz) / ((now - speedAt) / 1000.0); speed = inst > 100 ? speed : speed * 0.5 + inst * 0.5; lx = game.x(); lz = game.z(); speedAt = now; }

        int W = game.width(), H = game.height(); bossBars = game.bossBars();
        if (V.on("frame") && !game.screenOpen()) frame(W, H);
        if (V.on("crosshair") && !game.screenOpen()) crosshair(W, H, game);
        float s = V.scale;
        place("watermark", W, H, s, () -> watermarkSize(game), () -> watermark(game));
        place("info", W, H, s, () -> infoSize(game), () -> info(game));
        place("keystrokes", W, H, s, () -> new float[]{70, keysH()}, () -> keystrokes(game));
        List<Game.Item> armor = game.armor();
        if (!armor.isEmpty() || editing) place("armor", W, H, s, () -> armorSize(armor), () -> armor(armor));
        List<Game.Effect> eff = game.effects();
        if (!eff.isEmpty() || editing) place("effects", W, H, s, () -> effSize(eff), () -> effects(eff));
        Object t = Fx.target(game);
        if (t != null || editing) place("targethud", W, H, s, () -> targetSize(game, t), () -> target(game, t));
        place("arraylist", W, H, s, Hud::listSize, Hud::arraylist);
        place("notifications", W, H, s, Hud::notesSize, Hud::notes);
        // wave 2
        place("island", W, H, s, () -> islandSize(game), () -> island(game));
        Object[] inv = game.inventory();
        if (inv.length > 0) {
            place("hotbar", W, H, s, () -> hotbarSize(game), () -> hotbar(game, inv));
            place("inventory", W, H, s, () -> new float[]{9 * 19 + 7, 3 * 19 + 19}, () -> inventory(game, inv));
            place("totems", W, H, s, () -> new float[]{40, 22}, () -> totems(game, inv));
            List<Object> cd = cooldowns(game, inv);
            if (!cd.isEmpty() || editing) place("cooldowns", W, H, s, () -> new float[]{104, Math.max(1, cd.size()) * 20 + 4}, () -> cooldownList(game, cd));
        }
        if (Media.present() || editing) place("music", W, H, s, () -> new float[]{156, 36}, Hud::music);
        if (game.serverInfo() != null || editing) place("servercard", W, H, s, () -> serverSize(game), () -> serverCard(game));
        if (V.on("nametags") && !editing) try { nametags(game, W, H); } catch (Throwable ignored) {}
        if (V.on("markers") && !editing) try { marks(game, W, H); } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------------ wave 2: island, hotbar, inventory, totems, cooldowns, music
    private static String islandText(Game game) {
        Note n = NOTES.isEmpty() ? null : NOTES.get(NOTES.size() - 1);
        if (n != null && System.currentTimeMillis() - n.at < 2600) return n.title + "  —  " + n.text;
        if (Media.present() && Media.active() && (System.currentTimeMillis() / 5000) % 2 == 1) return Media.title + (Media.artist.isEmpty() ? "" : "  —  " + Media.artist);
        List<String> p = new ArrayList<>(); p.add("Asteria");
        if (V.bool("island", "time")) p.add(HM.format(new Date()));
        p.add(fps + " fps"); int ping = game.ping(); if (ping >= 0) p.add(ping + " ms");
        return String.join("  ·  ", p);
    }
    private static float[] islandSize(Game game) { return new float[]{D.anim("island.w", Math.min(260, D.w(islandText(game)) + 22), 10), 18}; }
    private static void island(Game game) {
        String t = islandText(game); float w = islandSize(game)[0];
        D.shadow(0, 0, w, 18, 9, 1.2f);
        int base = D.alpha(0x05090D, (int) (255 * Math.max(0.55f, V.bg)));
        D.round(0, 0, w, 18, 9, D.mix(base, D.alpha(V.a1() & 0xFFFFFF, 255), 0.22f), D.mix(base, D.alpha(V.a2() & 0xFFFFFF, 255), 0.22f)); // theme-tinted glass
        D.round(0.6f, 0.6f, w - 0.6f, 1.6f, 0.5f, D.alpha(0xFFFFFF, 0), D.alpha(0xFFFFFF, 60));
        D.round(w * 0.2f, 16.6f, w * 0.8f, 17.6f, 0.5f, D.a(V.a1(), 0.9f), D.a(V.a2(), 0.9f));
        String shown = t; while (D.w(shown) > w - 16 && shown.length() > 3) shown = shown.substring(0, shown.length() - 2);
        if (!shown.equals(t)) shown = shown + "…";
        D.g.text(shown, (w - D.w(shown)) / 2, 5, 0xFFFFFFFF, V.shadow);
    }
    private static float[] hotbarSize(Game game) { return new float[]{9 * 20 + 4 + (game.offhand() != null ? 26 : 0), 22}; }
    private static void slot(Game game, Object st, float x, float y, float sz) {
        if (st == null) return; D.g.item(st, (int) (x + (sz - 16) / 2), (int) (y + (sz - 16) / 2));
        int n = game.count(st); if (n > 1) { String c = String.valueOf(n); D.g.textTop(c, x + sz - 1 - D.w(c), y + sz - 8, 0xFFFFFFFF); }
    }
    private static void hotbar(Game game, Object[] inv) {
        Object off = game.offhand(); float ox = off != null ? 26 : 0;
        if (off != null) { D.panel(0, 0, 22, 22, 6, false); slot(game, off, 1, 1, 20); }
        D.panel(ox, 0, ox + 9 * 20 + 4, 22, 6, false);
        float sx = D.anim("hotbar.sel", game.selectedSlot() * 20, 22);
        D.round(ox + 2 + sx, 1, ox + 2 + sx + 20, 21, 5, D.a(V.a1(), 0.55f), D.a(V.a2(), 0.55f));
        D.round(ox + 2 + sx + 4, 20, ox + 2 + sx + 16, 21, 0.5f, 0xFFFFFFFF);
        for (int i = 0; i < 9; i++) slot(game, inv[i], ox + 2 + i * 20, 1, 20);
    }
    private static void inventory(Game game, Object[] inv) {
        D.panel(0, 0, 9 * 19 + 7, 3 * 19 + 19, 6);
        D.gradText("Инвентарь", 5, 4, true);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) { float x = 4 + c * 19, y = 15 + r * 19;
            D.round(x, y, x + 18, y + 18, 3, 0x18FFFFFF); slot(game, inv[9 + r * 9 + c], x + 1, y + 1, 16); }
    }
    private static Object lastTotem;
    private static void totems(Game game, Object[] inv) {
        int n = 0; Object one = null;
        for (Object st : inv) if (st != null && game.itemId(st).endsWith("totem_of_undying")) { n += game.count(st); one = st; }
        Object off = game.offhand(); if (off != null && game.itemId(off).endsWith("totem_of_undying")) { n += game.count(off); one = off; }
        if (lastTotem == null) lastTotem = game.totemStack();
        D.panel(0, 0, 40, 22, 7, false);
        if (n <= 1) { float p = 0.4f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 220.0); D.round(0, 0, 40, 22, 7, D.alpha(n == 0 ? 0xF87171 : 0xFACC15, (int) (40 * p))); }
        if (lastTotem != null) D.g.item(lastTotem, 3, 3);
        String t = String.valueOf(n); int col = n == 0 ? 0xFFF87171 : n == 1 ? 0xFFFACC15 : 0xFFFFFFFF; float ts = n > 99 ? 0.9f : 1.2f;
        D.g.push(21 + (17 - D.w(t) * ts) / 2, 11 - 4 * ts, ts); D.g.text(t, 0, 0, col, true); D.g.pop();
    }
    private static List<Object> cooldowns(Game game, Object[] inv) {
        List<Object> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        List<Object> all = new ArrayList<>(Arrays.asList(inv)); all.add(game.offhand());
        for (Object st : all) if (st != null && game.itemCooldown(st) > 0.001f && seen.add(game.itemId(st))) out.add(st);
        return out;
    }
    private static void cooldownList(Game game, List<Object> l) {
        D.panel(0, 0, 104, Math.max(1, l.size()) * 20 + 4, 5);
        float y = 2;
        for (Object st : l) { float f = game.itemCooldown(st);
            D.g.item(st, 3, (int) y + 1);
            D.round(24, y + 8, 98, y + 12, 2, 0x30FFFFFF); D.round(24, y + 8, 24 + 74 * f, y + 12, 2, V.a1(), V.a2());
            y += 20; }
        if (l.isEmpty()) D.g.text("Перезарядки", 6, 8, 0xFF9FB3C0, false);
    }
    private static void music() {
        boolean has = Media.present(), on = Media.active(); String t = has ? Media.title : "Ничего не играет", a = has ? (Media.artist.isEmpty() ? (on ? "Играет" : "Пауза") : Media.artist + (on ? "" : "  ·  пауза")) : "Spotify, Яндекс Музыка, браузер…";
        D.panel(0, 0, 156, 36, 7);
        java.io.File cv = Media.coverFile();
        if (cv == null || !D.g.image(Media.cover, cv, 4, 4, 28, 28)) { // no cover: gradient tile with an equalizer
            D.round(4, 4, 32, 32, 6, V.a1(), V.a2());
            float bars = System.currentTimeMillis() / 160f;
            for (int i = 0; i < 4; i++) { float h = on ? 5 + 9 * Math.abs((float) Math.sin(bars + i * 1.3f)) : 4; D.round(9 + i * 5.5f, 25 - h, 12 + i * 5.5f, 25, 1, 0xFFFFFFFF); } }
        String tt = t; while (D.w(tt) > 116 && tt.length() > 3) tt = tt.substring(0, tt.length() - 2); if (!tt.equals(t)) tt += "…";
        D.g.text(tt, 37, 5, 0xFFFFFFFF, V.shadow);
        String aa = a; while (D.w(aa) * 0.8f > 114 && aa.length() > 3) aa = aa.substring(0, aa.length() - 2); if (!aa.equals(a)) aa += "…";
        D.textS(aa, 37, 15, 0.8f, 0xFF9FB3C0);
        double d = Media.dur(), p = Media.pos(); float f = d > 0 ? (float) Math.min(1, p / d) : 0;
        D.round(37, 27, 150, 29, 1, 0x30FFFFFF); D.round(37, 27, 37 + 113 * f, 29, 1, V.a1(), V.a2());
    }

    /** elements that always grow from their middle */
    public static boolean centred(String id) { return id.equals("armor") || id.equals("targethud") || id.equals("island") || id.equals("hotbar"); }
    interface Size { float[] get(); }
    /** draws an element at its saved position; x/y > 0.5 anchor to the right/bottom edge so elements grow away from the edge */
    private static void place(String id, int W, int H, float s, Size size, Runnable draw) {
        V.Mod m = V.mod(id); if (m == null || !(m.on || editing)) return;
        float[] wh = size.get(); float w = wh[0] * s, h = wh[1] * s; if (w <= 0 || h <= 0) return;
        float x = m.hud[0] > 0.5f ? m.hud[0] * W - w : m.hud[0] * W, y = m.hud[1] > 0.5f ? m.hud[1] * H - h : m.hud[1] * H;
        if (centred(id) || Math.abs(m.hud[0] - 0.5f) < 0.002f) x = m.hud[0] * W - w / 2; // centred elements (or put in the middle in the editor)
        if (Math.abs(m.hud[1] - 0.5f) < 0.002f) y = H / 2f - h / 2;
        if (id.equals("hotbar") && m.hud[1] > 0.97f) y = H - h; // sits exactly where the vanilla hotbar was: XP, hearts and food stay free
        if (id.equals("island") && m.hud[1] < 0.15f) y += bossBars * 19; // never on top of boss bars
        x = Math.max(1, Math.min(W - w - 1, x)); y = Math.max(1, Math.min(H - h - 1, y));
        Box b = new Box(m); b.x = x; b.y = y; b.w = w; b.h = h; BOXES.add(b);
        if (!m.on) { D.round(x, y, x + w, y + h, 4, D.alpha(0x808080, 50)); D.textC(m.name, x + w / 2, y + h / 2 - 4, 0xAAFFFFFF); return; }
        D.g.push(x, y, s);
        try { draw.run(); } catch (Throwable ignored) {} finally { D.g.pop(); }
    }

    // ------------------------------------------------------------------ elements
    private static float[] watermarkSize(Game game) {
        int st = V.mode("watermark", "style");
        String extra = extra(game);
        if (st == 2) return new float[]{D.w("Asteria") * 1.5f + 4, 16};
        return new float[]{22 + D.w("Asteria") + (extra.isEmpty() ? 6 : D.w(extra) + 14), 18};
    }
    private static String extra(Game game) {
        List<String> p = new ArrayList<>();
        if (V.bool("watermark", "fps")) p.add(fps + " fps");
        if (V.bool("watermark", "time")) p.add(HM.format(new Date()));
        return String.join("  ·  ", p);
    }
    private static void watermark(Game game) {
        int st = V.mode("watermark", "style"); String extra = extra(game);
        if (st == 2) { D.g.push(0, 0, 1.5f); D.gradText("Asteria", 1, 1, true); D.g.pop(); return; }
        float w = watermarkSize(game)[0];
        if (st == 1) { D.shadow(0, 0, w, 18, 9, 1); D.round(0, 0, w, 18, 9, D.alpha(0x0A1520, (int) (255 * V.bg))); D.round(0, 0, w, 18, 9, D.a(V.a1(), 0.18f), D.a(V.a2(), 0.18f)); }
        else D.panel(0, 0, w, 18, 5, false);
        float spin = (System.currentTimeMillis() % 6000) / 6000f;
        D.mark(1, -1, 18 + (float) Math.sin(spin * Math.PI * 2) * 0.6f);
        float tx = 19 + D.gradText("Asteria", 19, 5, V.shadow);
        if (!extra.isEmpty()) { D.circle(tx + 6, 9, 1.2f, 0x99FFFFFF); D.text(extra, tx + 11, 5, 0xFFD8EEF5); }
    }

    private static List<String[]> infoLines(Game game) {
        List<String[]> r = new ArrayList<>();
        if (V.bool("info", "fps")) r.add(new String[]{"FPS", String.valueOf(fps)});
        if (V.bool("info", "ping")) { int p = game.ping(); if (p >= 0) r.add(new String[]{"Пинг", p + " мс"}); }
        if (V.bool("info", "cps")) r.add(new String[]{"CPS", LMB.size() + " | " + RMB.size()});
        if (V.bool("info", "coords")) r.add(new String[]{"XYZ", (int) Math.floor(game.x()) + " " + (int) Math.floor(game.y()) + " " + (int) Math.floor(game.z()) + " " + facing(game.yaw())});
        if (V.bool("info", "speed")) r.add(new String[]{"Скорость", String.format("%.1f б/с", speed)});
        if (V.bool("info", "clock")) r.add(new String[]{"Время", HM.format(new Date())});
        if (V.bool("info", "server")) { String s = game.server(); r.add(new String[]{"Сервер", s == null || s.isEmpty() ? "одиночная" : s}); }
        return r;
    }
    private static float[] infoSize(Game game) {
        List<String[]> r = infoLines(game); float w = 60;
        for (String[] l : r) w = Math.max(w, D.w(l[0] + ":") + D.w(l[1]) + 26);
        return new float[]{w, r.size() * 11 + 7};
    }
    private static void info(Game game) {
        List<String[]> r = infoLines(game); float w = infoSize(game)[0];
        D.panel(0, 0, w, r.size() * 11 + 7, 5);
        float y = 4;
        for (int i = 0; i < r.size(); i++) { String[] l = r.get(i);
            if (V.hudStyle == 1) D.diamond(8, y + 3.5f, 2.4f, V.grad(i * 0.12f)); else D.sparkle(8, y + 3.5f, 2.2f, V.grad(i * 0.12f));
            D.text(l[0] + ":", 13, y, V.grad(i * 0.12f)); D.text(l[1], w - 6 - D.w(l[1]), y, 0xFFEAF6FA); y += 11; }
    }

    private static float keysH() { return 50 + (V.bool("keystrokes", "mouse") ? 24 : 0) + (V.bool("keystrokes", "space") ? 14 : 0); }
    private static void keystrokes(Game game) {
        int u = 22, gp = 2;
        key("k0", game.keyName(0), u + gp, 0, u, u, game.key(0), null);
        key("k1", game.keyName(1), 0, u + gp, u, u, game.key(1), null);
        key("k2", game.keyName(2), u + gp, u + gp, u, u, game.key(2), null);
        key("k3", game.keyName(3), 2 * (u + gp), u + gp, u, u, game.key(3), null);
        float y = 2 * (u + gp), full = 3 * u + 2 * gp;
        if (V.bool("keystrokes", "mouse")) { float hw = (full - gp) / 2;
            key("k6", "ЛКМ", 0, y, hw, u, game.key(6), LMB.size() + " CPS"); key("k7", "ПКМ", hw + gp, y, full - hw - gp, u, game.key(7), RMB.size() + " CPS"); y += u + gp; }
        if (V.bool("keystrokes", "space")) { float p = D.anim("k4", game.key(4) ? 1 : 0, game.key(4) ? 30 : 9);
            keyBg(0, y, full, 12, p); D.round(full / 2 - 12, y + 5, full / 2 + 12, y + 7, 1, D.mix(0xFFEAF6FA, 0xFF061018, p)); }
    }
    private static void keyBg(float x, float y, float w, float h, float p) {
        if (V.shadow && p > 0.05f) D.round(x - 2, y - 2, x + w + 2, y + h + 2, 6, D.a(V.a1(), 0.25f * p), D.a(V.a2(), 0.25f * p)); // glow while pressed
        D.tile(x, y, x + w, y + h, 4, p);
    }
    private static void key(String id, String label, float x, float y, float w, float h, boolean down, String sub) {
        float p = D.anim(id, down ? 1 : 0, down ? 30 : 9);
        keyBg(x, y, w, h, p);
        int c = D.mix(0xFFEAF6FA, 0xFF061018, p);
        if (sub == null) D.g.text(label, x + (w - D.w(label)) / 2f, y + (h - 8) / 2f, c, p < 0.5f && V.shadow);
        else { D.g.text(label, x + (w - D.w(label)) / 2f, y + 3, c, false); D.g.push(x + (w - D.w(sub) * 0.7f) / 2f, y + 13, 0.7f); D.g.text(sub, 0, 0, D.a(c, 0.8f), false); D.g.pop(); }
    }

    private static float[] armorSize(List<Game.Item> a) {
        boolean row = V.mode("armor", "layout") == 0; int n = Math.max(1, a.size());
        return row ? new float[]{n * 22 + 4, V.bool("armor", "percent") ? 31 : 25} : new float[]{V.bool("armor", "percent") ? 50 : 24, n * 20 + 4};
    }
    private static void armor(List<Game.Item> items) {
        boolean row = V.mode("armor", "layout") == 0, pct = V.bool("armor", "percent");
        float[] wh = armorSize(items); D.panel(0, 0, wh[0], wh[1], 5, false);
        for (int i = 0; i < items.size(); i++) { Game.Item it = items.get(i);
            float x = row ? 3 + i * 22 : 4, y = row ? 3 : 3 + i * 20;
            D.g.item(it.stack, (int) x + 2, (int) y);
            if (it.max > 0) { float left = 1f - it.damage / (float) it.max; int col = dur(left);
                if (row) { D.round(x + 2, y + 18, x + 18, y + 20, 1, 0x60000000); D.round(x + 2, y + 18, x + 2 + 16 * left, y + 20, 1, col); if (pct) { D.g.push(x + 10 - D.w(Math.round(left * 100) + "%") * 0.3f, y + 21.5f, 0.6f); D.g.text(Math.round(left * 100) + "%", 0, 0, col, false); D.g.pop(); } }
                else if (pct) D.text(Math.round(left * 100) + "%", x + 20, y + 4, col); } }
    }

    private static float[] effSize(List<Game.Effect> l) { float w = 92; for (Game.Effect e : l) w = Math.max(w, 30 + Math.max(D.w(effName(e)), D.w(time(e.ticks)))); return new float[]{w, Math.max(1, l.size()) * 24 + 4}; }
    private static final Map<String, Integer> EFFMAX = new HashMap<>();
    private static void effects(List<Game.Effect> l) {
        float[] wh = effSize(l); D.panel(0, 0, wh[0], wh[1], 5);
        float y = 3;
        for (Game.Effect e : l) {
            String k = e.name + e.amp; Integer mx = EFFMAX.get(k); if (mx == null || e.ticks > mx) { mx = e.ticks; EFFMAX.put(k, mx); }
            int col = 0xFF000000 | e.color;
            D.tile(4, y + 2, 22, y + 20, 4, 0); D.round(5, y + 3, 21, y + 19, 3, D.a(col, 0.22f));
            if (!D.g.effectIcon(e.type, 5, (int) y + 3, 16)) D.circle(13, y + 11, 5, col);
            D.text(effName(e), 27, y + 2, 0xFFEAF6FA);
            boolean low = e.ticks < 200 && e.ticks < 32000;
            D.text(time(e.ticks), 27, y + 12, low && (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFF6B6B : 0xFF9FB3C0);
            if (V.bool("effects", "bars")) { float f = e.ticks >= 32000 ? 1 : Math.max(0, Math.min(1, e.ticks / (float) mx)); D.round(27 + D.w(time(e.ticks)) + 5, y + 15, wh[0] - 5, y + 17, 1, 0x40FFFFFF); float bx = 27 + D.w(time(e.ticks)) + 5; D.round(bx, y + 15, bx + (wh[0] - 5 - bx) * f, y + 17, 1, col); }
            y += 24;
        }
    }
    private static String effName(Game.Effect e) { return e.amp > 0 ? e.name + " " + roman(e.amp + 1) : e.name; }

    private static List<Game.Item> targetGear(Game game, Object t) {
        List<Game.Item> l = new ArrayList<>(); if (t == null) return l;
        if (V.bool("targethud", "armor")) l.addAll(game.armorOf(t));
        if (V.bool("targethud", "held")) { Game.Item h = game.heldBy(t); if (h != null) l.add(h); Game.Item o = game.offBy(t); if (o != null) l.add(o); }
        return l;
    }
    private static float[] targetSize(Game game, Object t) {
        List<Game.Item> gear = targetGear(game, t); float h = 38;
        if (!gear.isEmpty()) h += 24;
        return new float[]{Math.max(150, gear.size() * 22 + 10), h};
    }
    private static void target(Game game, Object t) {
        float[] sz = targetSize(game, t); float w = sz[0], h = sz[1];
        D.panel(0, 0, w, h, 6);
        if (t == null) { D.text("Цель", 8, 15, 0xAAFFFFFF); return; }
        boolean head = V.bool("targethud", "head"); float tx = 7;
        if (head) { Object skin = game.skin(t); float hs = 26; D.round(5, 5, 5 + hs + 2, 5 + hs + 2, 7, D.a(V.a1(), 0.9f), D.a(V.a2(), 0.9f));
            if (skin != null) D.g.headRound(skin, 6, 6, hs, 6);
            else { D.round(6, 6, 6 + hs, 6 + hs, 6, 0xFF10202A); boolean drawn = false; try { drawn = D.g.entity(t, 6, 6, hs); } catch (Throwable ignored) {}
                if (!drawn) { Object egg = game.entityIcon(t); if (egg != null) D.g.item(egg, 6 + 5, 6 + 5); else D.icon("dot", 6 + 7, 6 + 7, 12, 0xFF9FB3C0); } }
            tx = 38; }
        float hp = Math.max(0, game.hp(t)), mx = Math.max(1, game.maxHp(t)), f = Math.min(1, hp / mx);
        float shown = D.anim("thp" + System.identityHashCode(t), f, 6);
        String name = game.name(t); float room = w - tx - 40, ns = Math.max(0.6f, Math.min(1f, room / Math.max(1, D.w(name)))); // long nicks get smaller, not cut
        while (D.w(name) * ns > room && name.length() > 3) name = name.substring(0, name.length() - 1);
        D.textS(name, tx, 6 + (1 - ns) * 4, ns, 0xFFFFFFFF);
        String hpS = String.format("%.1f", hp / 2); float hx = w - 6 - D.w(hpS) - 11; D.icon("heart", hx, 5.5f, 9, dur(f)); D.text(hpS, hx + 11, 6, dur(f));
        D.round(tx, 22, w - 7, 28, 3, 0x50000000);
        D.round(tx, 22, tx + Math.max(3, (w - 7 - tx) * shown), 28, 3, D.alpha(0xFFFFFF, 90)); // trailing damage
        D.round(tx, 22, tx + Math.max(3, (w - 7 - tx) * f), 28, 3, V.a1(), V.a2());
        List<Game.Item> gear = targetGear(game, t);
        if (!gear.isEmpty()) { float gx = 6, gy = 36;
            for (Game.Item it : gear) {
                D.round(gx, gy, gx + 20, gy + 20, 4, 0x22FFFFFF);
                if (!it.ench.isEmpty()) D.round(gx, gy, gx + 20, gy + 20, 4, D.a(0xFFB980FF, 0.25f)); // enchanted: violet glow
                D.g.item(it.stack, (int) gx + 2, (int) gy + 2);
                if (V.bool("targethud", "durability") && it.max > 0) { float left = 1f - it.damage / (float) it.max; D.round(gx + 2, gy + 18, gx + 18, gy + 19.5f, 0.7f, 0x60000000); D.round(gx + 2, gy + 18, gx + 2 + 16 * left, gy + 19.5f, 0.7f, dur(left)); }
                gx += 22; } }
    }

    // ------------------------------------------------------------------ server card
    private static String[] srv(Game game) { String[] i = game.serverInfo(); return i != null ? i : new String[]{"Сервер", "play.safimine.ru", "0"}; }
    private static float[] serverSize(Game game) { String[] i = srv(game); String l2 = line2(game, i); return new float[]{Math.max(110, Math.max(D.w(i[0]), D.w(l2) * 0.8f) + 44), 36}; }
    private static String line2(Game game, String[] i) {
        List<String> p = new ArrayList<>(); if (V.bool("servercard", "ip")) p.add(i[1]);
        if (V.bool("servercard", "online")) p.add(i[2] + " онлайн"); int ping = game.ping(); if (ping >= 0) p.add(ping + " ms");
        return String.join("  ·  ", p);
    }
    private static void serverCard(Game game) {
        String[] i = srv(game); float w = serverSize(game)[0];
        D.panel(0, 0, w, 36, 7);
        java.io.File ic = game.serverIcon();
        if (ic == null || !D.g.image("srv:" + i[1], ic, 4, 4, 28, 28)) { D.round(4, 4, 32, 32, 6, V.a1(), V.a2()); D.icon("globe", 10, 10, 16, 0xFFFFFFFF); }
        D.g.text(i[0], 37, 6, 0xFFFFFFFF, V.shadow);
        D.textS(line2(game, i), 37, 19, 0.8f, 0xFF9FB3C0);
    }

    // ------------------------------------------------------------------ info above players: hearts (Neat), armour with durability, hands, enchantments
    private static void nametags(Game game, int W, int H) {
        double range = V.num("nametags", "range"); float pt = Fx.partial(); double[] me = game.pos(game.self(), pt);
        List<Object> ps = new ArrayList<>(game.players());
        for (Object e : ps) {
            if (!game.alive(e)) continue;
            double[] p = game.pos(e, pt); double dx = p[0] - me[0], dy = p[1] - me[1], dz = p[2] - me[2], dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > range) continue;
            float[] sp = Fx.project(p[0], p[1] + game.height(e) + 0.62, p[2], W, H); if (sp == null) continue;
            float k = (float) (V.num("nametags", "scale") * Math.max(0.45, Math.min(1.1, 6 / Math.max(1, sp[2])))), y = sp[1];
            D.g.push(sp[0], y, k); try { tag(game, e); } finally { D.g.pop(); }
        }
    }
    private static void tag(Game game, Object e) {
        float y = 0;
        if (V.bool("nametags", "hearts")) { float hp = Math.max(0, game.hp(e)), mx = Math.max(1, game.maxHp(e)); int n = (int) Math.ceil(Math.min(40, mx) / 2), per = Math.min(10, n), rows = (n + per - 1) / per;
            String v = String.format("%.1f", hp / 2); float w = per * 8 + 4 + D.w(v) * 0.8f + 4; y -= rows * 8 + 4;
            D.round(-w / 2, y - 2, w / 2, y + rows * 8 + 1, 4, D.alpha(0x05090D, 150));
            for (int i = 0; i < n; i++) { float hx = -w / 2 + 3 + (i % per) * 8, hy = y + (i / per) * 8; float f = Math.max(0, Math.min(1, hp / 2 - i));
                D.icon("heart", hx, hy, 7.5f, 0x80303030); if (f > 0) { D.icon("heart", hx, hy, 7.5f, f >= 1 ? 0xFFF43F5E : 0xFFFB7185); } }
            D.textS(v, -w / 2 + per * 8 + 5, y + (rows * 8 - 7) / 2f, 0.8f, dur(hp / mx));
            y -= 2; }
        List<Game.Item> items = new ArrayList<>();
        if (V.bool("nametags", "hands")) { Game.Item h = game.heldBy(e); if (h != null) items.add(h); }
        if (V.bool("nametags", "armor")) items.addAll(game.armorOf(e));
        if (V.bool("nametags", "hands")) { Game.Item o = game.offBy(e); if (o != null) items.add(o); }
        if (items.isEmpty()) return;
        float w = items.size() * 18; y -= 22;
        D.round(-w / 2 - 2, y - 1, w / 2 + 2, y + 20, 4, D.alpha(0x05090D, 150));
        for (int i = 0; i < items.size(); i++) { Game.Item it = items.get(i); float x = -w / 2 + i * 18 + 1;
            D.g.item(it.stack, (int) x, (int) y);
            if (it.max > 0) { float left = 1f - it.damage / (float) it.max; D.round(x + 1, y + 17, x + 15, y + 18.5f, 0.6f, 0x80000000); D.round(x + 1, y + 17, x + 1 + 14 * left, y + 18.5f, 0.6f, dur(left)); } }
        if (!V.bool("nametags", "enchants")) return;
        for (int i = 0; i < items.size(); i++) { Game.Item it = items.get(i); if (it.ench.isEmpty()) continue; float x = -w / 2 + i * 18 + 9;
            float ey = y - 2; for (String en : it.ench) { String sh = shortEnch(en); ey -= 5.5f; D.textS(sh, x - D.w(sh) * 0.25f, ey, 0.5f, 0xFFD8B4FE); } }
    }
    /** "Защита IV" -> "Защ4": short enough to stack above an item */
    private static String shortEnch(String e) {
        String[] p = e.trim().split(" "); String lvl = p.length > 1 ? p[p.length - 1] : "";
        int n = 0; for (int i = 0; i < ROMAN.length; i++) if (ROMAN[i].equals(lvl)) n = i;
        String name = n > 0 ? e.substring(0, e.length() - lvl.length()).trim() : e.trim(); name = name.length() > 3 ? name.substring(0, 3) : name;
        return name + (n > 0 ? n : "");
    }
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private static void marks(Game game, int W, int H) {
        Fx.marksServer(game); double[] me = game.pos(game.self(), Fx.partial());
        for (int k = 0; k < Fx.MARKS.size(); k++) { double[] m = Fx.MARKS.get(k); float[] sp = Fx.project(m[0], m[1] + 0.6, m[2], W, H);
            if (sp == null) continue; float x = Math.max(12, Math.min(W - 12, sp[0])), y = Math.max(12, Math.min(H - 22, sp[1]));
            double dx = m[0] - me[0], dy = m[1] - me[1], dz = m[2] - me[2]; int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            float near = dist < 6 ? 0.45f : 1; // close up it fades so it doesn't cover the block
            D.circle(x, y, 9, D.a(0xFF05090D, 0.55f * near)); D.ring(x, y, 8, 9.2f, D.a(V.grad(k * 0.17f), near), D.a(V.grad(k * 0.17f + 0.3f), near));
            D.mark(x - 8, y - 8, 16);
            if (V.bool("markers", "dist")) { String t = dist + " м"; float tw = D.w(t) * 0.8f; D.round(x - tw / 2 - 3, y + 11, x + tw / 2 + 3, y + 20, 3, D.a(0xFF05090D, 0.6f * near)); D.textS(t, x - tw / 2, y + 12, 0.8f, D.a(0xFFFFFFFF, near)); } }
    }
    /** a module switched by its key: sound + notification */
    public static void bindToggled(V.Mod m) { notify(m.name, m.on ? "Включено" : "Выключено", m.on); Fx.sound(m.on ? "blip" : "pop", 0.55f); }

    // ------------------------------------------------------------------ array list
    private static final Map<String, Float> SLIDE = new HashMap<>();
    private static List<V.Mod> listed() {
        List<V.Mod> l = new ArrayList<>();
        for (V.Mod m : V.MODS) if (!m.cat.equals("hud") && (m.on || SLIDE.getOrDefault(m.id, 0f) > 0.02f)) l.add(m);
        l.sort((a, b) -> D.w(b.name) - D.w(a.name)); return l;
    }
    private static float[] listSize() { float w = 40; List<V.Mod> l = listed(); for (V.Mod m : l) w = Math.max(w, D.w(m.name) + 12); return new float[]{w, Math.max(1, l.size()) * 11 + 2}; }
    private static void arraylist() {
        List<V.Mod> l = listed(); float w = listSize()[0]; boolean right = V.mod("arraylist").hud[0] > 0.5f;
        float y = 0; int i = 0;
        for (V.Mod m : l) {
            float p = D.ease(D.anim("al" + m.id, m.on ? 1 : 0, 8)); SLIDE.put(m.id, p);
            float tw = D.w(m.name) + 8, x = right ? w - tw * p : 0 - tw * (1 - p);
            int c = V.grad(i * 0.08f);
            if (V.bool("arraylist", "bg")) D.rect(x, y, x + tw, y + 11, D.alpha(0x0A1520, (int) (200 * V.bg * p)));
            if (V.bool("arraylist", "line")) D.rect(right ? x + tw - 1.5f : x, y, right ? x + tw : x + 1.5f, y + 11, D.a(c, p));
            D.g.text(m.name, x + (right ? 3 : 5), y + 2, D.a(c, Math.max(0.05f, p)), V.shadow);
            y += 11 * p; i++;
        }
    }

    // ------------------------------------------------------------------ notifications
    private static final class Note { String title, text; boolean on; long at; }
    private static final List<Note> NOTES = new ArrayList<>();
    public static void notify(String title, String text, boolean on) {
        if (V.mod("notifications") == null || !V.mod("notifications").on) return;
        Note n = new Note(); n.title = title; n.text = text; n.on = on; n.at = System.currentTimeMillis(); NOTES.add(0, n); if (NOTES.size() > 5) NOTES.remove(NOTES.size() - 1);
    }
    private static float[] notesSize() { return new float[]{140, editing && NOTES.isEmpty() ? 30 : Math.max(1, NOTES.size()) * 34}; }
    private static void notes() {
        long now = System.currentTimeMillis(); NOTES.removeIf(n -> now - n.at > 4600);
        float y = 0;
        for (Note n : NOTES) {
            float age = (now - n.at) / 1000f, in = D.ease(Math.min(1, age * 4)), out = age > 4.2f ? D.ease((age - 4.2f) * 2.5f) : 0;
            float x = (1 - in + out) * 150;
            D.g.push(x, y, 1);
            D.panel(0, 0, 140, 30, 6, false);
            D.round(6, 7, 22, 23, 8, n.on ? 0xFF22C55E : 0xFFEF4444); D.textC(n.on ? "✔" : "✖", 14, 11, 0xFFFFFFFF);
            D.text(n.title, 28, 6, 0xFFFFFFFF); D.text(n.text, 28, 17, 0xFF9FB3C0);
            D.round(4, 27, 4 + 132 * Math.max(0, 1 - age / 4.4f), 28.5f, 0.7f, V.a1(), V.a2());
            D.g.pop();
            y += 34 * (1 - out);
        }
    }

    // ------------------------------------------------------------------ crosshair
    private static void xrect(float x0, float y0, float x1, float y1, int c, int o, float ot) { if (ot > 0) D.rect(x0 - ot, y0 - ot, x1 + ot, y1 + ot, o); D.rect(x0, y0, x1, y1, c); }
    private static void crosshair(int W, int H, Game game) {
        int st = V.mode("crosshair", "style"); if (st == 7) return; // vanilla: the game draws its own
        if (V.bool("crosshair", "hidef5") && game.thirdPerson()) return;
        float cx = W / 2f, cy = H / 2f, al = (float) V.num("crosshair", "alpha"); int c = D.a(V.color("crosshair", "color"), al), o = D.a(0xFF000000, al * 0.85f);
        float size = (float) V.num("crosshair", "size"), gap = (float) V.num("crosshair", "gap"), th = (float) V.num("crosshair", "thick"), ot = V.bool("crosshair", "outline") ? (float) V.num("crosshair", "othick") : 0;
        if (V.bool("crosshair", "dynamic")) gap += (1 - game.cooldown()) * 4;
        if (st == 6) { String p = V.text("crosshair", "pattern"); int n = (int) Math.round(Math.sqrt(p.length())); if (n < 3 || n * n != p.length()) return;
            float cs = th, x0 = cx - n * cs / 2, y0 = cy - n * cs / 2;
            for (int pass = ot > 0 ? 0 : 1; pass < 2; pass++) for (int i = 0; i < n * n; i++) if (p.charAt(i) == '1') { float x = x0 + (i % n) * cs, y = y0 + (i / n) * cs;
                if (pass == 0) D.rect(x - ot, y - ot, x + cs + ot, y + cs + ot, o); else D.rect(x, y, x + cs, y + cs, c); }
            return; }
        if (st == 0 || st == 1 || st == 5) for (float[] d : new float[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { if (st == 5 && d[1] < 0) continue;
            float x1 = cx + d[0] * gap, y1 = cy + d[1] * gap, x2 = cx + d[0] * (gap + size), y2 = cy + d[1] * (gap + size), hx = d[0] == 0 ? th / 2 : 0, hy = d[1] == 0 ? th / 2 : 0;
            xrect(Math.min(x1, x2) - hx, Math.min(y1, y2) - hy, Math.max(x1, x2) + hx, Math.max(y1, y2) + hy, c, o, ot); }
        if (st == 3 || st == 4) { float r = size + gap; if (ot > 0) D.ring(cx, cy, r - th / 2 - ot, r + th / 2 + ot, o, o); D.ring(cx, cy, r - th / 2, r + th / 2, c, c); }
        if (st == 1 || st == 2 || st == 4) { float r = Math.max(0.6f, th * 0.75f) * (st == 2 ? 1.4f : 1); if (ot > 0) D.circle(cx, cy, r + ot, o); D.circle(cx, cy, r, c); }
    }

    /** Asteria frame: thin stepped lines in the corners and a star with lines at the top centre (asset board HUD) */
    /** star charts drawn in the corners: node positions (x, y) from the corner, and which nodes are joined */
    private static final float[][] STARS = {{8, 7}, {34, 12}, {58, 6}, {80, 15}, {46, 27}, {16, 34}, {10, 58}};
    private static final int[][] LINKS = {{0, 1}, {1, 2}, {2, 3}, {1, 4}, {4, 5}, {5, 6}};
    private static void frame(int W, int H) {
        boolean corners = V.bool("frame", "corners"), top = V.bool("frame", "top");
        long now = System.currentTimeMillis();
        if (corners) for (int sx = 0; sx < 2; sx++) for (int sy = 0; sy < 2; sy++) {
            float ox = sx == 0 ? 0 : W, oy = sy == 0 ? 0 : H, d = sx == 0 ? 1 : -1, e = sy == 0 ? 1 : -1; int seed = sx * 2 + sy;
            for (int[] l : LINKS) { float[] a = STARS[l[0]], b = STARS[l[1]];
                D.line(ox + d * a[0], oy + e * a[1], ox + d * b[0], oy + e * b[1], 0.6f, D.a(V.grad((l[0] + seed) / 7f), 0.45f)); }
            for (int i = 0; i < STARS.length; i++) { float tw = (float) (0.55 + 0.45 * Math.sin(now / (380.0 + i * 70) + i * 1.7 + seed));
                float px = ox + d * STARS[i][0], py = oy + e * STARS[i][1];
                if (i % 3 == 0) D.sparkle(px, py, 2.4f + tw, D.a(0xFFFFFFFF, 0.5f + 0.5f * tw)); else D.circle(px, py, 1f + 0.4f * tw, D.a(V.grad(i / 7f), 0.6f + 0.4f * tw)); }
        }
        if (top) { float cx = W / 2f, y = 10; int c = D.a(V.a2(), 0.85f);
            D.hgrad(cx - 130, y - 0.35f, cx - 14, y + 0.35f, D.a(V.a1(), 0f), c); D.hgrad(cx + 14, y - 0.35f, cx + 130, y + 0.35f, c, D.a(V.a1(), 0f));
            for (int s = -1; s <= 1; s += 2) { D.sparkle(cx + s * 52, y, 1.8f, D.a(0xFFFFFFFF, 0.8f)); D.circle(cx + s * 92, y, 0.9f, D.a(V.a1(), 0.8f)); }
            double a = now / 900.0; for (int k = 0; k < 2; k++) { double an = a + k * Math.PI; D.circle(cx + (float) Math.cos(an) * 11, y + (float) Math.sin(an) * 4.5f, 1f, D.a(V.grad(k * 0.5f), 0.9f)); }
            D.mark(cx - 11, y - 11, 22); }
    }

    private static void locked(Game game) {
        String a = "Asteria Visuals", b = "Нужна подписка PREMIUM — визуалы выключены";
        float w = Math.max(D.w(a), D.w(b)) + 30, x = (game.width() - w) / 2;
        D.g.push(x, 8, 1); D.panel(0, 0, w, 28, 6, false); D.mark(4, 5, 18);
        D.gradText(a, 24, 5, true); D.text(b, 24, 16, 0xFFEAF6FA); D.g.pop();
    }

    // ------------------------------------------------------------------ util
    static int dur(float f) { return f > 0.6f ? 0xFF4ADE80 : f > 0.3f ? 0xFFFACC15 : 0xFFF87171; }
    private static String time(int ticks) { if (ticks >= 32000) return "∞"; int s = ticks / 20; return (s / 60) + ":" + String.format("%02d", s % 60); }
    private static String facing(float yaw) { String[] d = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"}; float y = ((yaw % 360) + 360) % 360; return d[Math.round(y / 45f) % 8]; }
    private static String roman(int n) { String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}; return n > 0 && n < r.length ? r[n] : String.valueOf(n); }
}
