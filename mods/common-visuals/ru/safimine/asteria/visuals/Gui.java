package ru.safimine.asteria.visuals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The in-game Asteria menu (Right Shift by default): categories, module cards with switches, sliders, modes, colours,
 * per-module key binds, theme page and a drag-and-drop HUD editor. Pure drawing on {@link Gfx}; the version adapter
 * only forwards mouse/keyboard events.
 */
public final class Gui {
    private Gui() {}
    static final float W = 440, H = 276, SIDE = 116;
    private static String cat = "hud", search = ""; private static boolean searchFocus;
    private static float scroll, scrollTarget, openT; private static long openedAt;
    private static String expanded = null; private static V.Mod listening = null; private static V.S textEdit = null;
    private static V.S dragging = null; private static float dragX0, dragW;
    private static boolean themeDragBg, themeDragScale;
    private static Hud.Box moving = null; private static float moveDX, moveDY;
    private static V.S colorOpen = null;
    private static final List<Hit> HITS = new ArrayList<>();
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy");
    private static float sw = 1, ox, oy; // window scale and origin
    private static int screenW, screenH;

    private static final class Hit { float x1, y1, x2, y2; Runnable click; String id; Hit(float a, float b, float c, float d, String id, Runnable r) { x1 = a; y1 = b; x2 = c; y2 = d; this.id = id; click = r; } }

    public static boolean open;
    public static void opened() { open = true; openedAt = System.currentTimeMillis(); openT = 0; D.set("gui.open", 0); D.frame(); Hud.editing = false; listening = null; searchFocus = false; }
    public static void closed() { open = false; Hud.editing = false; listening = null; textEdit = null; dragging = null; moving = null; V.changed(); }

    // ================================================================== render
    public static void render(Gfx g, Game game, float mx, float my) {
        D.g = g; D.frame(); screenW = game.width(); screenH = game.height();
        HITS.clear();
        if (Hud.editing) { editor(g, game, mx, my); return; }
        openT = D.anim("gui.open", 1, 9);
        float a = D.ease(openT);
        D.rect(0, 0, screenW, screenH, D.alpha(0x02060A, (int) (150 * a)));
        // a scale that is a whole number of half screen pixels: the text stays as sharp as in the HUD (no zoom animation for the same reason)
        { float k = g.pxScale() * 2, fit = Math.min(1, Math.min((screenW - 20) / W, (screenH - 20) / H)); sw = Math.max(1, (float) Math.floor(fit * k)) / k; }
        ox = (screenW - W * sw) / 2; oy = (screenH - H * sw) / 2 + (1 - a) * 12;
        float lmx = (mx - ox) / sw, lmy = (my - oy) / sw;
        g.push(ox, oy, sw);
        // window
        D.shadow(0, 0, W, H, 10, 2.2f);
        D.round(0, 0, W, H, 10, D.alpha(0x070F17, 245));
        D.round(0, 0, SIDE, H, 10, D.alpha(0x0B1822, 255));
        D.rect(SIDE - 10, 0, SIDE, H, D.alpha(0x0B1822, 255));
        D.rect(SIDE, 8, SIDE + 0.6f, H - 8, 0x22FFFFFF);
        D.round(10, 0, W - 10, 1.2f, 0.5f, V.a1(), V.a2());
        sidebar(g, game, lmx, lmy);
        if (cat.equals("theme")) theme(g, lmx, lmy); else if (cat.equals("configs")) configs(g, lmx, lmy); else modules(g, game, lmx, lmy);
        g.pop();
        if (listening != null) { String t = "Нажмите клавишу или кнопку мыши для «" + listening.name + "»  ·  Esc — убрать"; float w = D.w(t) + 20;
            D.panel((screenW - w) / 2, screenH - 34, (screenW + w) / 2, screenH - 14, 6); D.textC(t, screenW / 2f, screenH - 28, 0xFFFFFFFF); }
    }

    private static void sidebar(Gfx g, Game game, float mx, float my) {
        D.mark(5, 8, 22);
        { float ts = Math.min(1.25f, (SIDE - 40) / (float) Math.max(1, D.w("Asteria"))); D.g.push(32, 18 - 5 * ts, ts); D.gradText("Asteria", 0, 0, true); D.g.pop(); }
        D.textS("VISUALS", 33, 25, 0.6f, 0xFF6F8B99);
        float y = 44;
        List<V.Cat> cats = new ArrayList<>(V.CATS); V.Cat th = new V.Cat(); th.id = "theme"; th.name = "Тема"; th.icon = "◐"; cats.add(th);
        V.Cat cf = new V.Cat(); cf.id = "configs"; cf.name = "Конфиги"; cf.icon = "☰"; cats.add(cf);
        for (V.Cat c : cats) {
            boolean on = c.id.equals(cat), hov = in(mx, my, 8, y, SIDE - 8, y + 20);
            float p = D.anim("cat." + c.id, on ? 1 : 0, 12);
            if (hov && !on) D.round(8, y, SIDE - 8, y + 20, 6, 0x14FFFFFF);
            if (p > 0.01f) { D.round(8, y, 8 + (SIDE - 16) * p, y + 20, 6, D.a(V.a1(), 0.9f * p), D.a(V.a2(), 0.9f * p)); }
            D.icon(catIcon(c.id), 13, y + 4.5f, 11, on ? 0xFFFFFFFF : D.a(V.a1(), 1));
            D.g.text(c.name, 30, y + 6, on ? 0xFFFFFFFF : 0xFFB9CBD4, false);
            int count = 0; for (V.Mod m : V.MODS) if (m.cat.equals(c.id) && m.on) count++;
            if (count > 0 && !c.id.equals("theme")) { String n = String.valueOf(count); D.round(SIDE - 22 - D.w(n) * 0.7f, y + 5, SIDE - 13, y + 15, 5, on ? 0x33FFFFFF : 0x22FFFFFF); D.textS(n, SIDE - 17.5f - D.w(n) * 0.7f, y + 7, 0.7f, 0xFFFFFFFF); }
            final String id = c.id; hit(8, y, SIDE - 8, y + 20, "cat", () -> { cat = id; scroll = scrollTarget = 0; expanded = null; colorOpen = null; });
            y += 23;
        }
        // HUD editor button
        float by = H - 52; boolean hov = in(mx, my, 8, by, SIDE - 8, by + 20);
        D.round(8, by, SIDE - 8, by + 20, 6, hov ? 0x30FFFFFF : 0x1AFFFFFF); String el = "Изменить HUD"; float es = Math.min(1f, (SIDE - 16 - 26) / (float) D.w(el)), tw = D.w(el) * es + 14; D.icon("move", SIDE / 2f - tw / 2, by + 5, 10, 0xFFFFFFFF); D.textS(el, SIDE / 2f - tw / 2 + 14, by + 10 - 4 * es, es, 0xFFFFFFFF);
        hit(8, by, SIDE - 8, by + 20, "hud", () -> Hud.editing = true);
        // licence line
        D.round(8, H - 26, SIDE - 8, H - 10, 5, D.a(V.a1(), 0.18f), D.a(V.a2(), 0.18f));
        D.mark(11, H - 27, 18); D.textS("AsteriaLauncher", 31, H - 21, 0.8f, 0xFFE0F2FE);
    }

    // ------------------------------------------------------------------ module list
    private static void modules(Gfx g, Game game, float mx, float my) {
        float x0 = SIDE + 14, x1 = W - 14, top = 46;
        String title = cat; for (V.Cat c : V.CATS) if (c.id.equals(cat)) title = c.name;
        D.g.push(x0, 16, 1.35f); D.g.text(search.isEmpty() ? title : "Поиск", 0, 0, 0xFFFFFFFF, true); D.g.pop();
        // search box
        float sx = x1 - 120; boolean sh = in(mx, my, sx, 13, x1, 31);
        D.round(sx, 13, x1, 31, 6, searchFocus ? 0x30FFFFFF : sh ? 0x22FFFFFF : 0x16FFFFFF);
        if (searchFocus) D.rim(sx, 13, x1, 31, 6, 0.8f, 150, 60); // focus: a soft light rim instead of a line under the field
        D.icon("search", sx + 5, 17.5f, 9, 0xFF8FA6B2);
        String st = search.isEmpty() && !searchFocus ? "Найти функцию" : search + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        D.g.text(st, sx + 17, 18, search.isEmpty() && !searchFocus ? 0xFF6F8B99 : 0xFFFFFFFF, false);
        hit(sx, 13, x1, 31, "search", () -> searchFocus = true);

        List<V.Mod> list = new ArrayList<>();
        for (V.Mod m : V.MODS) if (search.isEmpty() ? m.cat.equals(cat) : (m.name + " " + m.desc).toLowerCase().contains(search.toLowerCase())) list.add(m);
        scroll += (scrollTarget - scroll) * Math.min(1, D.dt * 16);
        g.scissor(ox + (SIDE + 1) * sw, oy + top * sw, (W - SIDE - 2) * sw, (H - top - 6) * sw);
        float y = top - scroll, total = 0;
        for (V.Mod m : list) { float h = card(g, game, m, x0, y, x1, mx, my); y += h + 6; total += h + 6; }
        if (list.isEmpty()) D.textC("Ничего не найдено", (x0 + x1) / 2, top + 30, 0xFF6F8B99);
        g.noScissor();
        float maxScroll = Math.max(0, total - (H - top - 10)); scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        if (maxScroll > 0) { float vis = (H - top - 10), bar = vis * vis / (total), by = top + (vis - bar) * (scroll / maxScroll); D.round(W - 7, by, W - 5, by + bar, 1, 0x40FFFFFF); }
    }

    private static float card(Gfx g, Game game, V.Mod m, float x0, float y, float x1, float mx, float my) {
        boolean exp = m.id.equals(expanded);
        float bodyH = exp ? settingsHeight(m) : 0, h = 32 + D.anim("exp." + m.id, bodyH, 14);
        if (y > H + 10 || y + h < 30) return h; // off screen
        boolean hov = in(mx, my, x0, y, x1, y + 32) && my > 44;
        float p = D.anim("on." + m.id, m.on ? 1 : 0, 12);
        D.round(x0, y, x1, y + h, 7, hov ? 0x1EFFFFFF : 0x12FFFFFF);
        if (p > 0.01f) D.vgradRound(x0 + 3, y + 7, x0 + 5.5f, Math.min(y + h - 7, y + 25), D.a(V.a1(), p), D.a(V.a2(), p));
        hit(x0, y, x1, y + 32, "card", () -> { m.on = !m.on; V.changed(); Hud.notify(m.name, m.on ? "Включено" : "Выключено", m.on); if (V.on("sounds") && V.bool("sounds", "menu")) Fx.sound(m.on ? "module_enable" : "module_disable", (float) V.num("sounds", "volume")); });
        D.g.text(m.name, x0 + 10, y + 6, D.mix(0xFFCFE0E8, 0xFFFFFFFF, p), false);
        D.textS(m.desc, x0 + 10, y + 18, 0.72f, 0xFF7E97A4);
        // switch
        float sx = x1 - 30; toggleSwitch(sx, y + 10, p);
        // bind pill
        String bl = listening == m ? "…" : m.bind == 0 ? "" : game.keyLabel(m.bind);
        float bx = sx - 8 - (bl.isEmpty() ? 14 : D.w(bl) * 0.75f + 10);
        boolean bh = in(mx, my, bx, y + 10, sx - 6, y + 22);
        D.round(bx, y + 10, sx - 6, y + 22, 4, bh ? 0x33FFFFFF : 0x1AFFFFFF);
        if (bl.isEmpty()) D.icon("keyboard", bx + 2.5f, y + 11, 9, 0xFF8FA6B2); else D.textS(bl, bx + 5, y + 13, 0.75f, 0xFFFFFFFF);
        hit(bx, y + 10, sx - 6, y + 22, "bind", () -> listening = m);
        // music controls (the launcher passes them to Spotify / Yandex Music / the browser)
        if (m.id.equals("music")) { String[][] btn = {{"prev", "prev"}, {Media.active() ? "pause" : "play", "toggle"}, {"next", "next"}};
            float qx = bx - 4 - 3 * 17;
            for (String[] b : btn) { boolean qh = in(mx, my, qx, y + 9, qx + 15, y + 23);
                D.round(qx, y + 9, qx + 15, y + 23, 4, qh ? D.a(V.a1(), 0.6f) : 0x1AFFFFFF, qh ? D.a(V.a2(), 0.6f) : 0x1AFFFFFF);
                D.icon(b[0], qx + 3.5f, y + 12, 8, 0xFFFFFFFF); final String c = b[1];
                hit(qx, y + 9, qx + 15, y + 23, "media", () -> Media.cmd(c)); qx += 17; }
            bx -= 3 * 17 + 4; }
        // settings chevron
        if (!m.s.isEmpty()) { float cx = bx - 16; D.icon(exp ? "chevU" : "chevD", cx + 2, y + 11, 10, hov ? 0xFFFFFFFF : 0xFF8FA6B2);
            hit(cx, y + 6, bx - 2, y + 26, "exp", () -> { expanded = exp ? null : m.id; colorOpen = null; }); }
        if (h > 34) {
            boolean opening = Math.abs(h - 32 - bodyH) > 0.5f; // while the card animates, rows are clipped to it instead of spilling over the next card
            float cy0 = Math.max(y + 32, 46), cy1 = Math.min(y + h, H - 6);
            if (opening) g.scissor(ox + (SIDE + 1) * sw, oy + cy0 * sw, (W - SIDE - 2) * sw, Math.max(0, cy1 - cy0) * sw);
            g.push(0, 0, 1);
            float sy = y + 34;
            for (V.S s : m.s) { if (sy > y + h) break; sy += setting(g, game, m, s, x0 + 10, sy, x1 - 10, mx, my); }
            if (!m.s.isEmpty() && sy + 14 <= y + h) { float bx1 = x1 - 10, bx0 = bx1 - 70; boolean rh = in(mx, my, bx0, sy + 1, bx1, sy + 14); // back to the defaults (hands, positions, colours...)
                D.round(bx0, sy + 1, bx1, sy + 14, 5, rh ? 0x50FFFFFF : 0x26FFFFFF); D.textC("Сбросить", (bx0 + bx1) / 2, sy + 4, 0xFFEAF6FA);
                hit(bx0, sy + 1, bx1, sy + 14, "reset:" + m.id, () -> { for (V.S s : m.s) V.reset(s); V.changed(); }); }
            g.pop();
            if (opening) g.scissor(ox + (SIDE + 1) * sw, oy + 46 * sw, (W - SIDE - 2) * sw, (H - 46 - 6) * sw); // back to the list's clip
        }
        return h;
    }
    static String catIcon(String id) { switch (id) { case "hud": return "grid"; case "world": return "cube"; case "combat": return "swords"; case "camera": return "eye"; case "theme": return "palette"; case "configs": return "folder"; default: return "dot"; } }
    private static void toggleSwitch(float x, float y, float p) {
        int c = D.mix(0xFFE5484D, 0xFF2FBF71, p); D.round(x, y, x + 22, y + 12, 6, c, D.mix(c, 0xFFFFFFFF, 0.12f)); // on = green, off = red
        D.circle(x + 6 + 10 * p, y + 6, 4.3f, 0xFFFFFFFF);
    }
    private static float settingsHeight(V.Mod m) { float h = 4; for (V.S s : m.s) h += rowH(s); return m.s.isEmpty() ? h : h + 18; }
    private static float rowH(V.S s) { return s.type.equals("num") ? 24 : s.type.equals("color") && s == colorOpen ? 44 : s.type.equals("mode") ? (s.options.length > 4 ? 18 + (s.options.length + 2) / 3 * 16 : 22) : 18; }

    private static float setting(Gfx g, Game game, V.Mod m, V.S s, float x0, float y, float x1, float mx, float my) {
        float h = rowH(s);
        D.g.text(s.name, x0, y + 3, 0xFFB9CBD4, false);
        switch (s.type) {
            case "bool": {
                float p = D.anim("sb." + m.id + s.id, s.bool ? 1 : 0, 12); toggleSwitch(x1 - 22, y + 1, p);
                hit(x0, y, x1, y + 16, "s", () -> { s.bool = !s.bool; V.changed(); }); break; }
            case "num": {
                String val = fmt(s.num, s.step); D.g.text(val, x1 - D.w(val), y + 3, 0xFFFFFFFF, false);
                float tx0 = x0, tx1 = x1, ty = y + 15, f = (float) ((s.num - s.min) / (s.max - s.min));
                D.round(tx0, ty, tx1, ty + 3, 1.5f, 0x30FFFFFF);
                D.round(tx0, ty, tx0 + (tx1 - tx0) * f, ty + 3, 1.5f, V.a1(), V.a2());
                D.circle(tx0 + (tx1 - tx0) * f, ty + 1.5f, 3.8f, 0xFFFFFFFF);
                hit(tx0 - 3, ty - 5, tx1 + 3, ty + 8, "num", () -> { dragging = s; dragX0 = tx0; dragW = tx1 - tx0; drag(lastMx); });
                break; }
            case "mode": {
                if (s.options.length > 4) { // long lists: every option at once, three per row
                    float cw = (x1 - x0) / 3f;
                    for (int i = 0; i < s.options.length; i++) { final int idx = i; float cx = x0 + (i % 3) * cw, cy = y + 16 + (i / 3) * 16; boolean on = s.mode == i, hov = in(mx, my, cx, cy, cx + cw - 2, cy + 14);
                        D.round(cx, cy, cx + cw - 2, cy + 14, 4, on ? V.a1() : hov ? 0x30FFFFFF : 0x18FFFFFF, on ? V.a2() : hov ? 0x30FFFFFF : 0x18FFFFFF);
                        String o = s.options[i]; float ts = Math.min(0.8f, (cw - 8) / Math.max(1, D.w(o))); D.textS(o, cx + (cw - 2 - D.w(o) * ts) / 2, cy + 7 - 4 * ts, ts, on ? 0xFFFFFFFF : 0xFFB9CBD4);
                        hit(cx, cy, cx + cw - 2, cy + 14, "mode", () -> { s.mode = idx; V.changed(); }); }
                    break;
                }
                float px = x1; for (int i = s.options.length - 1; i >= 0; i--) {
                    String o = s.options[i]; float w = D.w(o) * 0.8f + 10; px -= w;
                    boolean on = s.mode == i; final int idx = i;
                    D.round(px, y + 1, px + w - 2, y + 15, 4, on ? V.a1() : in(mx, my, px, y + 1, px + w - 2, y + 15) ? 0x30FFFFFF : 0x18FFFFFF, on ? V.a2() : in(mx, my, px, y + 1, px + w - 2, y + 15) ? 0x30FFFFFF : 0x18FFFFFF);
                    D.textS(o, px + 4, y + 5, 0.8f, on ? 0xFFFFFFFF : 0xFFB9CBD4);
                    hit(px, y + 1, px + w - 2, y + 15, "mode", () -> { s.mode = idx; V.changed(); });
                } break; }
            case "str": {
                if (s.id.equals("pattern")) { D.textS(s.text.indexOf('1') >= 0 ? "нарисован в лаунчере" : "нарисуйте в лаунчере", x1 - D.w("нарисуйте в лаунчере") * 0.8f, y + 5, 0.8f, 0xFF9FB3C0); break; }
                boolean ed = textEdit == s; String v = s.text + (ed && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
                float fw = Math.min(150, x1 - x0 - D.w(s.name) - 10), fx = x1 - fw;
                D.round(fx, y + 1, x1, y + 15, 4, ed ? D.a(V.a1(), 0.35f) : 0x22FFFFFF, ed ? D.a(V.a2(), 0.35f) : 0x22FFFFFF);
                String shown = v; while (D.w(shown) * 0.8f > fw - 8 && shown.length() > 1) shown = shown.substring(1);
                D.textS(shown.isEmpty() ? "введите текст" : shown, fx + 4, y + 5, 0.8f, shown.isEmpty() ? 0x80FFFFFF : 0xFFFFFFFF);
                hit(fx, y + 1, x1, y + 15, "str", () -> { textEdit = s; searchFocus = false; });
                break; }
            case "color": {
                D.round(x1 - 22, y + 2, x1, y + 14, 4, 0xFFFFFFFF); D.round(x1 - 21, y + 3, x1 - 1, y + 13, 3.5f, s.color);
                hit(x1 - 24, y, x1, y + 16, "color", () -> colorOpen = colorOpen == s ? null : s);
                if (s == colorOpen) {
                    int[] pal = {0xFF22D3EE, 0xFF0EA5E9, 0xFF6366F1, 0xFFA855F7, 0xFFEC4899, 0xFFF43F5E, 0xFFF97316, 0xFFFACC15, 0xFF22C55E, 0xFF14B8A6, 0xFFFFFFFF, 0xFF94A3B8};
                    float px = x0, py = y + 18;
                    for (int c : pal) { final int cc = c; D.round(px, py, px + 14, py + 10, 3, c); if ((s.color & 0xFFFFFF) == (c & 0xFFFFFF)) D.round(px + 5, py + 3, px + 9, py + 7, 2, 0xFF000000);
                        hit(px, py, px + 14, py + 10, "pal", () -> { s.color = cc; V.changed(); }); px += 16; }
                    // hue strip
                    float hy = py + 14, hx0 = x0, hx1 = x1;
                    for (int i = 0; i < 24; i++) D.hgrad(hx0 + (hx1 - hx0) * i / 24f, hy, hx0 + (hx1 - hx0) * (i + 1) / 24f + 0.3f, hy + 6, V.hsv(i / 24f, 0.75f, 1), V.hsv((i + 1) / 24f, 0.75f, 1));
                    hit(hx0, hy - 2, hx1, hy + 8, "hue", () -> { s.color = V.hsv(Math.max(0, Math.min(1, ((lastMx - ox) / sw - hx0) / (hx1 - hx0))), 0.75f, 1); V.changed(); });
                }
                break; }
        }
        return h;
    }
    private static float lastMx, lastMy;

    // ------------------------------------------------------------------ theme page
    private static void theme(Gfx g, float mx, float my) {
        float x0 = SIDE + 14, x1 = W - 14, y = 16;
        D.g.push(x0, y, 1.35f); D.g.text("Тема", 0, 0, 0xFFFFFFFF, true); D.g.pop(); y += 30;
        D.g.text("Цвета", x0, y, 0xFFB9CBD4, false); y += 13;
        int[][] presets = {{0xFF22D3EE, 0xFF3B82F6}, {0xFF22D3EE, 0xFFA855F7}, {0xFFF43F5E, 0xFFF59E0B}, {0xFF22C55E, 0xFF06B6D4}, {0xFFE879F9, 0xFF6366F1}, {0xFFFACC15, 0xFFFB7185}, {0xFFF472B6, 0xFFA78BFA}, {0xFFE2E8F0, 0xFF64748B}};
        float px = x0;
        for (int[] pr : presets) { boolean on = !V.chroma && V.accent == pr[0] && V.accent2 == pr[1];
            if (on) D.round(px - 2, y - 2, px + 32, y + 22, 7, 0xFFFFFFFF);
            D.round(px, y, px + 30, y + 20, 6, pr[0], pr[1]); hit(px, y, px + 30, y + 20, "pre", () -> { V.accent = pr[0]; V.accent2 = pr[1]; V.chroma = false; V.changed(); }); px += 36; }
        y += 32;
        y = styleRow(x0, y, x1);
        y = themeRow("Переливающиеся цвета (RGB)", V.chroma, x0, y, x1, () -> { V.chroma = !V.chroma; V.changed(); });
        y = themeRow("Тени", V.shadow, x0, y, x1, () -> { V.shadow = !V.shadow; V.changed(); });
        y = fontRow(x0, y, x1, mx);
        y = themeRow("Свой стиль меню Minecraft", V.menu, x0, y, x1, () -> { V.menu = !V.menu; V.changed(); });
        y = themeRow("Блик по панелям", V.glare, x0, y, x1, () -> { V.glare = !V.glare; V.changed(); });
        y += 4;
        y = themeSlider("Прозрачность панелей", V.bg, 0.15f, 0.95f, Math.round(V.bg * 100) + "%", x0, y, x1, mx, 0);
        y = themeSlider("Размер HUD", V.scale, 0.5f, 2f, Math.round(V.scale * 100) + "%", x0, y, x1, mx, 1);
        y += 6;
        D.panel(x0, y, x1, y + 36, 6); D.mark(x0 + 4, y + 8, 20);
        D.gradText("Так выглядят панели HUD", x0 + 28, y + 9, true); D.textS("Настройки тем сохраняются сразу и видны в лаунчере", x0 + 28, y + 21, 0.75f, 0xFF9FB3C0);
    }
    // ------------------------------------------------------------------ configs (made in the launcher)
    private static void configs(Gfx g, float mx, float my) {
        float x0 = SIDE + 14, x1 = W - 14, y = 16;
        D.g.push(x0, y, 1.35f); D.g.text("Конфиги", 0, 0, 0xFFFFFFFF, true); D.g.pop(); y += 30;
        if (V.PRESETS.isEmpty()) { D.textS("Конфиги создаются в лаунчере: Визуалы → Конфиги.", x0, y, 0.85f, 0xFF9FB3C0); return; }
        for (V.Preset p : V.PRESETS) {
            if (y > H - 30) break;
            boolean act = p.id.equals(V.activePreset), hov = in(mx, my, x0, y, x1, y + 24);
            D.round(x0, y, x1, y + 24, 6, act ? D.a(V.a1(), 0.28f) : hov ? 0x26FFFFFF : 0x14FFFFFF, act ? D.a(V.a2(), 0.28f) : hov ? 0x26FFFFFF : 0x14FFFFFF);
            D.g.text(p.name, x0 + 10, y + 8, 0xFFFFFFFF, false);
            String r = act ? "активный" : "включить"; D.textS(r, x1 - 10 - D.w(r) * 0.75f, y + 9, 0.75f, act ? 0xFFFFFFFF : 0xFF9FB3C0);
            if (!act) hit(x0, y, x1, y + 24, "preset", () -> V.usePreset(p));
            y += 28;
        }
        D.textS("Новые конфиги, импорт и коды для друзей — в лаунчере.", x0, H - 18, 0.72f, 0xFF6F8B99);
    }
    /** font picker: "‹ name ›", the name drawn in that font */
    private static float fontRow(float x0, float y, float x1, float mx) {
        D.g.text("Шрифт", x0, y + 3, 0xFFB9CBD4, false);
        String n = V.FONTS[V.font]; float w = Math.max(96, D.w(n) + 30), px = x1 - w;
        D.round(px, y, x1, y + 15, 5, D.a(V.a1(), 0.35f), D.a(V.a2(), 0.35f));
        D.icon("chevL", px + 4, y + 3.5f, 8, 0xFFFFFFFF); D.icon("chevR", x1 - 12, y + 3.5f, 8, 0xFFFFFFFF);
        D.g.text(n, px + (w - D.w(n)) / 2, y + 4, 0xFFFFFFFF, false);
        int k = V.FONTS.length;
        hit(px, y, px + w / 2, y + 15, "font", () -> { V.font = (V.font + k - 1) % k; V.changed(); });
        hit(px + w / 2, y, x1, y + 15, "font", () -> { V.font = (V.font + 1) % k; V.changed(); });
        return y + 20;
    }
    private static final String[] STYLES = {"Кристалл", "Созвездие", "Минимал", "Стекло"};
    private static float styleRow(float x0, float y, float x1) {
        D.g.text("Стиль панелей", x0, y + 3, 0xFFB9CBD4, false);
        float cw = (x1 - x0) / STYLES.length; // all styles in one row under the title
        for (int i = 0; i < STYLES.length; i++) { String o = STYLES[i]; float px = x0 + i * cw; boolean on = V.hudStyle == i; final int idx = i; float ts = Math.min(0.8f, (cw - 8) / Math.max(1, D.w(o)));
            D.round(px, y + 15, px + cw - 2, y + 29, 4, on ? V.a1() : 0x18FFFFFF, on ? V.a2() : 0x18FFFFFF); D.textS(o, px + (cw - 2 - D.w(o) * ts) / 2, y + 22 - 4 * ts, ts, on ? 0xFFFFFFFF : 0xFFB9CBD4);
            hit(px, y + 15, px + cw - 2, y + 29, "style", () -> { V.hudStyle = idx; V.changed(); }); }
        return y + 34;
    }
    private static float themeRow(String label, boolean on, float x0, float y, float x1, Runnable click) {
        D.g.text(label, x0, y + 3, 0xFFB9CBD4, false); toggleSwitch(x1 - 22, y + 1, D.anim("th." + label, on ? 1 : 0, 12)); hit(x0, y, x1, y + 16, "th", click); return y + 20;
    }
    private static float themeSlider(String label, float v, float min, float max, String val, float x0, float y, float x1, float mx, int which) {
        D.g.text(label, x0, y + 3, 0xFFB9CBD4, false); D.g.text(val, x1 - D.w(val), y + 3, 0xFFFFFFFF, false);
        float ty = y + 15, f = (v - min) / (max - min);
        D.round(x0, ty, x1, ty + 3, 1.5f, 0x30FFFFFF); D.round(x0, ty, x0 + (x1 - x0) * f, ty + 3, 1.5f, V.a1(), V.a2()); D.circle(x0 + (x1 - x0) * f, ty + 1.5f, 3.8f, 0xFFFFFFFF);
        hit(x0 - 3, ty - 5, x1 + 3, ty + 8, "ts", () -> { if (which == 0) themeDragBg = true; else themeDragScale = true; dragX0 = x0; dragW = x1 - x0; themeDrag(lastMx); });
        return y + 26;
    }
    private static void themeDrag(float mx) {
        float f = Math.max(0, Math.min(1, ((mx - ox) / sw - dragX0) / dragW));
        if (themeDragBg) V.bg = Math.round((0.15f + f * 0.8f) * 20) / 20f;
        if (themeDragScale) V.scale = Math.round((0.5f + f * 1.5f) * 20) / 20f;
        V.changed();
    }

    // ------------------------------------------------------------------ HUD editor
    private static void editor(Gfx g, Game game, float mx, float my) {
        int W0 = screenW, H0 = screenH;
        D.rect(0, 0, W0, H0, 0x33000000);
        // guides
        D.rect(W0 / 2f - 0.25f, 0, W0 / 2f + 0.25f, H0, 0x22FFFFFF); D.rect(0, H0 / 2f - 0.25f, W0, H0 / 2f + 0.25f, 0x22FFFFFF);
        for (Hud.Box b : Hud.BOXES) {
            boolean hov = in(mx, my, b.x, b.y, b.x + b.w, b.y + b.h) || moving == b;
            int c = hov ? V.a1() : 0x88FFFFFF;
            D.rect(b.x - 1, b.y - 1, b.x + b.w + 1, b.y, c); D.rect(b.x - 1, b.y + b.h, b.x + b.w + 1, b.y + b.h + 1, c);
            D.rect(b.x - 1, b.y, b.x, b.y + b.h, c); D.rect(b.x + b.w, b.y, b.x + b.w + 1, b.y + b.h, c);
            if (hov) { String n = b.mod.name + (b.mod.on ? "" : " (выкл.)"); D.round(b.x, b.y - 12, b.x + D.w(n) * 0.75f + 8, b.y - 2, 3, V.a1()); D.textS(n, b.x + 4, b.y - 10, 0.75f, 0xFFFFFFFF); }
        }
        String hint = "Перетаскивайте элементы  ·  ПКМ — вкл/выкл  ·  Esc — готово";
        float w = D.w(hint) + 24; D.panel((W0 - w) / 2, H0 - 40, (W0 + w) / 2, H0 - 18, 6); D.textC(hint, W0 / 2f, H0 - 33, 0xFFFFFFFF);
        float rx = (W0 - 120) / 2f, ry = 10; boolean rh = in(mx, my, rx, ry, rx + 120, ry + 18);
        D.round(rx, ry, rx + 120, ry + 18, 6, rh ? 0x50FFFFFF : 0x30FFFFFF); D.textC("Сбросить расположение", W0 / 2f, ry + 5, 0xFFFFFFFF);
        hit(rx, ry, rx + 120, ry + 18, "reset", () -> { for (V.Mod m : V.MODS) if (m.hud != null) m.hud = m.defHud.clone(); V.changed(); });
    }

    // ================================================================== input
    public static void click(float mx, float my, int button) {
        lastMx = mx; lastMy = my;
        if (listening != null && button > 0) { listening.bind = -(button + 1); listening = null; V.changed(); return; } // right / middle / side mouse buttons as binds
        textEdit = null;
        if (Hud.editing) {
            for (Hit h : HITS) if (in(mx, my, h.x1, h.y1, h.x2, h.y2)) { h.click.run(); return; }
            for (int i = Hud.BOXES.size() - 1; i >= 0; i--) { Hud.Box b = Hud.BOXES.get(i);
                if (in(mx, my, b.x, b.y, b.x + b.w, b.y + b.h)) {
                    if (button == 1) { b.mod.on = !b.mod.on; V.changed(); return; }
                    moving = b; moveDX = mx - b.x; moveDY = my - b.y; return; } }
            return;
        }
        float lx = (mx - ox) / sw, ly = (my - oy) / sw;
        if (!in(lx, ly, 0, 0, W, H)) return;
        boolean wasSearch = searchFocus; searchFocus = false;
        for (int i = HITS.size() - 1; i >= 0; i--) { Hit h = HITS.get(i); // last drawn = on top
            boolean listArea = !(h.id.equals("cat") || h.id.equals("hud") || h.id.equals("search") || h.id.startsWith("t") || h.id.equals("pre"));
            if (listArea && (ly < 44 || ly > H - 6)) continue; // scrolled out of view
            if (in(lx, ly, h.x1, h.y1, h.x2, h.y2)) {
                if (button == 1 && h.id.equals("card")) { expandFor(lx, ly); return; }
                if (h.id.equals("card") && listening != null) { listening = null; return; }
                h.click.run(); return; } }
        if (wasSearch) searchFocus = false;
    }
    private static void expandFor(float lx, float ly) { for (Hit h : HITS) if (h.id.equals("exp") && Math.abs(h.y1 - ly) < 20) { h.click.run(); return; } }
    public static void drag(float mx, float my) {
        lastMx = mx; lastMy = my;
        if (moving != null) {
            float x = mx - moveDX, y = my - moveDY, w = moving.w, h = moving.h;
            // snap to centre / edges
            boolean snapX = Math.abs(x + w / 2 - screenW / 2f) < 4, snapY = Math.abs(y + h / 2 - screenH / 2f) < 4; // any element can sit exactly in the middle
            if (snapX) x = screenW / 2f - w / 2; if (snapY) y = screenH / 2f - h / 2; if (x < 4) x = 1; if (x + w > screenW - 4) x = screenW - w - 1;
            if (y < 4) y = 1; if (y + h > screenH - 4) y = screenH - h - 1;
            moving.x = x; moving.y = y;
            boolean centred = Hud.centred(moving.mod.id);
            moving.mod.hud[0] = snapX ? 0.5f : centred ? (x + w / 2) / screenW : (x + w / 2 > screenW / 2f ? (x + w) / screenW : x / screenW);
            if (!snapX && !centred && moving.mod.hud[0] > 0.5f && x + w / 2 <= screenW / 2f) moving.mod.hud[0] = x / screenW;
            moving.mod.hud[1] = snapY ? 0.5f : y + h / 2 > screenH / 2f ? (y + h) / screenH : y / screenH;
            if (!snapY && moving.mod.hud[1] > 0.5f && y + h / 2 <= screenH / 2f) moving.mod.hud[1] = y / screenH;
            V.changed(); return;
        }
        if (dragging != null) drag(mx);
        if (themeDragBg || themeDragScale) themeDrag(mx);
    }
    private static void drag(float mx) {
        V.S s = dragging; float f = Math.max(0, Math.min(1, ((mx - ox) / sw - dragX0) / dragW));
        double v = s.min + (s.max - s.min) * f; v = Math.round(v / s.step) * s.step; s.num = Math.max(s.min, Math.min(s.max, v)); V.changed();
    }
    public static void release() { dragging = null; moving = null; themeDragBg = themeDragScale = false; }
    public static void scroll(double amount) { if (!Hud.editing) scrollTarget -= (float) amount * 28; }
    /** @return true if the key was used (Esc with nothing to cancel returns false -> the screen closes) */
    public static boolean key(int code) {
        if (listening != null) { listening.bind = code == 256 || code == 259 ? 0 : code; listening = null; V.changed(); return true; }
        if (textEdit != null) {
            if (code == 259 && !textEdit.text.isEmpty()) { textEdit.text = textEdit.text.substring(0, textEdit.text.length() - 1); V.changed(); }
            if (code == 256 || code == 257) textEdit = null;
            return true;
        }
        if (Hud.editing) { if (code == 256) { Hud.editing = false; return true; } return false; }
        if (searchFocus) {
            if (code == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); return true; }
            if (code == 256 || code == 257) { searchFocus = false; if (code == 256) search = ""; return true; }
            return code != 256;
        }
        return false;
    }
    public static void ch(char c) { if (textEdit != null) { if (c >= ' ' && textEdit.text.length() < 100) { textEdit.text += c; V.changed(); } return; } if (searchFocus && c >= ' ' && search.length() < 24) { search += c; scrollTarget = 0; } }

    // ------------------------------------------------------------------ util
    private static void hit(float x1, float y1, float x2, float y2, String id, Runnable r) { HITS.add(new Hit(x1, y1, x2, y2, id, r)); }
    static boolean in(float x, float y, float x1, float y1, float x2, float y2) { return x >= x1 && x < x2 && y >= y1 && y < y2; }
    private static String fmt(double v, double step) { return step >= 1 ? String.valueOf((int) Math.round(v)) : step >= 0.1 ? String.format("%.1f", v) : String.format("%.2f", v); }
}
