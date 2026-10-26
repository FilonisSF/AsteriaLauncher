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

    private static int frames, fps; private static long fpsAt;
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
        // clicks per second
        boolean lmb = game.key(6) && !game.screenOpen(), rmb = game.key(7) && !game.screenOpen();
        if (lmb && !lmbWas) LMB.add(now); if (rmb && !rmbWas) RMB.add(now); lmbWas = lmb; rmbWas = rmb;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.pollFirst();
        if (now - speedAt >= 250) { double dx = game.x() - lx, dz = game.z() - lz; double inst = speedAt == 0 ? 0 : Math.sqrt(dx * dx + dz * dz) / ((now - speedAt) / 1000.0); speed = inst > 100 ? speed : speed * 0.5 + inst * 0.5; lx = game.x(); lz = game.z(); speedAt = now; }

        int W = game.width(), H = game.height();
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
        if (t != null || editing) place("targethud", W, H, s, () -> new float[]{120, 38}, () -> target(game, t));
        place("arraylist", W, H, s, Hud::listSize, Hud::arraylist);
        place("notifications", W, H, s, Hud::notesSize, Hud::notes);
    }

    interface Size { float[] get(); }
    /** draws an element at its saved position; x/y > 0.5 anchor to the right/bottom edge so elements grow away from the edge */
    private static void place(String id, int W, int H, float s, Size size, Runnable draw) {
        V.Mod m = V.mod(id); if (m == null || !(m.on || editing)) return;
        float[] wh = size.get(); float w = wh[0] * s, h = wh[1] * s; if (w <= 0 || h <= 0) return;
        float x = m.hud[0] > 0.5f ? m.hud[0] * W - w : m.hud[0] * W, y = m.hud[1] > 0.5f ? m.hud[1] * H - h : m.hud[1] * H;
        if (id.equals("armor") || id.equals("targethud")) x = m.hud[0] * W - w / 2; // centred elements
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
        else D.panel(0, 0, w, 18, 4);
        float spin = (System.currentTimeMillis() % 6000) / 6000f;
        D.g.push(10, 9, 1); D.star(0, 0, 6.5f + (float) Math.sin(spin * Math.PI * 2) * 0.6f, V.a1(), V.a2()); D.g.pop();
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
        for (String[] l : r) w = Math.max(w, D.w(l[0]) + D.w(l[1]) + 18);
        return new float[]{w, r.size() * 11 + 7};
    }
    private static void info(Game game) {
        List<String[]> r = infoLines(game); float w = infoSize(game)[0];
        D.panel(0, 0, w, r.size() * 11 + 7, 4);
        float y = 4;
        for (int i = 0; i < r.size(); i++) { String[] l = r.get(i);
            D.text(l[0], 6, y, V.grad(i * 0.12f)); D.text(l[1], w - 6 - D.w(l[1]), y, 0xFFEAF6FA); y += 11; }
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
        D.shadow(x, y, x + w, y + h, 4, 0.6f);
        D.round(x, y, x + w, y + h, 4, D.alpha(0x0A1520, (int) (255 * V.bg)));
        if (p > 0.01f) D.round(x, y, x + w, y + h, 4, D.a(V.a1(), p), D.a(V.a2(), p));
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
        return row ? new float[]{n * 22 + 4, 28} : new float[]{V.bool("armor", "percent") ? 48 : 24, n * 20 + 4};
    }
    private static void armor(List<Game.Item> items) {
        boolean row = V.mode("armor", "layout") == 0, pct = V.bool("armor", "percent");
        float[] wh = armorSize(items); D.panel(0, 0, wh[0], wh[1], 4);
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
        float[] wh = effSize(l); D.panel(0, 0, wh[0], wh[1], 4);
        float y = 3;
        for (Game.Effect e : l) {
            String k = e.name + e.amp; Integer mx = EFFMAX.get(k); if (mx == null || e.ticks > mx) { mx = e.ticks; EFFMAX.put(k, mx); }
            int col = 0xFF000000 | e.color;
            D.round(4, y + 2, 22, y + 20, 4, D.a(col, 0.25f));
            if (!D.g.effectIcon(e.type, 5, (int) y + 3, 16)) D.circle(13, y + 11, 5, col);
            D.text(effName(e), 27, y + 2, 0xFFEAF6FA);
            boolean low = e.ticks < 200 && e.ticks < 32000;
            D.text(time(e.ticks), 27, y + 12, low && (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFF6B6B : 0xFF9FB3C0);
            if (V.bool("effects", "bars")) { float f = e.ticks >= 32000 ? 1 : Math.max(0, Math.min(1, e.ticks / (float) mx)); D.round(27 + D.w(time(e.ticks)) + 5, y + 15, wh[0] - 5, y + 17, 1, 0x40FFFFFF); float bx = 27 + D.w(time(e.ticks)) + 5; D.round(bx, y + 15, bx + (wh[0] - 5 - bx) * f, y + 17, 1, col); }
            y += 24;
        }
    }
    private static String effName(Game.Effect e) { return e.amp > 0 ? e.name + " " + roman(e.amp + 1) : e.name; }

    private static void target(Game game, Object t) {
        float w = 120, h = 38;
        D.panel(0, 0, w, h, 6);
        if (t == null) { D.text("Цель", 8, 15, 0xAAFFFFFF); return; }
        boolean head = V.bool("targethud", "head"); float tx = 7;
        if (head) { Object skin = game.skin(t); float hs = 26; D.round(5, 5, 5 + hs + 2, 5 + hs + 2, 5, D.a(V.a1(), 0.5f), D.a(V.a2(), 0.5f));
            if (skin != null) D.g.head(skin, 6, 6, hs); else { D.round(6, 6, 6 + hs, 6 + hs, 4, 0xFF203040); D.textC("?", 6 + hs / 2, 6 + hs / 2 - 4, 0xFFFFFFFF); }
            tx = 38; }
        float hp = Math.max(0, game.hp(t)), mx = Math.max(1, game.maxHp(t)), f = Math.min(1, hp / mx);
        float shown = D.anim("thp" + System.identityHashCode(t), f, 6);
        String name = game.name(t); while (D.w(name) > w - tx - 32 && name.length() > 3) name = name.substring(0, name.length() - 1);
        D.text(name, tx, 6, 0xFFFFFFFF);
        String hpS = String.format("%.1f", hp / 2) + "❤"; D.text(hpS, w - 6 - D.w(hpS), 6, dur(f));
        D.round(tx, 22, w - 7, 28, 3, 0x50000000);
        D.round(tx, 22, tx + Math.max(3, (w - 7 - tx) * shown), 28, 3, D.alpha(0xFFFFFF, 90)); // trailing damage
        D.round(tx, 22, tx + Math.max(3, (w - 7 - tx) * f), 28, 3, V.a1(), V.a2());
    }

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
        long now = System.currentTimeMillis(); NOTES.removeIf(n -> now - n.at > 3200);
        float y = 0;
        for (Note n : NOTES) {
            float age = (now - n.at) / 1000f, in = D.ease(Math.min(1, age * 4)), out = age > 2.8f ? D.ease((age - 2.8f) * 2.5f) : 0;
            float x = (1 - in + out) * 150;
            D.g.push(x, y, 1);
            D.panel(0, 0, 140, 30, 6);
            D.round(6, 7, 22, 23, 8, n.on ? 0xFF22C55E : 0xFFEF4444); D.textC(n.on ? "✔" : "✖", 14, 11, 0xFFFFFFFF);
            D.text(n.title, 28, 6, 0xFFFFFFFF); D.text(n.text, 28, 17, 0xFF9FB3C0);
            D.round(4, 27, 4 + 132 * Math.max(0, 1 - age / 3f), 28.5f, 0.7f, V.a1(), V.a2());
            D.g.pop();
            y += 34 * (1 - out);
        }
    }

    // ------------------------------------------------------------------ crosshair
    private static void crosshair(int W, int H, Game game) {
        float cx = W / 2f, cy = H / 2f; int c = V.color("crosshair", "color"); int st = V.mode("crosshair", "style");
        float size = (float) V.num("crosshair", "size"), gap = (float) V.num("crosshair", "gap"), th = (float) V.num("crosshair", "thick");
        if (V.bool("crosshair", "dynamic")) gap += (1 - game.cooldown()) * 4;
        int o = 0x99000000;
        switch (st) {
            case 1: D.circle(cx, cy, th + 1.2f, o); D.circle(cx, cy, th + 0.3f, c); break;
            case 2: D.ring(cx, cy, size + gap, size + gap + th + 0.6f, o, o); D.ring(cx, cy, size + gap + 0.3f, size + gap + th + 0.3f, c, c); D.circle(cx, cy, 0.8f, c); break;
            case 3: for (int i = 0; i < 4; i++) { float sx = (i % 2 == 0 ? -1 : 1), sy = (i < 2 ? -1 : 1), x = cx + sx * (gap + 1), y = cy + sy * (gap + 1);
                D.rect(Math.min(x, x + sx * size), y - th / 2, Math.max(x, x + sx * size), y + th / 2, c); D.rect(x - th / 2, Math.min(y, y + sy * size), x + th / 2, Math.max(y, y + sy * size), c); } break;
            default:
                for (float[] d : new float[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    float x1 = cx + d[0] * gap, y1 = cy + d[1] * gap, x2 = cx + d[0] * (gap + size), y2 = cy + d[1] * (gap + size);
                    float hx = d[0] == 0 ? th / 2 : 0, hy = d[1] == 0 ? th / 2 : 0;
                    D.rect(Math.min(x1, x2) - hx - 0.5f, Math.min(y1, y2) - hy - 0.5f, Math.max(x1, x2) + hx + 0.5f, Math.max(y1, y2) + hy + 0.5f, o);
                    D.rect(Math.min(x1, x2) - hx, Math.min(y1, y2) - hy, Math.max(x1, x2) + hx, Math.max(y1, y2) + hy, c);
                }
        }
    }

    private static void locked(Game game) {
        String a = "Asteria Visuals", b = "Нужна подписка PREMIUM — визуалы выключены";
        float w = Math.max(D.w(a), D.w(b)) + 30, x = (game.width() - w) / 2;
        D.g.push(x, 8, 1); D.panel(0, 0, w, 28, 6); D.g.push(13, 14, 1); D.star(0, 0, 6, V.a1(), V.a2()); D.g.pop();
        D.gradText(a, 24, 5, true); D.text(b, 24, 16, 0xFFEAF6FA); D.g.pop();
    }

    // ------------------------------------------------------------------ util
    static int dur(float f) { return f > 0.6f ? 0xFF4ADE80 : f > 0.3f ? 0xFFFACC15 : 0xFFF87171; }
    private static String time(int ticks) { if (ticks >= 32000) return "∞"; int s = ticks / 20; return (s / 60) + ":" + String.format("%02d", s % 60); }
    private static String facing(float yaw) { String[] d = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"}; float y = ((yaw % 360) + 360) % 360; return d[Math.round(y / 45f) % 8]; }
    private static String roman(int n) { String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}; return n > 0 && n < r.length ? r[n] : String.valueOf(n); }
}
