package ru.safimine.asteria.visuals;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D cosmetics worn by the player: wings, hats and an item on the back. Every piece is built once from many small
 * oriented boxes (feathers, bones, gems, wraps) with a colour gradient along each box, then drawn every frame with the
 * owner's yaw, the world light and a little animation. Other Asteria players' choices come with their pets (Fx.FX_OF).
 */
public final class Cosmetics {
    private Cosmetics() {}
    // the own model keeps its old place in every list (wings 5, hat 6, back 4); new pieces come after it, so older mods of other players stay correct
    public static final String[] WINGS = {"Нет", "Ангел", "Демон", "Фея", "Дракон", "Своя модель", "Кристальные", "Бабочка"},
        HATS = {"Нет", "Корона", "Цилиндр", "Кошачьи ушки", "Шляпа ведьмы", "Рога", "Своя модель", "Ледяная тиара", "Цветочный венок"},
        BACK = {"Нет", "Коса", "Молот", "Катана", "Своя модель", "Небесный клинок", "Посох звёзд", "Лук ветра", "Нефритовое копьё"};
    static final int OWN_W = 5, OWN_H = 6, OWN_B = 4;

    /** an oriented box: centre, length axis u, width axis v (thickness along u x v), sizes, colour at the u- end and at the u+ end, part (0 still, 1 left wing, 2 right wing) */
    static final class Ob { float cx, cy, cz, ux, uy, uz, vx, vy, vz, lu, lv, lw; int c0, c1, part; boolean tri; }
    @SuppressWarnings("unchecked") private static final List<Ob>[] CACHE_W = new List[WINGS.length], CACHE_H = new List[HATS.length], CACHE_B = new List[BACK.length];

    private static Ob ob(List<Ob> l, double cx, double cy, double cz, double ux, double uy, double uz, double vx, double vy, double vz, double lu, double lv, double lw, int c0, int c1, int part) {
        double n = Math.sqrt(ux * ux + uy * uy + uz * uz); ux /= n; uy /= n; uz /= n;
        double d = vx * ux + vy * uy + vz * uz; vx -= d * ux; vy -= d * uy; vz -= d * uz; double m = Math.sqrt(vx * vx + vy * vy + vz * vz); if (m < 1e-6) { vx = 1; vy = 0; vz = 0; m = 1; } vx /= m; vy /= m; vz /= m;
        Ob o = new Ob(); o.cx = (float) cx; o.cy = (float) cy; o.cz = (float) cz; o.ux = (float) ux; o.uy = (float) uy; o.uz = (float) uz; o.vx = (float) vx; o.vy = (float) vy; o.vz = (float) vz;
        o.lu = (float) lu; o.lv = (float) lv; o.lw = (float) lw; o.c0 = c0; o.c1 = c1; o.part = part; l.add(o); return o;
    }
    /** a box from point a to point b */
    private static Ob seg(List<Ob> l, double[] a, double[] b, double w, double t, double[] side, int c0, int c1, int part) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2], len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return ob(l, (a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2, dx, dy, dz, side[0], side[1], side[2], len, w, t, c0, c1, part);
    }
    /** a triangle (membranes): drawn as a flat box with one collapsed end */
    private static void tri(List<Ob> l, double[] a, double[] b, double[] c, int ca, int cbc, int part) {
        Ob o = new Ob(); o.tri = true; o.part = part; o.c0 = ca; o.c1 = cbc;
        o.cx = (float) a[0]; o.cy = (float) a[1]; o.cz = (float) a[2]; o.ux = (float) b[0]; o.uy = (float) b[1]; o.uz = (float) b[2]; o.vx = (float) c[0]; o.vy = (float) c[1]; o.vz = (float) c[2]; l.add(o);
    }
    private static int mix(int a, int b, double t) { return D.mix(a, b, (float) Math.max(0, Math.min(1, t))); }
    private static double[] p(double x, double y, double z) { return new double[]{x, y, z}; }
    private static double[] add(double[] a, double[] d, double k) { return new double[]{a[0] + d[0] * k, a[1] + d[1] * k, a[2] + d[2] * k}; }
    private static double[] dirXY(double deg, double z) { double r = Math.toRadians(deg); return new double[]{-Math.cos(r), Math.sin(r), z}; } // pointing outward (-x) for the left wing

    // ================================================================ wings (built for the left wing in wing space: root at 0, outward = -x; the right wing is mirrored)
    private static List<Ob> wings(int kind) {
        List<Ob> l = new ArrayList<>(); double[] side = {0, 0, 1};
        if (kind == 1) { // angel: a soft leading edge, ~14 long primaries, 12 secondaries and three rows of coverts on top
            double[] root = p(0, 0, 0), wrist = p(-9, 4.5, -0.6), tip = p(-16, 6.5, -1.2);
            seg(l, root, wrist, 2.6, 1.8, side, 0xFFFFFFFF, 0xFFF6F9FF, 0); seg(l, wrist, tip, 2.0, 1.4, side, 0xFFF6F9FF, 0xFFEEF4FF, 0);
            for (int j = 0; j < 14; j++) { double f = j / 13.0; double[] b0 = add(wrist, new double[]{tip[0] - wrist[0], tip[1] - wrist[1], tip[2] - wrist[2]}, Math.min(1, (1 - f) * 1.2));
                double ang = Math.toRadians(8 + 78 * f); double[] d = {-Math.cos(ang), -Math.sin(ang), -0.03}; double len = 17 - 5 * Math.abs(f - 0.25), w = 3.6 - 1.0 * f;
                double[] st = add(b0, new double[]{0, 0, -0.12 * j}, 1); seg(l, st, add(st, d, len), w, 0.4, side, 0xFFFFFFFF, j % 2 == 0 ? 0xFFDCE8FF : 0xFFCFE0FF, 0);
                seg(l, add(st, new double[]{0, 0, 0.21}, 1), add(add(st, new double[]{0, 0, 0.21}, 1), d, len * 0.92), 0.25, 0.1, side, 0xFFE8EEF6, 0xFFB8C8DC, 0); } // quill
            for (int j = 0; j < 12; j++) { double f = j / 11.0; double[] b0 = add(root, new double[]{wrist[0], wrist[1], wrist[2]}, f), d = {-0.12 - 0.3 * f, -1, -0.02};
                double[] st = add(b0, new double[]{0, 0, 0.25}, 1); seg(l, st, add(st, d, 11 + 3.5 * f), 3.8, 0.4, side, 0xFFFAFCFF, j % 2 == 0 ? 0xFFE2ECFF : 0xFFD8E5FF, 0); }
            for (int r = 0; r < 3; r++) { int cnt = 11 - r * 3; for (int j = 0; j < cnt; j++) { double f = j / (double) (cnt - 1); double[] b0 = add(root, new double[]{wrist[0] - (r == 0 ? 3 : 0), wrist[1] + (r == 0 ? 1 : 0), wrist[2]}, f), d = {-0.35, -1, 0.02};
                double[] st = add(b0, new double[]{0, -0.4 - 0.2 * r, 0.6 + 0.3 * r}, 1); seg(l, st, add(st, d, 7 - 1.8 * r), 3.3 - 0.4 * r, 0.38, side, 0xFFFFFFFF, mix(0xFFFFFFFF, 0xFFE4EEFF, 0.5 + 0.2 * r), 0); } }
        } else if (kind == 2 || kind == 4) { // demon / dragon: arm bone, four finger bones, a membrane between them with a scalloped edge
            boolean dr = kind == 4; int bone = dr ? 0xFF1F5A3A : 0xFF2A1416, boneTip = dr ? 0xFF6FCB8A : 0xFF5A2A2E, memA = dr ? 0xE02F8F5A : 0xE0801828, memB = dr ? 0xB0103020 : 0xB0200608;
            double[] root = p(0, 0, 0), wrist = p(-8, 6, -0.8);
            seg(l, root, wrist, 1.4, 1.4, side, bone, boneTip, 0);
            seg(l, wrist, add(wrist, new double[]{0.2, 1, 0}, 2.2), 0.8, 0.8, side, boneTip, 0xFFE8E0D0, 0); // claw
            double[][] fingers = {{-6, 2, 15}, {-28, 1.5, 16}, {-52, 1.2, 14}, {-78, 1, 11}}; double[][] tips = new double[4][];
            for (int i = 0; i < 4; i++) { double a = Math.toRadians(fingers[i][0]); double[] d = {-Math.cos(a), Math.sin(a) * (a < 0 ? 1 : 1), -0.05}; d[1] = Math.sin(a);
                double[] mid = add(wrist, d, fingers[i][2] * 0.55); tips[i] = add(wrist, d, fingers[i][2]);
                seg(l, wrist, mid, fingers[i][1] * 0.8, fingers[i][1] * 0.8, side, bone, mix(bone, boneTip, 0.5), 0); seg(l, mid, tips[i], fingers[i][1] * 0.5, fingers[i][1] * 0.5, side, mix(bone, boneTip, 0.5), boneTip, 0);
                if (dr) for (int k = 1; k < 4; k++) { double[] s = add(wrist, d, fingers[i][2] * k / 4.0); seg(l, s, add(s, new double[]{0, 1, 0.3}, 1.2), 0.5, 0.5, side, boneTip, 0xFFE0F5C0, 0); } } // dragon spikes
            double[] prev = root;
            for (int i = 3; i >= 0; i--) { double[] inner = add(add(tips[i], wrist, 1), new double[]{0, 0, 0}, 0); double[] mid = { (tips[i][0] + (i < 3 ? tips[i + 1][0] : prev[0])) / 2 * 0.86 + wrist[0] * 0.14, (tips[i][1] + (i < 3 ? tips[i + 1][1] : prev[1])) / 2 * 0.86 + wrist[1] * 0.14, -0.6 };
                double[] other = i < 3 ? tips[i + 1] : prev;
                tri(l, wrist, other, mid, memA, memB, 0); tri(l, wrist, mid, tips[i], memA, memB, 0); }
            tri(l, root, wrist, tips[3], memA, memB, 0);
        } else if (kind == 3) { // fairy: two rounded panes per side with bright veins
            for (int w = 0; w < 2; w++) { double rx = w == 0 ? 7.5 : 5, ry = w == 0 ? 10 : 6, ang = w == 0 ? 30 : -40, cx = -rx * 0.9, cy = w == 0 ? ry * 0.6 : -ry * 0.5; int seg = 22;
                double ca = Math.cos(Math.toRadians(ang)), sa = Math.sin(Math.toRadians(ang)); double[] centre = p(cx * ca - cy * sa * 0.3, cy, -0.4);
                for (int i = 0; i < seg; i++) { double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                    double[] q0 = p(centre[0] + Math.cos(a0) * rx * ca - Math.sin(a0) * ry * sa * 0.3, centre[1] + Math.sin(a0) * ry, -0.4), q1 = p(centre[0] + Math.cos(a1) * rx * ca - Math.sin(a1) * ry * sa * 0.3, centre[1] + Math.sin(a1) * ry, -0.4);
                    tri(l, centre, q0, q1, (0x60 << 24) | 0xFEFE01, (0xC8 << 24) | 0xFEFE02, 0); }
                for (int k = 0; k < 4; k++) { double a = Math.PI * 2 * k / 4 + 0.4; seg(l, centre, p(centre[0] + Math.cos(a) * rx * 0.85, centre[1] + Math.sin(a) * ry * 0.85, -0.35), 0.3, 0.2, side, 0xF0FFFFFF, 0x60FFFFFF, 0); }
                tri(l, p(0, 0, -0.4), centre, p(centre[0] * 0.3, centre[1] + 1.5, -0.4), (0x90 << 24) | 0xFEFE01, (0x90 << 24) | 0xFEFE02, 0); }
        } else if (kind == 6) { // crystal: a fan of faceted shards, each tapering to a point with a glowing core and a bright upper edge, plus small floating splinters
            double[][] sh = {{18, 17, 2.6}, {4, 15, 2.4}, {-12, 14, 2.3}, {-28, 12, 2.1}, {-44, 10, 1.9}, {-60, 8, 1.6}, {34, 11, 1.9}, {50, 7, 1.4}};
            for (int i = 0; i < sh.length; i++) { double a = Math.toRadians(sh[i][0]); double[] d = {-Math.cos(a), Math.sin(a), -0.04 * i}, root = p(-1.2, 1.5, -0.2 * i), tip = add(root, d, sh[i][1]), mid = add(root, d, sh[i][1] * 0.62);
                double w = sh[i][2]; int c0 = (0xE8 << 24) | 0xFEFE01, c1 = (0xA0 << 24) | 0xFEFE02;
                seg(l, root, mid, w, w * 0.55, side, c0, c1, 0); seg(l, mid, tip, w * 0.55, w * 0.3, side, c1, (0x70 << 24) | 0xFEFE02, 0);
                seg(l, add(root, new double[]{0, 0, 0.35}, 1), add(tip, new double[]{0, 0, 0.35}, 1), 0.28, 0.12, side, GLOW, GLOW, 0);
                double[] n = {-d[1], d[0], 0}; seg(l, add(root, n, w * 0.5), add(mid, n, w * 0.3), 0.18, 0.2, side, 0xE0FFFFFF, 0x80FFFFFF, 0);
                for (int k = 0; k < 2; k++) { double[] sp = add(add(root, d, sh[i][1] * (0.35 + 0.3 * k)), n, -(w + 1.2 + k)); seg(l, sp, add(sp, d, 1.4), 0.7, 0.4, side, (0xC0 << 24) | 0xFEFE02, (0x60 << 24) | 0xFEFE01, 0); } }
            seg(l, p(0.4, 0, 0), p(-2.2, 2.4, -0.4), 2.2, 1.4, side, 0xFFE8F4FF, (0xE0 << 24) | 0xFEFE01, 0);
        } else if (kind == 7) { // butterfly: a big forewing and a rounded hindwing, a dark rim, veins and white spots; colour from the cosmetics colour
            for (int w = 0; w < 2; w++) { double ry = w == 0 ? 15 : 9.5, rx = w == 0 ? 12 : 8; int n = w == 0 ? 18 : 14; double[] c = p(-rx * 0.62, w == 0 ? ry * 0.42 : -ry * 0.42, -0.35);
                double[][] rim = new double[n + 1][];
                for (int i = 0; i <= n; i++) { double a = (w == 0 ? -0.2 : Math.PI + 0.2) + (w == 0 ? 1 : -1) * Math.PI * 1.15 * i / n, rr = 1 + 0.12 * Math.sin(i * 1.7);
                    rim[i] = p(c[0] + Math.cos(a + Math.PI * 0.5) * rx * rr * 0.62, c[1] + Math.sin(a + Math.PI * 0.5) * ry * rr * 0.55, -0.35); }
                for (int i = 0; i < n; i++) { tri(l, p(0, 0, -0.3), rim[i], rim[i + 1], (0xF0 << 24) | 0xFEFE01, (0xD8 << 24) | 0xFEFE02, 0);
                    seg(l, rim[i], rim[i + 1], 0.75, 0.25, side, 0xFF1C1626, 0xFF2A2236, 0); }
                for (int i = 1; i < n; i += 3) { seg(l, p(0, 0, -0.25), add(rim[i], new double[]{-rim[i][0] * 0.12, -rim[i][1] * 0.12, 0}, 1), 0.18, 0.12, side, 0xD01C1626, 0x601C1626, 0);
                    double[] sp = add(rim[i], new double[]{-rim[i][0] * 0.16, -rim[i][1] * 0.16, 0.1}, 1); ob(l, sp[0], sp[1], -0.2, 0, 1, 0, 1, 0, 0, 0.7, 0.7, 0.12, 0xFFFFFFFF, 0xFFF4F0FF, 0); } }
            seg(l, p(0.3, -3, -0.2), p(0.3, 3, -0.2), 0.9, 0.9, side, 0xFF2A2236, 0xFF3A3048, 0);
        }
        return l;
    }

    // ================================================================ hats (head space: head is x,z -4..4, y 0..8, origin at the neck)
    private static List<Ob> hat(int kind) {
        List<Ob> l = new ArrayList<>(); double[] up = {0, 1, 0};
        if (kind == 1) { // crown: gold band, eight points with gems, red velvet
            int gold = 0xFFF6C744, goldD = 0xFFB07A12; int[] gems = {0xFFE0245E, 0xFF2F80ED, 0xFF27AE60, 0xFF9B51E0};
            for (int i = 0; i < 24; i++) { double a = Math.PI * 2 * i / 24, r = 4.5; double[] t = {-Math.sin(a), 0, Math.cos(a)};
                ob(l, Math.cos(a) * r, 9.2, Math.sin(a) * r, 0, 1, 0, t[0], 0, t[2], 2.4, 1.26, 0.55, goldD, gold, 0); }
            for (int i = 0; i < 8; i++) { double a = Math.PI * 2 * i / 8 + Math.PI / 8, r = 4.5; double x = Math.cos(a) * r, z = Math.sin(a) * r; double[] t = {-Math.sin(a), 0, Math.cos(a)};
                ob(l, x, 11.2, z, 0, 1, 0, t[0], 0, t[2], 1.8, 1.3, 0.5, gold, gold, 0); ob(l, x, 12.6, z, 0, 1, 0, t[0], 0, t[2], 1.2, 0.7, 0.45, gold, 0xFFFFE8A0, 0);
                ob(l, x * 1.03, 13.4, z * 1.03, 0, 1, 0, t[0], 0, t[2], 0.7, 0.7, 0.7, 0xFFFFFFFF, 0xFFFFFFFF, 0); // pearl
                ob(l, x * 1.04, 9.2, z * 1.04, 0, 1, 0, t[0], 0, t[2], 0.9, 0.9, 0.35, gems[i % 4], mix(gems[i % 4], 0xFFFFFFFF, 0.5), 0); }
            ob(l, 0, 10.4, 0, 0, 1, 0, 1, 0, 0, 0.5, 8.2, 8.2, 0xFFB01535, 0xFFD02040, 0);
            ob(l, 0, 11.6, 0, 0, 1, 0, 1, 0, 0, 2.4, 0.5, 0.5, gold, 0xFFFFF0B0, 0); ob(l, 0, 12.2, 0, 1, 0, 0, 0, 1, 0, 1.8, 0.5, 0.5, gold, gold, 0); // little cross on top
        } else if (kind == 2) { // top hat: brim, tall satin body, ribbon
            int blk = 0xFF16161C, sat = 0xFF34343E;
            for (int i = 0; i < 28; i++) { double a = Math.PI * 2 * i / 28; double[] t = {-Math.sin(a), 0, Math.cos(a)};
                ob(l, Math.cos(a) * 5.4, 8.25, Math.sin(a) * 5.4, Math.cos(a), 0, Math.sin(a), t[0], 0, t[2], 3.4, 1.3, 0.5, blk, sat, 0);
                ob(l, Math.cos(a) * 3.7, 12.6, Math.sin(a) * 3.7, 0, 1, 0, t[0], 0, t[2], 8.4, 0.88, 0.5, blk, sat, 0);
                ob(l, Math.cos(a) * 3.82, 9.4, Math.sin(a) * 3.82, 0, 1, 0, t[0], 0, t[2], 1.6, 0.9, 0.3, 0xFFA3122A, 0xFFD02040, 0); }
            ob(l, 0, 16.75, 0, 0, 1, 0, 1, 0, 0, 0.4, 7.2, 7.2, sat, sat, 0); ob(l, 3.9, 9.4, 1.2, 0, 1, 0, 0, 0, 1, 1.4, 1.4, 0.4, 0xFFF6C744, 0xFFFFE8A0, 0); // buckle
        } else if (kind == 3) { // cat ears: layered pyramids with pink inner ear and fluffy tips (fur takes the cosmetics colour)
            for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 6; k++) { double f = k / 5.0, w = 3.4 * (1 - f * 0.85), h = 0.8;
                ob(l, s * (2.4 + f * 0.6), 8.4 + k * h, 0.4 - f * 0.3, 0, 1, 0, 1, 0, 0, h, w, 1.6 * (1 - f * 0.6), ACC, ACC2, 0);
                if (k < 4) ob(l, s * (2.4 + f * 0.6), 8.4 + k * h, 1.2 - f * 0.3, 0, 1, 0, 1, 0, 0, h, w * 0.55, 0.2, 0xFFFFA6BF, 0xFFFFC2D2, 0); }
        } else if (kind == 4) { // witch hat: wide drooping brim, bent cone, gold band and buckle
            int pur = 0xFF3B1E5E, pur2 = 0xFF6A3BA0;
            for (int i = 0; i < 32; i++) { double a = Math.PI * 2 * i / 32; double[] t = {-Math.sin(a), 0, Math.cos(a)};
                for (int r = 0; r < 3; r++) { double rad = 4.6 + r * 1.6, y = 8.3 - r * r * 0.25; ob(l, Math.cos(a) * rad, y, Math.sin(a) * rad, Math.cos(a), -0.15 * r, Math.sin(a), t[0], 0, t[2], 1.7, 2.6 * rad / 8, 0.45, pur, pur2, 0); } }
            for (int k = 0; k < 9; k++) { double f = k / 8.0, rad = 4.2 * (1 - f) + 0.4, y = 9 + k * 1.5, bz = -f * f * 4; int n = 16;
                for (int i = 0; i < n; i++) { double a = Math.PI * 2 * i / n; double[] t = {-Math.sin(a), 0, Math.cos(a)};
                    ob(l, Math.cos(a) * rad, y, Math.sin(a) * rad + bz, 0.0, 1, -f * 0.6, t[0], 0, t[2], 1.7, rad * 2 * Math.PI / n + 0.15, 0.45, mix(pur, pur2, f * 0.5), mix(pur, pur2, f), 0); } }
            for (int i = 0; i < 18; i++) { double a = Math.PI * 2 * i / 18; double[] t = {-Math.sin(a), 0, Math.cos(a)}; ob(l, Math.cos(a) * 4.35, 9.6, Math.sin(a) * 4.35, 0, 1, 0, t[0], 0, t[2], 1.2, 1.6, 0.3, 0xFFD9A520, 0xFFFFD86B, 0); }
            ob(l, 0, 9.6, 4.6, 0, 1, 0, 1, 0, 0, 1.8, 1.8, 0.4, 0xFFFFD86B, 0xFFFFF2B0, 0); ob(l, 0, 9.6, 4.75, 0, 1, 0, 1, 0, 0, 0.9, 0.9, 0.3, pur, pur, 0);
        } else if (kind == 5) { // horns: curved segments, dark at the root, ivory at the tip
            for (int s = -1; s <= 1; s += 2) { double[] pnt = p(s * 3.2, 7.2, 1.2); double ang = 0; double th = 1.9;
                for (int k = 0; k < 10; k++) { ang += 0.28; double[] d = {s * 0.35 * Math.cos(ang * 0.5), Math.cos(ang), -Math.sin(ang)}; double len = 1.25;
                    double[] nx = add(pnt, d, len); seg(l, pnt, nx, th, th, new double[]{1, 0, 0}, mix(0xFF2B1B14, 0xFFE8DCC0, k / 10.0), mix(0xFF2B1B14, 0xFFF4ECD8, (k + 1) / 10.0), 0); pnt = nx; th *= 0.84; } }
        } else if (kind == 7) { // ice tiara: a thin silver band, tall crystal spikes in the front getting smaller to the sides, a gem in the middle
            for (int i = 0; i < 30; i++) { double a = Math.PI * 2 * i / 30; double[] t = {-Math.sin(a), 0, Math.cos(a)}; ob(l, Math.cos(a) * 4.4, 8.7, Math.sin(a) * 4.4, 0, 1, 0, t[0], 0, t[2], 0.7, 1.0, 0.35, 0xFFB8C6D8, 0xFFF2F8FF, 0); }
            for (int i = -4; i <= 4; i++) { double a = Math.PI / 2 + i * 0.27, x = Math.cos(a) * 4.5, z = Math.sin(a) * 4.5, hgt = 5.2 - Math.abs(i) * 1.05; double[] t = {-Math.sin(a), 0, Math.cos(a)};
                ob(l, x, 9 + hgt / 2, z, 0.0, 1, 0.12, t[0], 0, t[2], hgt, 1.1 - Math.abs(i) * 0.08, 0.5, 0xE0BFE8FF, 0xF0FFFFFF, 0);
                ob(l, x * 1.01, 9 + hgt * 0.45, z * 1.01, 0.0, 1, 0.12, t[0], 0, t[2], hgt * 0.7, 0.3, 0.55, GLOW, GLOW, 0);
                if (i % 2 == 0) ob(l, x, 9.3 + hgt, z, 0, 1, 0, t[0], 0, t[2], 0.6, 0.6, 0.6, 0xFFFFFFFF, 0xFFFFFFFF, 0); }
            ob(l, 0, 9.5, 4.75, 0, 1, 0, 1, 0, 0, 1.6, 1.3, 0.5, ACC, ACC2, 0); ob(l, 0, 9.5, 4.95, 0, 1, 0, 1, 0, 0, 0.7, 0.6, 0.3, 0xFFFFFFFF, 0xFFFFFFFF, 0);
        } else if (kind == 8) { // flower wreath: green leaves around the head with roses, daisies and violets
            for (int i = 0; i < 36; i++) { double a = Math.PI * 2 * i / 36, r = 4.6; double[] t = {-Math.sin(a), 0, Math.cos(a)}; double tilt = i % 2 == 0 ? 0.5 : -0.5;
                ob(l, Math.cos(a) * r, 8.9 + (i % 2) * 0.3, Math.sin(a) * r, t[0], tilt, t[2], 0, 1, 0, 2.2, 0.9, 0.3, 0xFF3C8A3E, 0xFF7CC95A, 0); }
            int[][] fl = {{0xFFFF7AA2, 0xFFFFC2D4, 0xFFFFE066}, {0xFFFFFFFF, 0xFFF4F0FF, 0xFFFFC93C}, {0xFFB48CFF, 0xFFE2D4FF, 0xFFFFE066}, {0xFFFF9F43, 0xFFFFD29A, 0xFFFFF3B0}};
            for (int i = 0; i < 9; i++) { double a = Math.PI * 2 * i / 9 + 0.2, r = 4.75, x = Math.cos(a) * r, z = Math.sin(a) * r; int[] c = fl[i % 4]; double[] o = {Math.cos(a), 0.25, Math.sin(a)}, t = {-Math.sin(a), 0, Math.cos(a)};
                for (int k = 0; k < 5; k++) { double pa = Math.PI * 2 * k / 5 + i, dx = Math.cos(pa) * 0.75, dy = Math.sin(pa) * 0.75;
                    ob(l, x + t[0] * dx, 9.4 + dy, z + t[2] * dx, o[0], o[1], o[2], t[0] * Math.cos(pa), Math.sin(pa), t[2] * Math.cos(pa), 0.3, 1.05, 0.8, c[0], c[1], 0); }
                ob(l, x * 1.04, 9.4, z * 1.04, o[0], o[1], o[2], t[0], 0, t[2], 0.45, 0.6, 0.6, c[2], 0xFFFFFFFF, 0); }
        }
        return l;
    }

    // ================================================================ back items (body space: origin at the middle of the back, up = +y, outward = -z)
    private static List<Ob> back(int kind) {
        List<Ob> l = new ArrayList<>(); double[] side = {1, 0, 0}, up = {0, 1, 0};
        if (kind == 1) { // scythe: dark shaft with leather wraps, gold collars and glowing runes; a long crescent blade with a gold spine, a bright edge and a back spike
            seg(l, p(0, -14, 0), p(0, 14, 0), 1.0, 1.0, side, 0xFF1E1620, 0xFF3A2A3E, 0);
            for (int k = 0; k < 10; k++) ob(l, 0, -12 + k * 1.1, 0, 0, 1, k % 2 == 0 ? 0.35 : -0.35, 1, 0, 0, 0.8, 1.2, 1.2, 0xFF3A2416, 0xFF5E3B22, 0);
            for (double y : new double[]{-12.8, -0.8, 6.5, 12.6}) { ob(l, 0, y, 0, 0, 1, 0, 1, 0, 0, 0.7, 1.45, 1.45, 0xFFB8892A, 0xFFFFE08A, 0); ob(l, 0, y, 0, 0, 1, 0, 1, 0, 0, 0.25, 1.6, 1.6, 0xFFFFF2C0, 0xFFFFF2C0, 0); }
            for (int k = 0; k < 4; k++) ob(l, 0, 1.2 + k * 1.3, -0.52, 0, 1, 0, 1, 0, 0, 0.7, 0.45, 0.12, GLOW, GLOW, 0);
            seg(l, p(0, -14, 0), p(0, -16.5, 0), 0.7, 0.7, side, 0xFFB8892A, 0xFFFFE08A, 0); ob(l, 0, -17, 0, 0, 1, 0, 1, 0, 0, 1.1, 1.1, 1.1, ACC, ACC2, 0);
            double[] prev = p(0, 13.5, 0);
            for (int k = 1; k <= 18; k++) { double f = k / 18.0, a = Math.PI * 0.55 * f; double[] nx = p(-Math.sin(a) * 11 - f * 2.5, 13.5 + Math.cos(a) * 3 - f * f * 7.5, 0);
                double w = 3.6 * (1 - f) + 0.3; seg(l, prev, nx, w, 0.42, new double[]{0, 1, 0}, mix(0xFF5C6474, 0xFFC6CFDC, f), mix(0xFF8E98A8, 0xFFE6EEF8, f), 0);
                seg(l, add(prev, new double[]{0, w * 0.48, 0}, 1), add(nx, new double[]{0, w * 0.48, 0}, 1), 0.45, 0.6, new double[]{0, 1, 0}, 0xFFB8892A, 0xFFFFE08A, 0);
                seg(l, add(prev, new double[]{0, -w * 0.46, 0}, 1), add(nx, new double[]{0, -w * 0.46, 0}, 1), 0.3, 0.48, new double[]{0, 1, 0}, GLOW, GLOW, 0);
                if (k % 4 == 2) ob(l, nx[0], nx[1] + w * 0.05, 0.25, 1, 0, 0, 0, 1, 0, 0.5, 0.5, 0.1, ACC2, ACC2, 0);
                prev = nx; }
            seg(l, p(0.2, 14.2, 0), p(3.8, 16.4, 0), 1.6, 0.45, new double[]{0, 1, 0}, 0xFF8E98A8, 0xFFE6EEF8, 0); seg(l, p(3.8, 16.4, 0), p(5, 17, 0), 0.6, 0.4, new double[]{0, 1, 0}, 0xFFE6EEF8, 0xFFFFFFFF, 0);
            ob(l, 0, 13.9, 0, 0, 1, 0, 1, 0, 0, 2.2, 2.2, 2.2, 0xFF8A6418, 0xFFFFD86B, 0); ob(l, 0, 13.9, -1.15, 0, 1, 0, 1, 0, 0, 1.1, 1.1, 0.35, ACC, ACC2, 0); ob(l, 0, 13.9, 1.15, 0, 1, 0, 1, 0, 0, 1.1, 1.1, 0.35, ACC, ACC2, 0);
        } else if (kind == 2) { // war hammer: wrapped grip, heavy head with gold bands and a glowing rune
            seg(l, p(0, -12, 0), p(0, 8, 0), 1.1, 1.1, side, 0xFF4A3020, 0xFF6B4A30, 0);
            for (int k = 0; k < 8; k++) ob(l, 0, -11 + k * 1.2, 0, 0, 1, 0, 1, 0, 0, 0.9, 1.35, 1.35, k % 2 == 0 ? 0xFF2A1A10 : 0xFF5A3A22, k % 2 == 0 ? 0xFF3A2416 : 0xFF6E4A2C, 0);
            ob(l, 0, -12.6, 0, 0, 1, 0, 1, 0, 0, 1.2, 1.7, 1.7, 0xFFC9A030, 0xFFFFE08A, 0);
            ob(l, 0, 10.5, 0, 1, 0, 0, 0, 1, 0, 9, 4.8, 4.8, 0xFF4E5664, 0xFF8C96A6, 0);
            for (int s = -1; s <= 1; s += 2) { ob(l, s * 3.9, 10.5, 0, 1, 0, 0, 0, 1, 0, 0.8, 5.3, 5.3, 0xFFC9A030, 0xFFFFE08A, 0); ob(l, s * 4.8, 10.5, 0, 1, 0, 0, 0, 1, 0, 0.9, 3.6, 3.6, 0xFF6E7684, 0xFFA8B0BE, 0); }
            ob(l, 0, 10.5, -2.45, 1, 0, 0, 0, 1, 0, 5.2, 1.0, 0.2, GLOW, GLOW, 0); ob(l, 0, 10.5, 2.45, 1, 0, 0, 0, 1, 0, 5.2, 1.0, 0.2, GLOW, GLOW, 0);
            seg(l, p(0, 12.9, 0), p(0, 15, 0), 1.2, 1.2, side, 0xFF8C96A6, 0xFFE6ECF4, 0);
        } else if (kind == 3) { // katana in its sheath: black lacquer, gold fittings, round guard, diamond-wrapped handle
            seg(l, p(0, -15, 0), p(0, 6, 0), 1.5, 1.0, side, 0xFF0E0E14, 0xFF2A2A36, 0);
            for (double y : new double[]{-15, -6, 5.5}) ob(l, 0, y, 0, 0, 1, 0, 1, 0, 0, 0.8, 1.7, 1.2, 0xFFC9A030, 0xFFFFE08A, 0);
            for (int i = 0; i < 16; i++) { double a = Math.PI * 2 * i / 16; ob(l, Math.cos(a) * 1.4, 6.6, Math.sin(a) * 1.4, 0, 1, 0, -Math.sin(a), 0, Math.cos(a), 0.45, 0.65, 0.5, 0xFFB08820, 0xFFFFE08A, 0); }
            for (int k = 0; k < 9; k++) { ob(l, 0, 7.4 + k * 0.85, 0, 0, 1, 0, 1, 0, 0, 0.8, 1.15, 0.95, 0xFF1A1A22, 0xFF2A2A36, 0);
                ob(l, 0, 7.4 + k * 0.85, -0.5, 0, 1, (k % 2 == 0 ? 0.4 : -0.4), 1, 0, 0, 0.9, 0.95, 0.2, 0xFFE8E4D8, 0xFFFFFFFF, 0); }
            ob(l, 0, 15.2, 0, 0, 1, 0, 1, 0, 0, 0.9, 1.3, 1.1, 0xFFC9A030, 0xFFFFE08A, 0);
        } else if (kind == 5) { // celestial blade: a long pale blade with a glowing fuller, swept golden guard wings, a wrapped grip, a gem pommel and a tassel
            for (int k = 0; k < 12; k++) { double f = k / 12.0, f1 = (k + 1) / 12.0, y0 = 6.8 - f * 21, y1 = 6.8 - f1 * 21, w0 = 2.9 - f * 1.2, w1 = 2.9 - f1 * 1.2, hw = (w0 + w1) / 4 + 0.05;
                seg(l, p(0, y0, 0), p(0, y1, 0), (w0 + w1) / 2, 0.55, side, mix(0xFFEAF2FF, 0xFFB6CBEA, f), mix(0xFFEAF2FF, 0xFFB6CBEA, f1), 0);
                seg(l, p(-hw, y0, 0), p(-hw, y1, 0), 0.22, 0.3, side, 0xFFFFFFFF, 0xFFE6F0FF, 0); seg(l, p(hw, y0, 0), p(hw, y1, 0), 0.22, 0.3, side, 0xFFFFFFFF, 0xFFE6F0FF, 0);
                seg(l, p(0, y0, 0), p(0, y1, 0), 0.45, 0.62, side, GLOW, GLOW, 0); }
            tri(l, p(-1.75, -14.2, 0.05), p(1.75, -14.2, 0.05), p(0, -17.6, 0.05), 0xFFE0EBFF, 0xFFFFFFFF, 0); tri(l, p(1.75, -14.2, -0.05), p(-1.75, -14.2, -0.05), p(0, -17.6, -0.05), 0xFFE0EBFF, 0xFFFFFFFF, 0);
            ob(l, 0, 7.4, 0, 0, 1, 0, 1, 0, 0, 1.4, 4.2, 1.6, 0xFF9A6C14, 0xFFFFD86B, 0); ob(l, 0, 7.4, -0.85, 0, 1, 0, 1, 0, 0, 1.2, 1.2, 0.3, ACC, ACC2, 0); ob(l, 0, 7.4, 0.85, 0, 1, 0, 1, 0, 0, 1.2, 1.2, 0.3, ACC, ACC2, 0);
            for (int s2 = -1; s2 <= 1; s2 += 2) { double[] pr = p(s2 * 2, 7.4, 0);
                for (int k = 1; k <= 5; k++) { double a = Math.toRadians(10 + k * 16); double[] nx = p(s2 * (2 + Math.cos(a) * k * 0.95), 7.4 + Math.sin(a) * k * 0.7, 0); seg(l, pr, nx, 1.1 - k * 0.14, 0.7, side, 0xFFC9A030, 0xFFFFE8A0, 0); pr = nx; }
                for (int k = 0; k < 3; k++) seg(l, p(s2 * (2.4 + k * 0.9), 6.9, 0), p(s2 * (2.6 + k * 1.2), 5.6 - k * 0.3, 0), 0.4, 0.4, side, 0xFFC9A030, 0xFFFFE8A0, 0); }
            for (int k = 0; k < 6; k++) ob(l, 0, 8.7 + k * 0.85, 0, 0, 1, k % 2 == 0 ? 0.4 : -0.4, 1, 0, 0, 0.8, 1.05, 1.0, 0xFF1C2A4A, 0xFF2E4270, 0);
            ob(l, 0, 13.9, 0, 0, 1, 0, 1, 0, 0, 1.4, 1.5, 1.5, 0xFF9A6C14, 0xFFFFD86B, 0); ob(l, 0, 15, 0, 0, 1, 0, 1, 0, 0, 1.3, 1.3, 1.3, ACC, ACC2, 0);
            double[] tq = p(0, 15.4, 0); for (int k = 1; k <= 4; k++) { double[] nx = p(-0.6 * k, 15.4 - k * 1.3, -0.4 * k); seg(l, tq, nx, 0.5, 0.5, side, ACC, ACC2, 0); tq = nx; }
            ob(l, tq[0], tq[1] - 0.6, tq[2], 0, 1, 0, 1, 0, 0, 1.4, 0.8, 0.8, ACC2, 0xFFFFFFFF, 0);
        } else if (kind == 6) { // star staff: a pale shaft with a gold spiral, a ring at the top holding a glowing star and four small gems around it
            seg(l, p(0, -16, 0), p(0, 9, 0), 0.9, 0.9, side, 0xFFF2EEE6, 0xFFFFFFFF, 0);
            for (int k = 0; k < 20; k++) { double a = k * 0.9, y = -14 + k * 1.15; ob(l, Math.cos(a) * 0.5, y, Math.sin(a) * 0.5, 0, 1, 0, -Math.sin(a), 0, Math.cos(a), 1.0, 0.35, 0.35, 0xFFC9A030, 0xFFFFE08A, 0); }
            ob(l, 0, -16.6, 0, 0, 1, 0, 1, 0, 0, 1.2, 1.2, 1.2, 0xFFC9A030, 0xFFFFE08A, 0);
            int n = 22; double rr = 3.6, cy = 13;
            for (int i = 0; i < n; i++) { double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n; seg(l, p(Math.cos(a0) * rr, cy + Math.sin(a0) * rr, 0), p(Math.cos(a1) * rr, cy + Math.sin(a1) * rr, 0), 0.7, 0.7, new double[]{0, 0, 1}, 0xFFC9A030, 0xFFFFE8A0, 0); }
            seg(l, p(0, 9, 0), p(0, cy - rr, 0), 1.1, 1.1, side, 0xFFC9A030, 0xFFFFE08A, 0);
            for (int k = 0; k < 5; k++) { double a = Math.PI / 2 + Math.PI * 2 * k / 5, a2 = a + Math.PI / 5, a3 = a - Math.PI / 5; double[] tip = p(Math.cos(a) * 2.6, cy + Math.sin(a) * 2.6, 0);
                double[] l2 = p(Math.cos(a2) * 1.1, cy + Math.sin(a2) * 1.1, 0), l3 = p(Math.cos(a3) * 1.1, cy + Math.sin(a3) * 1.1, 0);
                tri(l, p(0, cy, 0.7), l3, tip, ACC2, GLOW, 0); tri(l, p(0, cy, 0.7), tip, l2, ACC2, GLOW, 0); tri(l, p(0, cy, -0.7), tip, l3, ACC2, GLOW, 0); tri(l, p(0, cy, -0.7), l2, tip, ACC2, GLOW, 0); }
            ob(l, 0, cy, 0, 0, 1, 0, 1, 0, 0, 1.2, 1.2, 1.2, 0xFFFFFFFF, 0xFFFFFFFF, 0);
            for (int k = 0; k < 4; k++) { double a = Math.PI * 2 * k / 4 + 0.6; ob(l, Math.cos(a) * 5, cy + Math.sin(a) * 5, 0, 0, 1, 0, 1, 0, 0, 0.8, 0.8, 0.8, ACC, 0xFFFFFFFF, 0); }
        } else if (kind == 7) { // wind bow: two curved limbs with gold bands and tips, feather ornaments, a wrapped grip and a glowing string
            double[] prevU = p(0, 2, 0), prevD = p(0, -2, 0);
            for (int k = 1; k <= 10; k++) { double f = k / 10.0; double[] u = p(-Math.sin(f * 1.2) * 4.2 + f * f * 1.2, 2 + f * 13, 0), d = p(u[0], -2 - f * 13, 0); double w = 1.5 - f * 0.8;
                int c0 = mix(0xFF2E7D6B, 0xFFE8F6F0, f * 0.6), c1 = mix(0xFF2E7D6B, 0xFFE8F6F0, f * 0.6 + 0.06); seg(l, prevU, u, w, w * 0.7, new double[]{1, 0, 0}, c0, c1, 0); seg(l, prevD, d, w, w * 0.7, new double[]{1, 0, 0}, c0, c1, 0);
                if (k == 5) { ob(l, u[0], u[1], 0, 0, 1, 0, 1, 0, 0, 0.6, 1.8, 1.4, 0xFFC9A030, 0xFFFFE08A, 0); ob(l, d[0], d[1], 0, 0, 1, 0, 1, 0, 0, 0.6, 1.8, 1.4, 0xFFC9A030, 0xFFFFE08A, 0); }
                prevU = u; prevD = d; }
            for (double[] tip : new double[][]{prevU, prevD}) { double sg = Math.signum(tip[1]); seg(l, tip, add(tip, new double[]{-1.2, sg, 0}, 1.4), 0.6, 0.6, side, 0xFFC9A030, 0xFFFFE8A0, 0);
                for (int k = 0; k < 3; k++) seg(l, add(tip, new double[]{0, -sg * 2.5, 0}, 1), add(tip, new double[]{1.6 + k * 0.6, -sg * (3 + k * 1.3), 0.3 * k}, 1), 0.9, 0.12, new double[]{0, 0, 1}, 0xFFE8F6F0, mix(0xFF7FD6B8, 0xFFFFFFFF, k * 0.3), 0); }
            seg(l, prevU, prevD, 0.18, 0.18, side, GLOW, GLOW, 0);
            for (int k = 0; k < 5; k++) ob(l, -0.05, -2 + k, 0, 0, 1, k % 2 == 0 ? 0.4 : -0.4, 1, 0, 0, 0.9, 1.6, 1.4, 0xFF3A2416, 0xFF6B4A30, 0);
            ob(l, -0.6, 0.4, 0, 0, 1, 0, 1, 0, 0, 1.0, 0.8, 1.6, ACC, ACC2, 0);
        } else if (kind == 8) { // jade spear: a long dark shaft with gold bands, a gold collar with side hooks, a faceted jade head and a red tassel
            seg(l, p(0, -17, 0), p(0, 10, 0), 0.85, 0.85, side, 0xFF2A1E1A, 0xFF4A3428, 0);
            for (double y : new double[]{-16.5, -6, 4}) ob(l, 0, y, 0, 0, 1, 0, 1, 0, 0, 0.8, 1.15, 1.15, 0xFFC9A030, 0xFFFFE08A, 0);
            ob(l, 0, 10.8, 0, 0, 1, 0, 1, 0, 0, 1.8, 1.8, 1.8, 0xFF9A6C14, 0xFFFFD86B, 0);
            for (int s2 = -1; s2 <= 1; s2 += 2) { seg(l, p(s2 * 0.9, 11.5, 0), p(s2 * 3.2, 12.6, 0), 0.6, 0.5, side, 0xFFC9A030, 0xFFFFE8A0, 0); seg(l, p(s2 * 3.2, 12.6, 0), p(s2 * 3.6, 14.4, 0), 0.5, 0.45, side, 0xFFFFE8A0, 0xFFFFFFFF, 0); }
            double[] b0 = p(0, 11.6, 0), tip = p(0, 21, 0); double[][] rim = {p(-1.8, 14.5, 0), p(0, 14.5, 1.0), p(1.8, 14.5, 0), p(0, 14.5, -1.0)};
            for (int i = 0; i < 4; i++) { double[] a = rim[i], b = rim[(i + 1) % 4]; tri(l, b0, a, b, 0xFF1F8A62, 0xFF3FD69A, 0); tri(l, tip, b, a, 0xFFB8FFE0, 0xFF3FD69A, 0); }
            seg(l, p(0, 12, 0), p(0, 20, 0), 0.3, 2.1, side, GLOW, GLOW, 0);
            double[] tq = p(0, 10, 0); for (int k = 1; k <= 5; k++) { double[] nx = p(-0.4 * k, 10 - k * 1.1, -0.5 * k); seg(l, tq, nx, 1.1 - k * 0.1, 1.1 - k * 0.1, side, 0xFFD02436, 0xFFFF5A6A, 0); tq = nx; }
        }
        return l;
    }

    // ================================================================ drawing
    /** wings / hat / back item of a player. pos = feet, bodyYaw / headYaw in degrees, h = player height, colour tint (-1 = theme), air = flying or jumping */
    /** whose cosmetics are drawn ("" = yours): own models come from that player */
    public static String who = "";
    private static java.io.File custom(String slot) { return who.isEmpty() ? Model.own(slot) : Model.other(who, slot); }
    public static void draw(Fx.R3 r, double[] pos, float bodyYaw, float headYaw, float h, int wing, int hatK, int backK, int tint, boolean air, float light, long now) { draw(r, pos, bodyYaw, headYaw, 0, h < 1.7f, h, wing, hatK, backK, tint, air, light, now); }
    /** lean of the body when sneaking (radians, like the player model), applied around the hips (12 px) */
    private static final double LEAN = 0.5;
    /** sneaking like the player model: the head drops 4.2 px and the body 3.2 px, the whole model 0.125 blocks (2.13 px) more; the body leans around its top (the neck) */
    private static final double HEAD_DROP = 6.33, BODY_DROP = 5.33;
    private static double[] lean(double x, double y, double z, double a, double pivot) { if (a == 0) return new double[]{x, y, z}; double dy = y - pivot, c = Math.cos(a), s = Math.sin(a); return new double[]{x, pivot + dy * c - z * s, z * c + dy * s}; }
    public static void draw(Fx.R3 r, double[] pos, float bodyYaw, float headYaw, float pitch, boolean sneaking, float h, int wing, int hatK, int backK, int tint, boolean air, float light, long now) {
        if (sneaking && h > 1.7f) h = 1.5f; final double ln = sneaking ? LEAN : 0, pr = Math.toRadians(Math.max(-90, Math.min(90, pitch))), cp = Math.cos(pr), sp = Math.sin(pr);
        double yb = Math.toRadians(bodyYaw), cyb = Math.cos(yb), syb = Math.sin(yb), sc = (sneaking ? 1.8 : h) / 1.8;
        // own models: wings on the upper back, hat on the head (turns with it), item diagonally on the back
        if (wing == OWN_W) { double fl = Math.sin(now / 1000.0 * (air ? 9 : 1.8)) * (air ? 0.25 : 0.06);
            Model.draw(r, custom("wings"), (x, y, z) -> { double zz = z - 0.28 - Math.abs(x) * fl; return new double[]{pos[0] + (x * cyb - zz * syb) * sc, pos[1] + (0.55 + y) * sc, pos[2] + (x * syb + zz * cyb) * sc}; }, light); wing = 0; }
        if (hatK == OWN_H) { double yh = Math.toRadians(headYaw), ch = Math.cos(yh), shh = Math.sin(yh), top = (sneaking ? 1.84 - HEAD_DROP * 0.0586 : h < 1.7 ? 1.55 : 1.84) * sc;
            final double neck = 0.47; Model.draw(r, custom("hat"), (x, y, z) -> { double yy = y + neck, y2 = yy * cp - z * sp, z2 = z * cp + yy * sp; return new double[]{pos[0] + (x * ch - z2 * shh) * sc, pos[1] + top - neck * sc + y2 * sc, pos[2] + (x * shh + z2 * ch) * sc}; }, light); hatK = 0; }
        if (backK == OWN_B) { double a = Math.toRadians(35), ca = Math.cos(a), sa = Math.sin(a);
            Model.draw(r, custom("back"), (x, y, z) -> { double yy = y - 0.85, xx = x * ca - yy * sa, y2 = x * sa + yy * ca, zz = z - 0.26; return new double[]{pos[0] + (xx * cyb - zz * syb) * sc, pos[1] + (1.1 + y2) * sc, pos[2] + (xx * syb + zz * cyb) * sc}; }, light); backK = 0; }
        if (wing == OWN_W) wing = 0; if (hatK == OWN_H) hatK = 0; if (backK == OWN_B) backK = 0; // the own model was not loaded (yet)
        if (wing == 0 && hatK == 0 && backK == 0) return;
        double k = 0.9375 / 16 * (sneaking ? 1 : h / 1.8), sneak = 0; final double hDrop = sneaking ? HEAD_DROP : 0, bDrop = sneaking ? BODY_DROP : 0, neck = 24 - bDrop; float t = now / 1000f;
        int acc = tint == -1 ? V.a1() : tint, acc2 = tint == -1 ? V.a2() : tint;
        r.beginSolid();
        if (wing > 0 && wing < WINGS.length) { List<Ob> w = CACHE_W[wing] != null ? CACHE_W[wing] : (CACHE_W[wing] = wings(wing));
            final double ph = Math.toRadians(26 + 5 * Math.sin(t * 1.8)), be = Math.toRadians(air ? 26 * Math.sin(t * 9) : 6 * Math.sin(t * 1.8)), sn = sneak;
            for (int s = -1; s <= 1; s += 2) { final int sd = s;
                // wing space (outward = -x) -> body space: spread backwards by ph, beat up and down by be, attached to the upper back; +x is the player's left
                emit(r, w, pos, bodyYaw, k, light, acc, acc2, (x, y, z) -> { double o = Math.max(0, -x), oo = o * Math.cos(be);
                    return lean(sd * (1.6 + oo * Math.cos(ph)), 21.5 + y + o * Math.sin(be) - bDrop, -2.3 - oo * Math.sin(ph) + z, ln, neck); }); } }
        if (hatK > 0 && hatK < HATS.length) { List<Ob> hl = CACHE_H[hatK] != null ? CACHE_H[hatK] : (CACHE_H[hatK] = hat(hatK));
            float rel = headYaw - bodyYaw;
            final double sn = sneak; emit(r, hl, pos, bodyYaw, k, light, acc, acc2, (x, y, z) -> { double y2 = y * cp - z * sp, z2 = z * cp + y * sp, a = Math.toRadians(rel), cx = x * Math.cos(a) - z2 * Math.sin(a), cz = x * Math.sin(a) + z2 * Math.cos(a); return new double[]{cx, 24 + y2 - hDrop, cz}; }); }
        if (backK > 0 && backK < BACK.length) { List<Ob> bl = CACHE_B[backK] != null ? CACHE_B[backK] : (CACHE_B[backK] = back(backK));
            final double sn = sneak; emit(r, bl, pos, bodyYaw, k, light, acc, acc2, (x, y, z) -> { double a = Math.toRadians(38); double cx = x * Math.cos(a) - y * Math.sin(a), cy = x * Math.sin(a) + y * Math.cos(a); return lean(cx, 18 + cy - bDrop, -3.4 + z, ln, neck); }); }
        r.end();
    }
    interface Tf { double[] at(double x, double y, double z); }
    /** colour codes (low bytes FE FE 0n, alpha kept): ACC = cosmetics colour, ACC2 = lighter, GLOW = glowing edge */
    static final int ACC = 0xFFFEFE01, ACC2 = 0xFFFEFE02, GLOW = 0xFFFEFE03;
    private static int col(int c, int acc, int acc2) { int k = c & 0xFFFFFF, a = c >>> 24; if (k == 0xFEFE01) return D.alpha(acc & 0xFFFFFF, a); if (k == 0xFEFE02) return D.alpha(D.mix(acc2, 0xFFFFFFFF, 0.35f) & 0xFFFFFF, a); if (k == 0xFEFE03) return D.alpha(D.mix(acc, 0xFFFFFFFF, 0.25f) & 0xFFFFFF, a); return c; }
    private static void emit(Fx.R3 r, List<Ob> list, double[] pos, float yawDeg, double k, float light, int acc, int acc2, Tf tf) {
        double yaw = Math.toRadians(yawDeg), cy = Math.cos(yaw), sy = Math.sin(yaw);
        for (Ob o : list) {
            int c0 = col(o.c0, acc, acc2), c1 = col(o.c1, acc, acc2); float glow0 = (o.c0 & 0xFFFFFF) == 0xFEFE03 || (o.c1 & 0xFFFFFF) == 0xFEFE03 ? 1 : light;
            if (o.tri) { double[] a = world(tf.at(o.cx, o.cy, o.cz), pos, cy, sy, k), b = world(tf.at(o.ux, o.uy, o.uz), pos, cy, sy, k), c = world(tf.at(o.vx, o.vy, o.vz), pos, cy, sy, k);
                int ca = Fx.shade(c0, light), cb = Fx.shade(c1, light); r.v(a[0], a[1], a[2], ca); r.v(b[0], b[1], b[2], cb); r.v(c[0], c[1], c[2], cb); r.v(c[0], c[1], c[2], cb); continue; }
            double nx = o.uy * o.vz - o.uz * o.vy, ny = o.uz * o.vx - o.ux * o.vz, nz = o.ux * o.vy - o.uy * o.vx;
            double[][] v = new double[8][];
            for (int i = 0; i < 8; i++) { double su = (i & 1) == 0 ? -0.5 : 0.5, sv = (i & 2) == 0 ? -0.5 : 0.5, sn = (i & 4) == 0 ? -0.5 : 0.5;
                v[i] = world(tf.at(o.cx + o.ux * o.lu * su + o.vx * o.lv * sv + nx * o.lw * sn, o.cy + o.uy * o.lu * su + o.vy * o.lv * sv + ny * o.lw * sn, o.cz + o.uz * o.lu * su + o.vz * o.lv * sv + nz * o.lw * sn), pos, cy, sy, k); }
            float lt = glow0;
            int a0 = Fx.shade(c0, lt), a1 = Fx.shade(c1, lt), d0 = Fx.shade(c0, lt * 0.78f), d1 = Fx.shade(c1, lt * 0.78f), m0 = Fx.shade(c0, lt * 0.9f), m1 = Fx.shade(c1, lt * 0.9f);
            q(r, v[4], v[5], v[7], v[6], a0, a1, a1, a0); q(r, v[0], v[2], v[3], v[1], d0, d0, d1, d1);   // the two big faces (+n, -n)
            q(r, v[2], v[6], v[7], v[3], m0, m0, m1, m1); q(r, v[0], v[1], v[5], v[4], m0, m1, m1, m0);   // sides (+v, -v)
            q(r, v[1], v[3], v[7], v[5], a1, a1, a1, a1); q(r, v[0], v[4], v[6], v[2], d0, d0, d0, d0);   // ends (+u tip, -u root)
        }
    }
    private static double[] world(double[] l, double[] pos, double cy, double sy, double k) { return new double[]{pos[0] + (l[0] * cy - l[2] * sy) * k, pos[1] + l[1] * k, pos[2] + (l[0] * sy + l[2] * cy) * k}; }
    private static void q(Fx.R3 r, double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) { r.v(a[0], a[1], a[2], ca); r.v(b[0], b[1], b[2], cb); r.v(c[0], c[1], c[2], cc); r.v(d[0], d[1], d[2], cd); }
}
