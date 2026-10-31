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
        if (game.screenOpen() && !game.chatOpen() && !editing && !Gui.open) return; // the Asteria menu shows the HUD behind it: changes are seen at once
        // clicks per second
        boolean lmb = game.key(6) && !game.screenOpen(), rmb = game.key(7) && !game.screenOpen();
        if (lmb && !lmbWas) LMB.add(now); if (rmb && !rmbWas) RMB.add(now); lmbWas = lmb; rmbWas = rmb;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.pollFirst();
        if (now - speedAt >= 250) { double dx = game.x() - lx, dz = game.z() - lz; double inst = speedAt == 0 ? 0 : Math.sqrt(dx * dx + dz * dz) / ((now - speedAt) / 1000.0); speed = inst > 100 ? speed : speed * 0.5 + inst * 0.5; lx = game.x(); lz = game.z(); speedAt = now; }

        if (now - notifyAt > 500) { notifyAt = now; friendNote(); }
        int W = game.width(), H = game.height(); bossBars = game.bossBars();
        if (V.on("frame") && !game.screenOpen()) frame(W, H);
        if (V.on("crosshair") && !game.screenOpen()) { float turn = (float) V.num("crosshair", "rotate"); // every style turns, the own picture too
            if (turn != 0) { D.g.pushRot(W / 2f, H / 2f, turn); try { crosshair(W, H, game); } finally { D.g.pop(); } } else crosshair(W, H, game); }
        if (V.on("dash") && !game.thirdPerson()) { float t = (now - Fx.dashAt) / 520f; if (t >= 0 && t < 1) { float a = (1 - t) * 0.55f; java.util.Random rn = new java.util.Random(Fx.dashAt);
            for (int i = 0; i < 26; i++) { double an = rn.nextDouble() * Math.PI * 2; float r0 = (float) (Math.min(W, H) * (0.36 + rn.nextDouble() * 0.1) + t * 40), len = 30 + rn.nextInt(50);
                float cx = W / 2f, cy = H / 2f, cs = (float) Math.cos(an), sn = (float) Math.sin(an); D.line(cx + cs * r0, cy + sn * r0, cx + cs * (r0 + len), cy + sn * (r0 + len), 1.2f, D.a(V.grad(i / 26f), a)); } } }
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
        if (inv.length > 0 && !game.spectator()) { // a spectator sees the game's own bar: ours would lie over it
            place("hotbar", W, H, s, () -> hotbarSize(game), () -> hotbar(game, inv));
            place("inventory", W, H, s, () -> new float[]{9 * 19 + 7, 3 * 19 + 19 + (V.bool("inventory", "hotbar") ? 23 : 0)}, () -> inventory(game, inv));
            place("totems", W, H, s, () -> new float[]{28 + D.w("×" + totemCount(game, inv)) * 1.1f + 6, 22}, () -> totems(game, inv));
            List<Object> cd = cooldowns(game, inv);
            if (!cd.isEmpty() || editing) place("cooldowns", W, H, s, () -> new float[]{104, Math.max(1, cd.size()) * 20 + 4}, () -> cooldownList(game, cd));
        }
        if (Media.present() || editing) place("music", W, H, s, () -> new float[]{170, 42}, Hud::music);
        if (game.serverInfo() != null || editing) place("servercard", W, H, s, () -> serverSize(game), () -> serverCard(game));
        if (V.on("nametags") && !editing) try { nametags(game, W, H); } catch (Throwable ignored) {}
        if (V.on("markers") && !editing) try { marks(game, W, H); } catch (Throwable ignored) {}
        if (!Fx.DMG.isEmpty() && !editing) try { damageNumbers(W, H); } catch (Throwable ignored) {}
        if (alertAt > 0) try { alertBanner(W, H); } catch (Throwable ignored) {}
        if (Fx.totemAt > 0) try { totemPop(game, W, H); } catch (Throwable ignored) {}
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
    private static float[] islandSize(Game game) { return new float[]{D.anim("island.w", Math.min(280, D.w(islandText(game)) + 36), 10), 18}; }
    private static void island(Game game) {
        String t = islandText(game); float w = islandSize(game)[0];
        D.shadow(0, 0, w, 18, 9, 1.2f);
        int base = D.alpha(0x05090D, (int) (255 * Math.max(0.55f, V.bg)));
        D.round(0, 0, w, 18, 9, D.mix(base, D.alpha(V.a1() & 0xFFFFFF, 255), 0.22f), D.mix(base, D.alpha(V.a2() & 0xFFFFFF, 255), 0.22f)); // theme-tinted glass
        D.mark(4, 1, 16);
        String shown = t; while (D.w(shown) > w - 30 && shown.length() > 3) shown = shown.substring(0, shown.length() - 2);
        if (!shown.equals(t)) shown = shown + "…";
        D.g.text(shown, 20 + (w - 24 - D.w(shown)) / 2, 5, 0xFFFFFFFF, V.shadow);
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
        boolean hb = V.bool("inventory", "hotbar"); D.panel(0, 0, 9 * 19 + 7, 3 * 19 + 19 + (hb ? 23 : 0), 6);
        D.gradText("Инвентарь", 5, 4, true);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) { float x = 4 + c * 19, y = 15 + r * 19;
            D.round(x, y, x + 18, y + 18, 3.5f, 0x22FFFFFF); D.round(x + 0.5f, y + 0.5f, x + 17.5f, y + 1.5f, 1, 0x14FFFFFF); slot(game, inv[9 + r * 9 + c], x + 1, y + 1, 16); }
        if (hb) { float y = 15 + 3 * 19 + 4; D.round(4, y - 3, 9 * 19 + 3, y - 2.4f, 0.3f, D.a(V.a1(), 0.5f), D.a(V.a2(), 0.5f)); // a thin line between the inventory and the hotbar
            for (int c = 0; c < 9; c++) { float x = 4 + c * 19; boolean sel = game.selectedSlot() == c;
                D.round(x, y, x + 18, y + 18, 3.5f, sel ? D.a(V.a1(), 0.55f) : 0x2AFFFFFF, sel ? D.a(V.a2(), 0.55f) : 0x2AFFFFFF); slot(game, inv[c], x + 1, y + 1, 16); } }
    }
    private static Object lastTotem;
    private static int totemCount(Game game, Object[] inv) { int n = 0; for (Object st : inv) if (st != null && game.itemId(st).endsWith("totem_of_undying")) n += game.count(st); Object off = game.offhand(); if (off != null && game.itemId(off).endsWith("totem_of_undying")) n += game.count(off); return n; }
    private static void totems(Game game, Object[] inv) {
        int n = 0; Object one = null;
        for (Object st : inv) if (st != null && game.itemId(st).endsWith("totem_of_undying")) { n += game.count(st); one = st; }
        Object off = game.offhand(); if (off != null && game.itemId(off).endsWith("totem_of_undying")) { n += game.count(off); one = off; }
        if (lastTotem == null) lastTotem = game.totemStack();
        // a tinted tile with the totem, the count beside it; one or none left: the panel breathes yellow / red
        int warn = n == 0 ? 0xFFF87171 : n == 1 ? 0xFFFACC15 : 0; String t = "×" + n; float w = 28 + D.w(t) * 1.1f + 6;
        D.panel(0, 0, w, 22, 7, false);
        if (warn != 0) { float p = 0.35f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 220.0); D.round(0, 0, w, 22, 7, D.a(warn, 0.18f * p + 0.05f)); }
        D.round(3, 2, 21, 20, 5, D.a(warn != 0 ? warn : V.a1(), 0.16f));
        if (lastTotem != null) D.g.item(lastTotem, 4, 3);
        D.g.push(25, 6.5f, 1.1f); D.g.text(t, 0, 0, warn != 0 ? warn : 0xFFFFFFFF, true); D.g.pop();
    }
    private static List<Object> cooldowns(Game game, Object[] inv) {
        List<Object> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        List<Object> all = new ArrayList<>(Arrays.asList(inv)); all.add(game.offhand());
        for (Object st : all) if (st != null && game.itemCooldown(st) > 0.001f && seen.add(game.itemId(st))) out.add(st);
        return out;
    }
    private static final Map<String, float[]> CD = new HashMap<>(); // item -> last progress, time, speed
    private static void cooldownList(Game game, List<Object> l) {
        D.panel(0, 0, 104, Math.max(1, l.size()) * 20 + 4, 5);
        float y = 2;
        long now = System.currentTimeMillis();
        for (Object st : l) { float f = game.itemCooldown(st); String id = game.itemId(st); float[] r = CD.get(id);
            if (r == null || f > r[0] + 0.02f) r = new float[]{f, now % 100000, 0}; else if (f < r[0]) { float dt = ((now % 100000) - r[1]) / 1000f; if (dt > 0.05f) { float rate = (r[0] - f) / dt; r[2] = r[2] == 0 ? rate : r[2] * 0.7f + rate * 0.3f; r[0] = f; r[1] = now % 100000; } }
            CD.put(id, r); String left = r[2] > 0 ? String.format(java.util.Locale.ROOT, "%.1fс", f / r[2]) : "";
            D.g.item(st, 3, (int) y + 1);
            D.round(24, y + 8, 98 - (left.isEmpty() ? 0 : 24), y + 12, 2, 0x30FFFFFF); D.round(24, y + 8, 24 + (74 - (left.isEmpty() ? 0 : 24)) * f, y + 12, 2, V.a1(), V.a2());
            if (!left.isEmpty()) D.textS(left, 78, y + 6.5f, 0.8f, 0xFFFFFFFF);
            y += 20; }
        if (l.isEmpty()) D.g.text("Перезарядки", 6, 8, 0xFF9FB3C0, false);
    }
    private static void music() {
        boolean has = Media.present(), on = Media.active(); String t = has ? Media.title : "Ничего не играет", a = has ? (Media.artist.isEmpty() ? (on ? "Играет" : "Пауза") : Media.artist + (on ? "" : "  ·  пауза")) : "Spotify, Яндекс Музыка, браузер…";
        D.panel(0, 0, 170, 42, 7);
        java.io.File cv = Media.coverFile();
        if (cv == null || !D.g.image(Media.cover, cv, 4, 4, 34, 34)) { // no cover: gradient tile with an equalizer
            D.round(4, 4, 38, 38, 6, V.a1(), V.a2());
            float bars = System.currentTimeMillis() / 160f;
            for (int i = 0; i < 4; i++) { float h = on ? 6 + 11 * Math.abs((float) Math.sin(bars + i * 1.3f)) : 4; D.round(10 + i * 6.5f, 30 - h, 14 + i * 6.5f, 30, 1, 0xFFFFFFFF); } }
        String tt = t; while (D.w(tt) > 124 && tt.length() > 3) tt = tt.substring(0, tt.length() - 2); if (!tt.equals(t)) tt += "…";
        D.g.text(tt, 43, 5, 0xFFFFFFFF, V.shadow);
        String aa = a; while (D.w(aa) * 0.8f > 122 && aa.length() > 3) aa = aa.substring(0, aa.length() - 2); if (!aa.equals(a)) aa += "…";
        D.textS(aa, 43, 15.5f, 0.8f, 0xFFDCE6EE); // the artist: bright enough to read on any panel
        double d = Media.dur(), p = Media.pos(); float f = d > 0 ? (float) Math.min(1, p / d) : 0;
        timeBar(43, 27.5f, 165, d > 0 || p > 0 ? Media.time(p) : "", d > 0 ? Media.time(d) : "", f);
    }
    /** elapsed  ━━━●───  total: the times in full-size bright text on both sides of a 3 px bar */
    static void timeBar(float x0, float y, float x1, String el, String tot, float f) {
        float ew = el.isEmpty() ? 0 : D.w(el) + 5, tw = tot.isEmpty() ? 0 : D.w(tot) + 5, bx0 = x0 + ew, bx1 = x1 - tw, by = y + 3;
        if (!el.isEmpty()) D.g.text(el, x0, y, 0xFFFFFFFF, true);
        if (!tot.isEmpty()) D.g.text(tot, x1 - D.w(tot), y, 0xFFD2DEE8, true);
        D.round(bx0, by, bx1, by + 3, 1.5f, 0x38FFFFFF); float kx = bx0 + (bx1 - bx0) * f;
        if (f > 0) D.round(bx0, by, kx, by + 3, 1.5f, V.a1(), V.a2());
        D.circle(kx, by + 1.5f, 2.6f, 0xFFFFFFFF);
    }

    /** elements that always grow from their middle */
    public static boolean centred(String id) { return id.equals("armor") || id.equals("targethud") || id.equals("island") || id.equals("hotbar"); }
    interface Size { float[] get(); }
    /** draws an element at its saved position; x/y > 0.5 anchor to the right/bottom edge so elements grow away from the edge */
    private static void place(String id, int W, int H, float s, Size size, Runnable draw) {
        V.Mod m = V.mod(id); if (m == null || !(m.on || editing)) return;
        s *= m.hs; float[] wh = size.get(); float w = wh[0] * s, h = wh[1] * s; if (w <= 0 || h <= 0) return;
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
            D.circle(8, y + 3.5f, 1.6f, V.grad(i * 0.12f));
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
        boolean row = V.mode("armor", "layout") == 0; int n = a.isEmpty() ? 4 : a.size(); // the HUD editor shows the full set, in the chosen row / column
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
            boolean low = e.ticks >= 0 && e.ticks < 200;
            D.text(time(e.ticks), 27, y + 12, low && (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFF6B6B : 0xFF9FB3C0);
            if (V.bool("effects", "bars")) { float f = e.ticks < 0 || e.ticks >= 32000 ? 1 : Math.max(0, Math.min(1, e.ticks / (float) mx)); D.round(27 + D.w(time(e.ticks)) + 5, y + 15, wh[0] - 5, y + 17, 1, 0x40FFFFFF); float bx = 27 + D.w(time(e.ticks)) + 5; D.round(bx, y + 15, bx + (wh[0] - 5 - bx) * f, y + 17, 1, col); }
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
        float nw = t == null ? 0 : D.w(targetName(game, t)) + (V.bool("targethud", "head") ? 38 : 7) + 46; // the panel grows for a long nick instead of shrinking it
        return new float[]{Math.max(Math.max(150, Math.min(240, nw)), gear.size() * 22 + 10), h};
    }
    /** a player's own nick (server prefixes and ranks made it tiny), the shown name for mobs */
    private static String targetName(Game game, Object t) { try { if (game.players().contains(t)) { String n = game.playerName(t); if (n != null && !n.isEmpty()) return n; } } catch (Throwable ignored) {} return game.name(t); }
    private static void target(Game game, Object t) {
        float[] sz = targetSize(game, t); float w = sz[0], h = sz[1];
        D.panel(0, 0, w, h, 6);
        if (t == null) { D.text("Цель", 8, 15, 0xAAFFFFFF); return; }
        boolean head = V.bool("targethud", "head"); float tx = 7;
        if (head) { Object skin = game.skin(t); float hs = 26; D.round(5.4f, 5.4f, 5 + hs + 1.6f, 5 + hs + 1.6f, 6.6f, D.a(V.a1(), 0.6f), D.a(V.a2(), 0.6f));
            if (skin != null) D.g.headRound(skin, 6, 6, hs, 6);
            else { D.round(6, 6, 6 + hs, 6 + hs, 6, 0xFF10202A); boolean drawn = false; try { drawn = D.g.entity(t, 6, 6, hs); } catch (Throwable ignored) {}
                if (!drawn) { Object egg = game.entityIcon(t); if (egg != null) D.g.item(egg, 6 + 5, 6 + 5); else D.icon("dot", 6 + 7, 6 + 7, 12, 0xFF9FB3C0); } }
            tx = 38; }
        float hp = Math.max(0, game.hp(t)), mx = Math.max(1, game.maxHp(t)), f = Math.min(1, hp / mx);
        float shown = D.anim("thp" + System.identityHashCode(t), f, 6);
        String name = targetName(game, t); float room = w - tx - 40, ns = Math.max(0.8f, Math.min(1f, room / Math.max(1, D.w(name))));
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
            float k = (float) (V.num("nametags", "scale") * Math.max(0.7, Math.min(1.35, 8 / Math.max(1, sp[2]))) * (1 - 0.45 * Fx.zoomT)), y = sp[1]; // smaller while zooming: the player stays visible
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
            float ey = y - 2; for (String en : it.ench) { String sh = shortEnch(en); float ts = Math.min(0.6f, 15f / Math.max(1, D.w(sh))), tw = D.w(sh) * ts; ey -= 7f; // each column fits its 18 px slot: neighbours never overlap
                D.round(x - 8.5f, ey - 0.5f, x + 8.5f, ey + 6f, 2, 0xB0050A10); D.textS(sh, x - tw / 2, ey + (6 - 8 * ts) / 2 + 0.3f, ts, 0xFFE9D5FF); } }
    }
    /** enchantment abbreviations (the launcher shows the same table: Visuals -> Abbreviations) */
    public static final String[][] ABBR = {{"Защита от снарядов", "ЗСн"}, {"Защита от огня", "ЗОг"}, {"Защита от взрывов", "ЗВз"}, {"Взрывоустойчивость", "ЗВз"}, {"Огнеупорность", "ЗОг"}, {"Невесомость", "Нев"}, {"Защита", "Защ"},
        {"Подводное дыхание", "Дых"}, {"Родство с водой", "Вод"}, {"Подводник", "Вод"}, {"Шипы", "Шип"}, {"Подводная ходьба", "ПХ"}, {"Ледоход", "Лёд"}, {"Скорость душ", "Душ"}, {"Проворство", "Пров"}, {"Быстрые подкрадывания", "Пров"},
        {"Острота", "Остр"}, {"Небесная кара", "Кара"}, {"Бич членистоногих", "Бич"}, {"Отдача", "Отд"}, {"Заговор огня", "Огн"}, {"Добыча", "Доб"}, {"Разящий клинок", "Клин"},
        {"Эффективность", "Эфф"}, {"Шёлковое касание", "Шёлк"}, {"Прочность", "Проч"}, {"Удача", "Удч"}, {"Сила", "Сила"}, {"Откидывание", "Отк"}, {"Воспламенение", "Пыл"}, {"Горящая стрела", "Пыл"}, {"Бесконечность", "Беск"},
        {"Морская удача", "МУд"}, {"Приманка", "Прим"}, {"Верность", "Верн"}, {"Пронзатель", "Пронз"}, {"Тягун", "Тяг"}, {"Громовержец", "Гром"}, {"Многоразовый выстрел", "Мульт"}, {"Тройной выстрел", "Мульт"},
        {"Быстрая перезарядка", "БП"}, {"Пронзающая стрела", "Скв"}, {"Починка", "Почин"}, {"Проклятие несъёмности", "ПрН"}, {"Проклятие утраты", "ПрУ"}, {"Плотность", "Плот"}, {"Пробитие", "Проб"}, {"Порыв ветра", "Вет"}};
    /** "Защита IV" -> "Защ4" (unknown ones: first letters) */
    private static String shortEnch(String e) {
        String[] p = e.trim().split(" "); String lvl = p.length > 1 ? p[p.length - 1] : "";
        int n = 0; for (int i = 0; i < ROMAN.length; i++) if (ROMAN[i].equals(lvl)) n = i;
        String name = n > 0 ? e.substring(0, e.length() - lvl.length()).trim() : e.trim(), ab = null;
        for (String[] a : ABBR) if (name.equalsIgnoreCase(a[0])) { ab = a[1]; break; }
        if (ab == null) ab = name.length() > 4 ? name.substring(0, 4) : name;
        return ab + (n > 0 ? n : "");
    }
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    /** floating damage numbers: our font, size / colour / outline from the settings */
    private static void damageNumbers(int W, int H) {
        long now = System.currentTimeMillis(); double size = V.num("damagenumbers", "size"); int mode = V.mode("damagenumbers", "color"); boolean ol = V.bool("damagenumbers", "outline");
        for (double[] q : new ArrayList<>(Fx.DMG)) { float t = (now - (long) q[3]) / 1200f; if (t >= 1) continue;
            float[] sp = Fx.project(q[0], q[1] + t * 0.7, q[2], W, H); if (sp == null) continue;
            float a = t < 0.7f ? 1 : (1 - t) / 0.3f, pop = t < 0.12f ? 0.6f + t / 0.12f * 0.55f : t < 0.22f ? 1.15f - (t - 0.12f) * 1.5f : 1f;
            float k = (float) (size * 1.4 * pop * Math.max(0.6, Math.min(1.6, 7 / Math.max(1, sp[2]))));
            String s = q[4] >= 10 || !V.bool("damagenumbers", "decimals") ? String.valueOf(Math.round(q[4])) : String.format(java.util.Locale.ROOT, "%.1f", q[4]);
            int col = mode == 1 ? V.grad((float) q[5]) : mode == 2 ? 0xFFFFFFFF : q[4] >= 6 ? 0xFFFF4D6A : q[4] >= 3 ? 0xFFFFB547 : 0xFFFFF1A8;
            D.g.push(sp[0] - D.w(s) * k / 2, sp[1] - 4 * k, k);
            if (ol) for (float[] o : new float[][]{{-0.7f, 0}, {0.7f, 0}, {0, -0.7f}, {0, 0.7f}}) D.g.text(s, o[0], o[1], D.a(0xFF000000, a * 0.85f), false);
            D.g.text(s, 0, 0, D.a(col, a), !ol); D.g.pop(); }
    }
    private static void marks(Game game, int W, int H) {
        Fx.marksServer(game); double[] me = game.pos(game.self(), Fx.partial());
        for (int k = 0; k < Fx.MARKS.size(); k++) { Fx.Mark m = Fx.MARKS.get(k); float[] sp = Fx.project(m.x, m.y + 0.6, m.z, W, H);
            if (sp == null) continue; float x = Math.max(14, Math.min(W - 14, sp[0])), y = Math.max(14, Math.min(H - 30, sp[1]));
            double dx = m.x - me[0], dy = m.y - me[1], dz = m.z - me[2]; int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            float near = dist < 6 ? 0.45f : 1; int c = m.color != 0 ? m.color : V.grad(k * 0.17f); // close up it fades so it doesn't cover the block
            // a pin: coloured drop with the AL mark, the name and the distance under it
            D.circle(x, y + 1, 11, D.a(0xFF000000, 0.25f * near)); D.circle(x, y, 10, D.a(c, 0.95f * near)); D.circle(x, y, 8.3f, D.a(0xFF05090D, 0.85f * near));
            D.g.push(x - 7, y - 7, 1); D.mark(0, 0, 14); D.g.pop();
            D.g.poly(new float[]{x - 4, y + 8.5f, x + 4, y + 8.5f, x, y + 14}, new int[]{D.a(c, near), D.a(c, near), D.a(c, near)});
            String label = (m.name.isEmpty() ? "" : m.name + "  ·  ") + (V.bool("markers", "dist") ? dist + " м" : ""); if (label.endsWith("  ·  ")) label = label.substring(0, label.length() - 5);
            if (!label.isEmpty()) { float tw = D.w(label) * 0.8f; D.round(x - tw / 2 - 4, y + 16, x + tw / 2 + 4, y + 26, 4, D.a(0xFF05090D, 0.7f * near)); D.round(x - tw / 2 - 4, y + 16, x - tw / 2 - 2, y + 26, 1, D.a(c, near)); D.textS(label, x - tw / 2, y + 17.5f, 0.8f, D.a(0xFFFFFFFF, near)); } }
    }
    private static void totemPop(Game game, int W, int H) {
        float t = (System.currentTimeMillis() - Fx.totemAt) / 1300f; if (t >= 1) { Fx.totemAt = 0; return; }
        Object tot = game.totemStack(); float cx = W / 2f, cy = H * 0.42f, a = t < 0.75f ? 1 : (1 - t) / 0.25f, pop = D.ease(Math.min(1, t * 3.2f)) * (1 + 0.15f * (float) Math.sin(t * 12) * (1 - t));
        for (int i = 0; i < 3; i++) { float rt = Math.max(0, t * 1.6f - i * 0.18f); if (rt <= 0 || rt >= 1) continue; float rr = 14 + rt * 70; D.ring(cx, cy, rr, rr + 2.2f, D.a(V.grad(i * 0.3f), (1 - rt) * a), D.a(0xFFFFD86B, (1 - rt) * a)); }
        for (int i = 8; i >= 1; i--) D.circle(cx, cy, 6 + i * 4 * pop, D.alpha(0xFFE08A, (int) (10 * a)));
        if (tot != null) { float s = 3.2f * pop; D.g.push(cx - 8 * s, cy - 8 * s, s); D.g.item(tot, 0, 0); D.g.pop(); }
        String txt = "Тотем спас вас!"; D.textS(txt, cx - D.w(txt) * 0.6f, cy + 34 * pop, 1.2f, D.a(0xFFFFE8A0, a));
    }
    // ------------------------------------------------------------------ alerts (armour, HP, food, totem, effects)
    private static String alertT = "", alertS = ""; private static int alertC; private static long alertAt; private static String alertIcon = "heart"; private static float alertDur = 2.8f;
    public static void alert(String title, String text, int color, String icon) { alert(title, text, color, icon, ""); }
    /** key: which phrase the announcer says (alert_<key>.wav); otherwise a soft signal, never the "module off" voice */
    public static void alert(String title, String text, int color, String icon, String key) {
        if (!V.on("alerts")) return; alertT = title; alertS = text; alertC = color; alertIcon = icon; alertAt = System.currentTimeMillis(); alertDur = 2.8f;
        Voice.alert(key.isEmpty() ? "generic" : key);
    }
    /** invites and messages from friends: the launcher writes config/asteria-notify.json, shown here for 6 s with a sound (even with the alerts module off) */
    private static long notifyAt, notifyMtime, notifySeen = System.currentTimeMillis();
    private static void friendNote() {
        try { java.io.File f = new java.io.File(V.dir(), "asteria-notify.json"); if (!f.exists() || f.lastModified() == notifyMtime) return; notifyMtime = f.lastModified();
            com.google.gson.JsonObject o = new com.google.gson.JsonParser().parse(new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            long t = (long) V.d(o, "t", 0); if (t <= notifySeen) return; notifySeen = t; boolean inv = "invite".equals(V.str(o, "kind"));
            alertT = V.str(o, "title"); alertS = V.str(o, "text"); if (alertS.length() > 70) alertS = alertS.substring(0, 69) + "…"; alertC = inv ? 0xFF4ADE80 : 0xFF22D3EE; alertIcon = inv ? "users" : "dot"; alertAt = System.currentTimeMillis(); alertDur = 6f;
            Voice.alert("friend"); } catch (Throwable ignored) {}
    }
    /** the alert: a dark glass pill sliding from the top, the icon in a tinted round badge, no side bars */
    private static void alertBanner(int W, int H) {
        float t = (System.currentTimeMillis() - alertAt) / 1000f; if (t > alertDur) { alertAt = 0; return; }
        float in = D.ease(Math.min(1, t * 5)), out = t > alertDur - 0.4f ? D.ease((t - alertDur + 0.4f) / 0.4f) : 0, a = in * (1 - out);
        float h = 32, w = Math.max(D.w(alertT), D.w(alertS) * 0.8f) + 50, x = (W - w) / 2, y = H * 0.2f - 12 * (1 - in);
        D.round(x - 3, y - 3, x + w + 3, y + h + 3, 14, D.a(alertC, 0.10f * a));
        D.round(x, y, x + w, y + h, 12, D.alpha(0x0B0E16, (int) (230 * a)));
        D.round(x, y, x + w, y + h, 12, D.a(alertC, 0.12f * a), D.a(alertC, 0.03f * a));
        D.round(x + 7, y + 6, x + 27, y + 26, 10, D.a(alertC, 0.22f * a));
        D.icon(alertIcon, x + 10, y + 9, 14, D.a(D.mix(alertC, 0xFFFFFFFF, 0.25f), a));
        D.g.text(alertT, x + 35, y + 7, D.a(0xFFFFFFFF, a), false); D.textS(alertS, x + 35, y + 19, 0.8f, D.a(0xFFB9CBD4, a));
    }
    /** a module switched by its key: sound + notification */
    public static void bindToggled(V.Mod m) { notify(m.name, m.on ? "Включено" : "Выключено", m.on); Voice.toggled(m); if (false) Fx.sound(m.on ? "wv_enable" : "wv_disable", 0.6f); }

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
            D.g.pop();
            y += 34 * (1 - out);
        }
    }

    // ------------------------------------------------------------------ crosshair
    private static float xrot, xcx, xcy;
    private static void rrect(float x0, float y0, float x1, float y1, int c) {
        if (xrot == 0) { D.rect(x0, y0, x1, y1, c); return; } double a = Math.toRadians(xrot), cs = Math.cos(a), sn = Math.sin(a); float[] xy = new float[8]; float[][] p = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
        for (int i = 0; i < 4; i++) { float dx = p[i][0] - xcx, dy = p[i][1] - xcy; xy[i * 2] = (float) (xcx + dx * cs - dy * sn); xy[i * 2 + 1] = (float) (xcy + dx * sn + dy * cs); }
        D.g.poly(xy, new int[]{c, c, c, c}); }
    private static void xrect(float x0, float y0, float x1, float y1, int c, int o, float ot) { if (ot > 0) rrect(x0 - ot, y0 - ot, x1 + ot, y1 + ot, o); rrect(x0, y0, x1, y1, c); }
    private static void crosshair(int W, int H, Game game) {
        int st = V.mode("crosshair", "style"); if (st == 7) return; // vanilla: the game draws its own
        if (V.bool("crosshair", "hidef5") && game.thirdPerson()) return;
        float cx = W / 2f, cy = H / 2f, al = (float) V.num("crosshair", "alpha"); xrot = 0; xcx = cx; xcy = cy; Object aim = V.bool("crosshair", "reach") ? game.crosshairEntity() : null; // an entity under the crosshair within reach: you can hit it now
        int c = D.a(aim != null ? V.color("crosshair", "reachcolor") : V.color("crosshair", "color"), al), o = D.a(0xFF000000, al * 0.85f);
        float size = (float) V.num("crosshair", "size"), gap = (float) V.num("crosshair", "gap"), th = (float) V.num("crosshair", "thick"), ot = V.bool("crosshair", "outline") ? (float) V.num("crosshair", "othick") : 0;
        if (V.bool("crosshair", "dynamic")) gap += (1 - game.cooldown()) * 4;
        if (st == 8) { java.io.File f = new java.io.File(V.dir(), "asteria-crosshair.png"); float s = size * 4 + 8;
            if (f.exists()) { D.g.push(0, 0, 1); try { D.g.image("xh:" + f.lastModified(), f, cx - s / 2, cy - s / 2, s, s); } finally { D.g.pop(); } } return; }
        if (st == 6) { String p = V.text("crosshair", "pattern"); int n = (int) Math.round(Math.sqrt(p.length())); if (n < 3 || n * n != p.length()) return;
            float cs = th, x0 = cx - n * cs / 2, y0 = cy - n * cs / 2;
            for (int pass = ot > 0 ? 0 : 1; pass < 2; pass++) for (int i = 0; i < n * n; i++) if (p.charAt(i) == '1') { float x = x0 + (i % n) * cs, y = y0 + (i / n) * cs;
                if (pass == 0) rrect(x - ot, y - ot, x + cs + ot, y + cs + ot, o); else rrect(x, y, x + cs, y + cs, c); }
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
                if (i % 3 == 0) D.circle(px, py, 1.2f + 0.5f * tw, D.a(0xFFFFFFFF, 0.5f + 0.5f * tw)); else D.circle(px, py, 1f + 0.4f * tw, D.a(V.grad(i / 7f), 0.6f + 0.4f * tw)); }
        }
        if (top) { float cx = W / 2f, y = 10; int c = D.a(V.a2(), 0.85f);
            D.hgrad(cx - 130, y - 0.35f, cx - 14, y + 0.35f, D.a(V.a1(), 0f), c); D.hgrad(cx + 14, y - 0.35f, cx + 130, y + 0.35f, c, D.a(V.a1(), 0f));
            for (int s = -1; s <= 1; s += 2) { D.circle(cx + s * 52, y, 1.1f, D.a(0xFFFFFFFF, 0.8f)); D.circle(cx + s * 92, y, 0.9f, D.a(V.a1(), 0.8f)); }
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
    private static String time(int ticks) { if (ticks < 0 || ticks >= 32000) return "∞"; int s = ticks / 20; return (s / 60) + ":" + String.format("%02d", s % 60); }
    private static String facing(float yaw) { String[] d = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"}; float y = ((yaw % 360) + 360) % 360; return d[Math.round(y / 45f) % 8]; }
    private static String roman(int n) { String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}; return n > 0 && n < r.length ? r[n] : String.valueOf(n); }
}
