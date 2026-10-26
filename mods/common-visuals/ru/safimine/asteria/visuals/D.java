package ru.safimine.asteria.visuals;

/** Drawing helpers on top of {@link Gfx}: rounded rectangles, soft shadows, gradients, circles, icons, colour maths. */
public final class D {
    private D() {}
    public static Gfx g;

    // ------------------------------------------------------------------ colour
    public static int a(int c, float alpha) { return (Math.max(0, Math.min(255, (int) (((c >>> 24) & 255) * alpha))) << 24) | (c & 0xFFFFFF); }
    public static int alpha(int c, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (c & 0xFFFFFF); }
    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (a >>> 24) & 255, ab = (b >>> 24) & 255;
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gg = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return ((int) (aa + (ab - aa) * t) << 24) | (r << 16) | (gg << 8) | bb;
    }
    public static int bright(int c, float k) { int r = Math.min(255, (int) (((c >> 16) & 255) * k)), gg = Math.min(255, (int) (((c >> 8) & 255) * k)), b = Math.min(255, (int) ((c & 255) * k)); return (c & 0xFF000000) | (r << 16) | (gg << 8) | b; }

    // ------------------------------------------------------------------ shapes
    public static void rect(float x1, float y1, float x2, float y2, int c) { g.poly(new float[]{x1, y1, x1, y2, x2, y2, x2, y1}, new int[]{c, c, c, c}); }
    /** horizontal gradient left -> right */
    public static void hgrad(float x1, float y1, float x2, float y2, int l, int r) { g.poly(new float[]{x1, y1, x1, y2, x2, y2, x2, y1}, new int[]{l, l, r, r}); }
    public static void vgrad(float x1, float y1, float x2, float y2, int t, int b) { g.poly(new float[]{x1, y1, x1, y2, x2, y2, x2, y1}, new int[]{t, b, b, t}); }

    /** rounded rectangle, optionally with a horizontal gradient (cl -> cr) */
    public static void round(float x1, float y1, float x2, float y2, float r, int cl, int cr) {
        r = Math.max(0, Math.min(r, Math.min(x2 - x1, y2 - y1) / 2));
        if (r < 0.5f) { hgrad(x1, y1, x2, y2, cl, cr); return; }
        int seg = r > 4 ? 8 : 5, n = seg * 4 + 4;
        float[] xy = new float[n * 2]; int[] col = new int[n]; int k = 0;
        float[][] corners = {{x1 + r, y1 + r, 180}, {x2 - r, y1 + r, 270}, {x2 - r, y2 - r, 0}, {x1 + r, y2 - r, 90}}; // clockwise on screen
        for (float[] c : corners) for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians(c[2] + 90.0 * i / seg);
            float px = (float) (c[0] + Math.cos(ang) * r), py = (float) (c[1] + Math.sin(ang) * r);
            xy[k * 2] = px; xy[k * 2 + 1] = py; col[k] = cl == cr ? cl : mix(cl, cr, (px - x1) / Math.max(1, x2 - x1)); k++;
        }
        g.poly(xy, col);
    }
    public static void round(float x1, float y1, float x2, float y2, float r, int c) { round(x1, y1, x2, y2, r, c, c); }
    /** soft drop shadow made of a few expanding translucent rounded rects */
    public static void shadow(float x1, float y1, float x2, float y2, float r, float strength) {
        if (!V.shadow) return;
        for (int i = 4; i >= 1; i--) round(x1 - i, y1 - i + 1.5f, x2 + i, y2 + i + 1.5f, r + i, alpha(0, (int) (22 * strength / i)));
    }
    /** crystal panel used everywhere in the HUD (SafiMine style): soft accent glow, gradient crystal rim,
     *  dark translucent body with a glassy top sheen and a small crystal in the corner */
    public static void panel(float x1, float y1, float x2, float y2, float r) { panel(x1, y1, x2, y2, r, true); }
    public static void panel(float x1, float y1, float x2, float y2, float r, boolean gem) {
        if (V.hudStyle == 1) { lines(x1, y1, x2, y2); return; }
        if (V.shadow) for (int i = 3; i >= 1; i--) round(x1 - i, y1 - i, x2 + i, y2 + i, r + i, D.a(V.a1(), 0.10f / i), D.a(V.a2(), 0.10f / i));
        round(x1, y1, x2, y2, r, D.a(bright(V.a1(), 1.1f), 0.85f), D.a(V.a2(), 0.85f));                 // rim
        int body = alpha(0x050B16, (int) (255 * Math.max(0.35f, V.bg)));
        round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), body);                                   // body
        round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), D.a(V.a1(), 0.07f), D.a(V.a2(), 0.03f)); // tint
        float h = y2 - y1;
        if (h > 8) round(x1 + 2, y1 + 1.5f, x2 - 2, y1 + Math.min(h * 0.42f, 9), Math.max(0, r - 2), alpha(0xFFFFFF, 14)); // glass sheen
        round(x1 + r * 0.6f, y1 + 0.5f, x2 - r * 0.6f, y1 + 1.3f, 0.4f, alpha(0xFFFFFF, 0), alpha(0xFFFFFF, 140));          // highlight line
        if (gem && x2 - x1 > 34 && h > 14) { crystal(x1 + 1.5f, y1 + 1.5f, 3.6f); sparkle(x2 - 3.5f, y2 - 3.5f, 1.8f, alpha(0xFFFFFF, 170)); }
    }
    /** "Asteria lines" panel: barely-there backing, a stepped accent line on top (like the asset board) and a tiny diamond */
    public static void lines(float x1, float y1, float x2, float y2) {
        round(x1, y1, x2, y2, 3, alpha(0x041B22, (int) (150 * Math.max(0.2f, V.bg))));
        float w = x2 - x1, k = Math.min(36, w * 0.28f);
        line(x1 + 4, y1, x1 + k, y1, 1.2f, V.a2());
        line(x1 + k, y1, x1 + k + 4, y1 + 2.5f, 1.2f, V.a2());
        hgrad(x1 + k + 4, y1 + 2f, x2 - 2, y1 + 3.1f, D.a(V.a2(), 0.9f), D.a(V.a1(), 0.05f));
        diamond(x1 + 2.5f, y1 + 0.5f, 2.6f, V.a2());
        hgrad(x1, y2 - 0.8f, x1 + w * 0.35f, y2, D.a(V.a1(), 0.55f), D.a(V.a1(), 0f));
    }
    /** outlined diamond (crosshair / bullets from the asset board) */
    public static void diamond(float cx, float cy, float R, int c) {
        float i = R * 0.32f;
        g.poly(new float[]{cx, cy - R, cx + i, cy - i, cx + R, cy, cx + i, cy + i, cx, cy + R, cx - i, cy + i, cx - R, cy, cx - i, cy - i}, new int[]{c, c, c, c, c, c, c, c});
    }
    /** faceted four-point crystal star (light from the top-left), like the launcher icon */
    public static void crystal(float cx, float cy, float R) {
        float i = R * 0.3f; int a = V.a1(), b = V.a2();
        int[] col = {mix(b, 0xFFFFFFFF, 0.85f), mix(b, 0xFFFFFFFF, 0.45f), mix(b, 0xFFFFFFFF, 0.6f), b, a, bright(a, 0.6f), bright(a, 0.75f), mix(b, 0xFFFFFFFF, 0.2f)};
        float[][] tip = {{0, -R}, {0, -R}, {R, 0}, {R, 0}, {0, R}, {0, R}, {-R, 0}, {-R, 0}};
        float[][] in = {{i, -i}, {-i, -i}, {i, -i}, {i, i}, {i, i}, {-i, i}, {-i, i}, {-i, -i}};
        if (V.shadow && R > 3) ring(cx, cy, 0, R * 1.1f, D.a(b, 0.35f), D.a(b, 0f));
        for (int k = 0; k < 8; k++) g.poly(new float[]{cx + tip[k][0], cy + tip[k][1], cx + in[k][0], cy + in[k][1], cx, cy}, new int[]{col[k], col[k], col[k]});
        circle(cx, cy, Math.max(0.5f, R * 0.12f), 0xFFFFFFFF);
    }
    /** tiny four-point sparkle */
    public static void sparkle(float cx, float cy, float R, int c) { star(cx, cy, R, c, c); }
    /** crystal tile (keys, icon frames): rim + dark body, filled with the accent when p > 0 */
    public static void tile(float x1, float y1, float x2, float y2, float r, float p) {
        if (V.hudStyle == 1) { // outlined key like the board's WASD
            round(x1, y1, x2, y2, 3, D.a(V.a2(), 0.55f + 0.45f * p));
            round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 2, alpha(0x041B22, (int) (200 * Math.max(0.3f, V.bg))));
            if (p > 0.01f) round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 2, D.a(V.a2(), p * 0.85f), D.a(V.a1(), p * 0.85f));
            return; }
        round(x1, y1, x2, y2, r, D.a(V.a1(), 0.55f + 0.45f * p), D.a(V.a2(), 0.55f + 0.45f * p));
        round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), alpha(0x07111F, (int) (255 * Math.max(0.35f, V.bg))));
        if (p > 0.01f) round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), D.a(bright(V.a1(), 1.05f), p * 0.9f), D.a(V.a2(), p * 0.9f));
        if (y2 - y1 > 8) round(x1 + 2, y1 + 1.5f, x2 - 2, y1 + (y2 - y1) * 0.4f, Math.max(0, r - 2), alpha(0xFFFFFF, (int) (16 + 20 * p)));
    }
    public static void circle(float cx, float cy, float rad, int c) { ring(cx, cy, 0, rad, c, c); }
    /** ring / disc (inner radius 0 = disc); colour inner -> outer */
    public static void ring(float cx, float cy, float rin, float rout, int cin, int cout) {
        int seg = Math.max(12, (int) (rout * 3));
        if (rin <= 0) {
            float[] xy = new float[seg * 2 + 4]; int[] col = new int[seg + 2];
            xy[0] = cx; xy[1] = cy; col[0] = cin;
            for (int i = 0; i <= seg; i++) { double a = Math.PI * 2 * i / seg; xy[2 + i * 2] = (float) (cx + Math.cos(a) * rout); xy[3 + i * 2] = (float) (cy + Math.sin(a) * rout); col[1 + i] = cout; }
            g.poly(xy, col); return;
        }
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            g.poly(new float[]{(float) (cx + Math.cos(a0) * rin), (float) (cy + Math.sin(a0) * rin), (float) (cx + Math.cos(a0) * rout), (float) (cy + Math.sin(a0) * rout),
                    (float) (cx + Math.cos(a1) * rout), (float) (cy + Math.sin(a1) * rout), (float) (cx + Math.cos(a1) * rin), (float) (cy + Math.sin(a1) * rin)}, new int[]{cin, cout, cout, cin});
        }
    }
    /** arc ring from angle a0 to a1 (degrees, 0 = up, clockwise) */
    public static void arc(float cx, float cy, float rin, float rout, float a0, float a1, int c) {
        int seg = Math.max(6, (int) Math.abs(a1 - a0) / 8);
        for (int i = 0; i < seg; i++) {
            double t0 = Math.toRadians(a0 + (a1 - a0) * i / seg - 90), t1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / seg - 90);
            g.poly(new float[]{(float) (cx + Math.cos(t0) * rin), (float) (cy + Math.sin(t0) * rin), (float) (cx + Math.cos(t0) * rout), (float) (cy + Math.sin(t0) * rout),
                    (float) (cx + Math.cos(t1) * rout), (float) (cy + Math.sin(t1) * rout), (float) (cx + Math.cos(t1) * rin), (float) (cy + Math.sin(t1) * rin)}, new int[]{c, c, c, c});
        }
    }
    /** four-point Asteria star */
    public static void star(float cx, float cy, float R, int c1, int c2) {
        float r = R * 0.3f; float[] xy = new float[18]; int[] col = new int[9];
        xy[0] = cx; xy[1] = cy; col[0] = 0xFFFFFFFF;
        for (int i = 0; i < 8; i++) { double a = Math.toRadians(-90 + 45 * i); float rad = i % 2 == 0 ? R : r;
            xy[2 + i * 2] = (float) (cx + Math.cos(a) * rad); xy[3 + i * 2] = (float) (cy + Math.sin(a) * rad); col[1 + i] = i < 4 ? c1 : c2; }
        float[] full = new float[20]; System.arraycopy(xy, 0, full, 0, 18); full[18] = xy[2]; full[19] = xy[3];
        int[] fc = new int[10]; System.arraycopy(col, 0, fc, 0, 9); fc[9] = col[1];
        g.poly(full, fc);
    }
    public static void line(float x1, float y1, float x2, float y2, float w, int c) {
        float dx = x2 - x1, dy = y2 - y1, l = (float) Math.sqrt(dx * dx + dy * dy); if (l < 0.001f) return;
        float nx = -dy / l * w / 2, ny = dx / l * w / 2;
        g.poly(new float[]{x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny}, new int[]{c, c, c, c});
    }

    // ------------------------------------------------------------------ text
    public static void text(String s, float x, float y, int c) { g.text(s, x, y, c, V.shadow); }
    public static void textC(String s, float cx, float y, int c) { g.text(s, cx - g.width(s) / 2f, y, c, V.shadow); }
    public static void textS(String s, float x, float y, float scale, int c) { g.push(x, y, scale); g.text(s, 0, 0, c, V.shadow); g.pop(); }
    /** text drawn letter by letter along the theme gradient */
    public static float gradText(String s, float x, float y, boolean shadow) {
        float px = x; int n = Math.max(1, s.length() - 1);
        for (int i = 0; i < s.length(); i++) { String ch = s.substring(i, i + 1); g.text(ch, px, y, V.grad(i / (float) n * 0.5f), shadow); px += g.width(ch); }
        return px - x;
    }
    public static int w(String s) { return g.width(s); }

    // ------------------------------------------------------------------ animation
    private static final java.util.Map<String, float[]> AN = new java.util.HashMap<>();
    private static long lastFrame = System.nanoTime(); public static float dt = 0.016f;
    public static void frame() { long n = System.nanoTime(); dt = Math.min(0.1f, (n - lastFrame) / 1e9f); lastFrame = n; }
    /** smoothly moves a named value towards target (exponential), speed ~ 1/seconds */
    public static float anim(String key, float target, float speed) {
        float[] v = AN.get(key); if (v == null) { AN.put(key, new float[]{target}); return target; }
        v[0] += (target - v[0]) * Math.min(1f, dt * speed); if (Math.abs(target - v[0]) < 0.001f) v[0] = target; return v[0];
    }
    public static float ease(float t) { t = Math.max(0, Math.min(1, t)); return 1 - (float) Math.pow(1 - t, 3); }
}
