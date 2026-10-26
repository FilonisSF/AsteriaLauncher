package ru.safimine.asteria.visuals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** World visuals (trails, jump circles, target marker, china hat, hit particles) + target tracking + zoom state. */
public final class Fx {
    private Fx() {}

    /** 3D drawing in world coordinates. Implemented per Minecraft version. */
    public interface R3 {
        int TRIS = 0, QUADS = 1, STRIP = 2, LINES = 3;
        void begin(int mode, boolean depth);
        void v(double x, double y, double z, int argb);
        void end();
        double[] cam();
        float camYaw();
        float camPitch();
        float partial();
    }

    // ------------------------------------------------------------------ target
    private static Object target; private static long targetAt;
    public static void onAttack(Game g, Object e) {
        if (!g.living(e)) return;
        target = e; targetAt = System.currentTimeMillis();
        if (V.on("hitparticles")) burst(g, e);
    }
    /** what the target HUD / marker show: last attacked (3 s) or the living entity under the crosshair */
    public static Object target(Game g) {
        if (target != null && (!g.alive(target) || System.currentTimeMillis() - targetAt > 3500)) target = null;
        if (target != null) return target;
        Object c = g.crosshairEntity(); return c != null && g.living(c) && g.alive(c) ? c : null;
    }

    // ------------------------------------------------------------------ per tick
    private static final List<double[]> TRAIL = new ArrayList<>();
    private static final List<double[]> CIRCLES = new ArrayList<>(); // x,y,z,startMillis
    private static final List<double[]> PARTS = new ArrayList<>();   // x,y,z,vx,vy,vz,born,life,seed
    private static boolean wasGround = true;
    private static final List<double[]> KPARTS = new ArrayList<>();  // kill burst shards (same layout as PARTS)
    private static final List<double[]> PILLARS = new ArrayList<>(); // x,y,z,start (kill light pillar)
    private static final List<double[]> AMB = new ArrayList<>();     // ambient particles around the player
    private static double[] targetPos; private static float targetH = 1.8f;

    public static void tick(Game g) {
        Object self = g.self(); if (self == null) { TRAIL.clear(); return; }
        double[] p = g.pos(self, 1);
        long now0 = System.currentTimeMillis();
        // kill effect: the entity we hit last died within a few seconds
        if (target != null) {
            if (g.alive(target)) { targetPos = g.pos(target, 1); targetH = g.height(target); }
            else { if (V.on("killeffect") && targetPos != null && now0 - targetAt < 5000) kill(targetPos, targetH, now0); target = null; }
        }
        // ambient particles drifting around the player
        if (V.on("ambient")) {
            int max = (int) V.num("ambient", "count"); double R = V.num("ambient", "radius"); int st = V.mode("ambient", "style");
            AMB.removeIf(q -> now0 - q[6] > q[7] || Math.abs(q[0] - p[0]) > R * 1.6 || Math.abs(q[2] - p[2]) > R * 1.6);
            while (AMB.size() < max) { double a = Math.random() * Math.PI * 2, d = Math.random() * R;
                AMB.add(new double[]{p[0] + Math.cos(a) * d, p[1] + (st == 1 ? 2.5 + Math.random() * 2 : Math.random() * 3), p[2] + Math.sin(a) * d,
                    (Math.random() - .5) * .02, st == 1 ? -.02 - Math.random() * .02 : (Math.random() - .5) * .01, (Math.random() - .5) * .02, now0, 4000 + Math.random() * 5000, Math.random()}); }
            for (double[] q : AMB) { q[0] += q[3]; q[1] += q[4]; q[2] += q[5]; if (st != 1) { q[3] += (Math.random() - .5) * .004; q[5] += (Math.random() - .5) * .004; q[4] += (Math.random() - .5) * .002; } }
        } else AMB.clear();
        for (double[] q : KPARTS) { q[3] *= 0.93; q[5] *= 0.93; q[4] = q[4] * 0.93 - 0.008; q[0] += q[3]; q[1] += q[4]; q[2] += q[5]; }
        KPARTS.removeIf(q -> now0 - q[6] > q[7]); PILLARS.removeIf(q -> now0 - q[3] > 1600);
        if (V.on("trails")) {
            int max = (int) V.num("trails", "length");
            if (TRAIL.isEmpty() || dist(TRAIL.get(TRAIL.size() - 1), p) > 0.02) TRAIL.add(p.clone());
            else if (!TRAIL.isEmpty()) TRAIL.remove(0); // standing still: the trail shrinks
            while (TRAIL.size() > max) TRAIL.remove(0);
        } else TRAIL.clear();
        boolean ground = g.onGround();
        if (V.on("jumpcircles") && ground != wasGround) CIRCLES.add(new double[]{p[0], p[1], p[2], System.currentTimeMillis()});
        wasGround = ground;
        for (double[] q : PARTS) { q[3] *= 0.92; q[5] *= 0.92; q[4] = q[4] * 0.92 - 0.012; q[0] += q[3]; q[1] += q[4]; q[2] += q[5]; }
        long now = System.currentTimeMillis();
        PARTS.removeIf(q -> now - q[6] > q[7]);
    }
    private static void kill(double[] p, float h, long now) {
        for (int i = 0; i < 46; i++) { double a = Math.random() * Math.PI * 2, sp = 0.08 + Math.random() * 0.22;
            KPARTS.add(new double[]{p[0], p[1] + h * (0.2 + Math.random() * 0.7), p[2], Math.cos(a) * sp, 0.08 + Math.random() * 0.25, Math.sin(a) * sp, now, 1300 + Math.random() * 900, Math.random()}); }
        if (V.mode("killeffect", "style") == 0) PILLARS.add(new double[]{p[0], p[1], p[2], now});
    }
    private static void burst(Game g, Object e) {
        double[] p = g.pos(e, 1); float h = g.height(e); int n = (int) V.num("hitparticles", "count"); long now = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            double a = Math.random() * Math.PI * 2, s = 0.06 + Math.random() * 0.12;
            PARTS.add(new double[]{p[0], p[1] + h * (0.4 + Math.random() * 0.5), p[2], Math.cos(a) * s, 0.06 + Math.random() * 0.14, Math.sin(a) * s, now, 900 + Math.random() * 700, Math.random()});
        }
    }

    // ------------------------------------------------------------------ render (after the world, before the hand)
    public static void render(R3 r, Game g) {
        long now = System.currentTimeMillis(); float pt = r.partial();
        Object self = g.self(); if (self == null) return;
        if (V.on("trails") && TRAIL.size() > 1) trail(r, g, self, pt);
        if (V.on("jumpcircles")) circles(r, now);
        Object t = target(g);
        if (V.on("targetesp") && t != null && t != self) targetEsp(r, g, t, pt, now);
        if (V.on("chinahat") && g.thirdPerson()) hat(r, g, self, pt, now);
        if (!PARTS.isEmpty()) particles(r, now);
        if (!KPARTS.isEmpty()) { r.begin(R3.QUADS, true); for (double[] q : KPARTS) { float life = (float) ((now - q[6]) / q[7]), al = 1 - life; int c = D.a(V.grad((float) q[8]), al);
            double x = q[0] + q[3] * pt, y = q[1] + q[4] * pt, z = q[2] + q[5] * pt; shard(r, x, y, z, 0.16 * (0.6 + q[8]), q[8] * 6.28 + life * 4, c, D.a(0xFFFFFFFF, al)); } r.end(); }
        if (!PILLARS.isEmpty()) pillars(r, now);
        if (!AMB.isEmpty()) ambient(r, now, pt);
    }

    private static void trail(R3 r, Game g, Object self, float pt) {
        List<double[]> pts = new ArrayList<>(TRAIL); pts.add(g.pos(self, pt));
        int n = pts.size(); boolean ribbon = V.mode("trails", "mode") == 0; double h = V.num("trails", "height");
        if (ribbon) {
            r.begin(R3.STRIP, true);
            for (int i = 0; i < n; i++) { double[] p = pts.get(i); float f = i / (float) (n - 1); int c = V.grad(f * 0.6f);
                r.v(p[0], p[1] + 0.05, p[2], D.a(c, 0.75f * f)); r.v(p[0], p[1] + h, p[2], D.a(c, 0.05f * f)); }
            r.end();
        } else {
            r.begin(R3.LINES, true);
            for (int i = 1; i < n; i++) { double[] a = pts.get(i - 1), b = pts.get(i); float f = i / (float) (n - 1); int c = D.a(V.grad(f * 0.6f), f);
                r.v(a[0], a[1] + 0.1, a[2], c); r.v(b[0], b[1] + 0.1, b[2], c); }
            r.end();
        }
    }
    private static void circles(R3 r, long now) {
        double life = V.num("jumpcircles", "life") * 1000, R = V.num("jumpcircles", "radius");
        Iterator<double[]> it = CIRCLES.iterator();
        while (it.hasNext()) {
            double[] c = it.next(); double t = (now - c[3]) / life; if (t >= 1) { it.remove(); continue; }
            double e = 1 - Math.pow(1 - t, 3), rad = 0.2 + R * e, w = 0.12 + 0.25 * (1 - t); float a = (float) (1 - t);
            r.begin(R3.STRIP, true);
            int seg = 48;
            for (int i = 0; i <= seg; i++) { double ang = Math.PI * 2 * i / seg; int col = V.grad(i / (float) seg);
                r.v(c[0] + Math.cos(ang) * rad, c[1] + 0.02, c[2] + Math.sin(ang) * rad, D.a(col, a * 0.9f));
                r.v(c[0] + Math.cos(ang) * (rad - w), c[1] + 0.02, c[2] + Math.sin(ang) * (rad - w), D.a(col, 0)); }
            r.end();
        }
    }
    private static void targetEsp(R3 r, Game g, Object t, float pt, long now) {
        double[] p = g.pos(t, pt); float h = g.height(t), w = g.width(t); double sp = V.num("targetesp", "speed");
        double phase = (Math.sin(now / 1000.0 * sp * 2.2) + 1) / 2, y = p[1] + h * (0.05 + 0.9 * phase), rad = w * 0.75 + 0.15;
        int seg = 56;
        if (V.mode("targetesp", "style") == 0) {
            // glowing ring + a fading curtain trailing behind its motion
            double dir = Math.cos(now / 1000.0 * sp * 2.2) > 0 ? -1 : 1;
            r.begin(R3.STRIP, true);
            for (int i = 0; i <= seg; i++) { double a = Math.PI * 2 * i / seg; int c = V.grad(i / (float) seg); double x = p[0] + Math.cos(a) * rad, z = p[2] + Math.sin(a) * rad;
                r.v(x, y, z, D.a(c, 0.85f)); r.v(x, y + dir * 0.35, z, D.a(c, 0)); }
            r.end();
            r.begin(R3.LINES, true);
            for (int i = 0; i < seg; i++) { double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg; int c = V.grad(i / (float) seg);
                r.v(p[0] + Math.cos(a0) * rad, y, p[2] + Math.sin(a0) * rad, c); r.v(p[0] + Math.cos(a1) * rad, y, p[2] + Math.sin(a1) * rad, c); }
            r.end();
        } else {
            // "ghost": three orbiting glowing orbs leaving short tails
            for (int k = 0; k < 3; k++) {
                r.begin(R3.QUADS, true);
                for (int j = 0; j < 14; j++) {
                    double a = now / 1000.0 * sp * 3 + k * Math.PI * 2 / 3 - j * 0.07;
                    double yy = p[1] + h * (0.5 + 0.4 * Math.sin(now / 900.0 * sp + k * 2 - j * 0.07));
                    billboard(r, p[0] + Math.cos(a) * rad, yy, p[2] + Math.sin(a) * rad, 0.11 * (1 - j / 16.0), D.a(V.grad(k / 3f), 0.9f * (1 - j / 14f)));
                }
                r.end();
            }
        }
    }
    private static void hat(R3 r, Game g, Object self, float pt, long now) {
        double[] p = g.pos(self, pt); double top = p[1] + g.height(self) + 0.08, rad = V.num("chinahat", "size"); float a = (float) V.num("chinahat", "alpha");
        int seg = 40; double yaw = Math.toRadians(-g.bodyYaw(pt));
        r.begin(R3.TRIS, true);
        for (int i = 0; i < seg; i++) {
            double a0 = yaw + Math.PI * 2 * i / seg, a1 = yaw + Math.PI * 2 * (i + 1) / seg; int c0 = V.grad(i / (float) seg), c1 = V.grad((i + 1) / (float) seg);
            r.v(p[0], top + rad * 0.45, p[2], D.a(D.mix(c0, 0xFFFFFFFF, 0.3f), a));
            r.v(p[0] + Math.cos(a0) * rad, top, p[2] + Math.sin(a0) * rad, D.a(c0, a * 0.7f));
            r.v(p[0] + Math.cos(a1) * rad, top, p[2] + Math.sin(a1) * rad, D.a(c1, a * 0.7f));
        }
        r.end();
        r.begin(R3.LINES, true);
        for (int i = 0; i < seg; i++) { double a0 = yaw + Math.PI * 2 * i / seg, a1 = yaw + Math.PI * 2 * (i + 1) / seg; int c = V.grad(i / (float) seg);
            r.v(p[0] + Math.cos(a0) * rad, top, p[2] + Math.sin(a0) * rad, c); r.v(p[0] + Math.cos(a1) * rad, top, p[2] + Math.sin(a1) * rad, c); }
        r.end();
    }
    private static void particles(R3 r, long now) {
        int style = V.mode("hitparticles", "style");
        r.begin(R3.QUADS, true);
        for (double[] q : PARTS) {
            float life = (float) ((now - q[6]) / q[7]), a = 1 - life; int c = D.a(V.grad((float) q[8]), a);
            double pt = r.partial(), x = q[0] + q[3] * pt, y = q[1] + q[4] * pt, z = q[2] + q[5] * pt, s = 0.07 * (0.6 + q[8] * 0.6);
            if (style == 1) billboard(r, x, y, z, s, c);
            else if (style == 4) { bubble(r, x, y + life * 0.3, z, s * (1.2 + life * 1.6), c); billboard(r, x - s * 0.4, y + s * 0.5 + life * 0.3, z, s * 0.18, D.a(0xFFFFFFFF, a * 0.8f)); }
            else if (style == 3) { shard(r, x, y, z, s * 1.6, q[8] * 6.28 + life * 3, c, D.a(0xFFFFFFFF, a)); shard(r, x, y, z, s * 0.9, q[8] * 6.28 + 1.57 + life * 3, c, D.a(0xFFFFFFFF, a * 0.8f)); }
            else if (style == 2) { billboard(r, x - s * 0.5, y + s * 0.4, z, s * 0.7, c); billboard(r, x + s * 0.5, y + s * 0.4, z, s * 0.7, c); billboard(r, x, y - s * 0.3, z, s * 0.8, c); }
            else { billboard(r, x, y, z, s * 0.35, D.mix(c, D.a(0xFFFFFFFF, a), 0.5f)); starBillboard(r, x, y, z, s * 1.4, c); }
        }
        r.end();
    }
    private static void pillars(R3 r, long now) {
        double[] rt = right(r); r.begin(R3.QUADS, true);
        for (double[] q : PILLARS) { float t = (now - (long) q[3]) / 1600f, a = (float) Math.sin(Math.PI * Math.min(1, t * 1.4)) * (1 - t); double w = 0.35 + 0.5 * t, top = 18;
            int c = D.a(V.a2(), a), e = D.a(V.a1(), 0);
            r.v(q[0] - rt[0] * w, q[1], q[2] - rt[2] * w, c); r.v(q[0] - rt[0] * w, q[1] + top, q[2] - rt[2] * w, e);
            r.v(q[0] + rt[0] * w, q[1] + top, q[2] + rt[2] * w, e); r.v(q[0] + rt[0] * w, q[1], q[2] + rt[2] * w, c);
            double w2 = w * 0.25; int cw = D.a(0xFFFFFFFF, a);
            r.v(q[0] - rt[0] * w2, q[1], q[2] - rt[2] * w2, cw); r.v(q[0] - rt[0] * w2, q[1] + top * 0.8, q[2] - rt[2] * w2, D.a(0xFFFFFFFF, 0));
            r.v(q[0] + rt[0] * w2, q[1] + top * 0.8, q[2] + rt[2] * w2, D.a(0xFFFFFFFF, 0)); r.v(q[0] + rt[0] * w2, q[1], q[2] + rt[2] * w2, cw); }
        r.end();
    }
    private static void ambient(R3 r, long now, float pt) {
        int st = V.mode("ambient", "style"); r.begin(R3.QUADS, true);
        for (double[] q : AMB) { float life = (float) ((now - q[6]) / q[7]), a = (float) Math.min(1, Math.min(life * 4, (1 - life) * 3));
            double x = q[0] + q[3] * pt, y = q[1] + q[4] * pt, z = q[2] + q[5] * pt;
            if (st == 1) billboard(r, x, y, z, 0.035, D.a(0xFFF0F9FF, a * 0.9f));
            else if (st == 2) starBillboard(r, x, y, z, 0.09 + q[8] * 0.06, D.a(V.grad((float) q[8]), a));
            else { float tw = 0.55f + 0.45f * (float) Math.sin(now / 300.0 + q[8] * 20); int c = V.grad((float) q[8]);
                billboard(r, x, y, z, 0.07, D.a(c, a * tw * 0.25f)); billboard(r, x, y, z, 0.022, D.a(D.mix(c, 0xFFFFFFFF, 0.6f), a * tw)); } }
        r.end();
    }
    /** camera-facing ring (hit bubbles) */
    static void bubble(R3 r, double x, double y, double z, double s, int c) {
        double[] rt = right(r), up = up(r); int seg = 12; double w = s * 0.16;
        for (int i = 0; i < seg; i++) { double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            r.v(x + (rt[0] * c0 + up[0] * s0) * s, y + (rt[1] * c0 + up[1] * s0) * s, z + (rt[2] * c0 + up[2] * s0) * s, c);
            r.v(x + (rt[0] * c0 + up[0] * s0) * (s - w), y + (rt[1] * c0 + up[1] * s0) * (s - w), z + (rt[2] * c0 + up[2] * s0) * (s - w), D.a(c, 0.2f));
            r.v(x + (rt[0] * c1 + up[0] * s1) * (s - w), y + (rt[1] * c1 + up[1] * s1) * (s - w), z + (rt[2] * c1 + up[2] * s1) * (s - w), D.a(c, 0.2f));
            r.v(x + (rt[0] * c1 + up[0] * s1) * s, y + (rt[1] * c1 + up[1] * s1) * s, z + (rt[2] * c1 + up[2] * s1) * s, c); }
    }
    /** camera-facing crystal shard: a long diamond, white tip -> accent tail, rotated by angle */
    static void shard(R3 r, double x, double y, double z, double s, double ang, int c, int tip) {
        double[] rt = right(r), up = up(r); double ca = Math.cos(ang), sa = Math.sin(ang);
        double[] ax = {rt[0] * ca + up[0] * sa, rt[1] * ca + up[1] * sa, rt[2] * ca + up[2] * sa}, ay = {-rt[0] * sa + up[0] * ca, -rt[1] * sa + up[1] * ca, -rt[2] * sa + up[2] * ca};
        double L = s, W = s * 0.32;
        r.v(x + ay[0] * L, y + ay[1] * L, z + ay[2] * L, tip); r.v(x + ax[0] * W, y + ax[1] * W, z + ax[2] * W, c);
        r.v(x - ay[0] * L * 0.6, y - ay[1] * L * 0.6, z - ay[2] * L * 0.6, D.a(c, 0.6f)); r.v(x - ax[0] * W, y - ax[1] * W, z - ax[2] * W, D.mix(c, tip, 0.5f));
    }
    /** camera-facing square */
    static void billboard(R3 r, double x, double y, double z, double s, int c) {
        double[] rt = right(r), up = up(r);
        r.v(x - rt[0] * s - up[0] * s, y - rt[1] * s - up[1] * s, z - rt[2] * s - up[2] * s, c);
        r.v(x - rt[0] * s + up[0] * s, y - rt[1] * s + up[1] * s, z - rt[2] * s + up[2] * s, c);
        r.v(x + rt[0] * s + up[0] * s, y + rt[1] * s + up[1] * s, z + rt[2] * s + up[2] * s, c);
        r.v(x + rt[0] * s - up[0] * s, y + rt[1] * s - up[1] * s, z + rt[2] * s - up[2] * s, c);
    }
    /** thin camera-facing cross (+ shape) — reads as a little star */
    static void starBillboard(R3 r, double x, double y, double z, double s, int c) {
        double[] rt = right(r), up = up(r); double t = s * 0.18; int e = D.a(c, 0.0f);
        // horizontal bar
        r.v(x - rt[0] * s - up[0] * t, y - rt[1] * s - up[1] * t, z - rt[2] * s - up[2] * t, e); r.v(x - rt[0] * s + up[0] * t, y - rt[1] * s + up[1] * t, z - rt[2] * s + up[2] * t, e);
        r.v(x + rt[0] * s + up[0] * t, y + rt[1] * s + up[1] * t, z + rt[2] * s + up[2] * t, c); r.v(x + rt[0] * s - up[0] * t, y + rt[1] * s - up[1] * t, z + rt[2] * s - up[2] * t, c);
        r.v(x - up[0] * s - rt[0] * t, y - up[1] * s - rt[1] * t, z - up[2] * s - rt[2] * t, e); r.v(x - up[0] * s + rt[0] * t, y - up[1] * s + rt[1] * t, z - up[2] * s + rt[2] * t, e);
        r.v(x + up[0] * s + rt[0] * t, y + up[1] * s + rt[1] * t, z + up[2] * s + rt[2] * t, c); r.v(x + up[0] * s - rt[0] * t, y + up[1] * s - rt[1] * t, z + up[2] * s - rt[2] * t, c);
    }
    static double[] right(R3 r) { double y = Math.toRadians(r.camYaw()); return new double[]{-Math.cos(y), 0, -Math.sin(y)}; }
    static double[] up(R3 r) { double y = Math.toRadians(r.camYaw()), p = Math.toRadians(r.camPitch()); return new double[]{-Math.sin(y) * Math.sin(p), Math.cos(p), Math.cos(y) * Math.sin(p)}; }
    private static double dist(double[] a, double[] b) { double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2]; return Math.sqrt(dx * dx + dy * dy + dz * dz); }

    // ------------------------------------------------------------------ zoom
    private static float zoomT, zoomLevel = -1; private static boolean zoomHeld;
    public static void zoomKey(boolean held) { if (held && !zoomHeld) zoomLevel = (float) V.num("zoom", "fov"); zoomHeld = held; }
    public static boolean zooming() { return zoomHeld && V.on("zoom"); }
    /** mouse wheel while zooming: stronger / weaker zoom. true = consumed */
    public static boolean scroll(double amount) {
        if (!zooming() || !V.bool("zoom", "scroll")) return false;
        zoomLevel = (float) Math.max(2, Math.min(70, zoomLevel * (amount > 0 ? 0.85 : 1.18))); return true;
    }
    public static double fov(double base) {
        float target = zooming() ? 1 : 0;
        zoomT = V.on("zoom") && V.bool("zoom", "smooth") ? zoomT + (target - zoomT) * Math.min(1, D.dt * 14) : target;
        if (zoomT < 0.002f) { zoomT = 0; return base; }
        double z = zoomLevel > 0 ? zoomLevel : V.num("zoom", "fov");
        return base + (z - base) * zoomT;
    }
    /** mouse sensitivity multiplier while zoomed (finer aim) */
    public static double sensitivity() { return zoomT > 0 ? 1 - zoomT * 0.55 : 1; }
}
