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
        int seg = r > 8 ? 14 : r > 4 ? 10 : 6, n = seg * 4 + 4; // round corners stay round on big GUI scales
        float[] xy = new float[n * 2]; int[] col = new int[n]; int k = 0;
        float[][] corners = {{x1 + r, y1 + r, 180}, {x2 - r, y1 + r, 270}, {x2 - r, y2 - r, 0}, {x1 + r, y2 - r, 90}}; // clockwise on screen
        for (float[] c : corners) for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians(c[2] + 90.0 * i / seg);
            float px = (float) (c[0] + Math.cos(ang) * r), py = (float) (c[1] + Math.sin(ang) * r);
            xy[k * 2] = px; xy[k * 2 + 1] = py; col[k] = cl == cr ? cl : mix(cl, cr, (px - x1) / Math.max(1, x2 - x1)); k++;
        }
        g.poly(xy, col);
        // feather: a strip from the edge (full colour) to 0.6 px outside (transparent)
        float f = feather(); float[] sx = new float[(n + 1) * 4]; int[] sc = new int[(n + 1) * 2]; k = 0;
        for (float[] c : corners) for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians(c[2] + 90.0 * i / seg); float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
            int idx = k / 2; sx[k * 2] = xy[idx * 2]; sx[k * 2 + 1] = xy[idx * 2 + 1]; sc[k] = col[idx]; k++;
            sx[k * 2] = xy[idx * 2] + ca * f; sx[k * 2 + 1] = xy[idx * 2 + 1] + sa * f; sc[k] = col[idx] & 0x00FFFFFF; k++;
        }
        sx[k * 2] = sx[0]; sx[k * 2 + 1] = sx[1]; sc[k] = sc[0]; k++; sx[k * 2] = sx[2]; sx[k * 2 + 1] = sx[3]; sc[k] = sc[1];
        g.strip(sx, sc);
    }
    public static void round(float x1, float y1, float x2, float y2, float r, int c) { round(x1, y1, x2, y2, r, c, c); }
    /** soft drop shadow made of a few expanding translucent rounded rects */
    public static void shadow(float x1, float y1, float x2, float y2, float r, float strength) {
        if (!V.shadow || V.lite) return;
        for (int i = 4; i >= 1; i--) round(x1 - i, y1 - i + 1.5f, x2 + i, y2 + i + 1.5f, r + i, alpha(0, (int) (22 * strength / i)));
    }
    /** crystal panel used everywhere in the HUD (SafiMine style): soft accent glow, gradient crystal rim,
     *  dark translucent body with a glassy top sheen and a small crystal in the corner */
    public static void panel(float x1, float y1, float x2, float y2, float r) { panel(x1, y1, x2, y2, r, true); }
    public static void panel(float x1, float y1, float x2, float y2, float r, boolean gem) {
        if (V.hudStyle == 1) { lines(x1, y1, x2, y2); return; }
        if (V.hudStyle == 2) { minimal(x1, y1, x2, y2, r); return; }
        if (V.hudStyle == 3) { glass(x1, y1, x2, y2, r); return; }
        if (V.shadow && !V.lite) for (int i = 3; i >= 1; i--) round(x1 - i, y1 - i, x2 + i, y2 + i, r + i, D.a(V.a1(), 0.10f / i), D.a(V.a2(), 0.10f / i));
        round(x1, y1, x2, y2, r, D.a(bright(V.a1(), 1.1f), 0.85f), D.a(V.a2(), 0.85f));                 // rim
        int body = alpha(0x050B16, (int) (255 * V.bg));
        round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), body);                                   // body
        round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), D.a(V.a1(), 0.07f), D.a(V.a2(), 0.03f)); // tint
        float h = y2 - y1;
        if (h > 8) round(x1 + 2, y1 + 1.5f, x2 - 2, y1 + Math.min(h * 0.42f, 9), Math.max(0, r - 2), alpha(0xFFFFFF, 14)); // glass sheen
        round(x1 + r * 0.6f, y1 + 0.5f, x2 - r * 0.6f, y1 + 1.3f, 0.4f, alpha(0xFFFFFF, 0), alpha(0xFFFFFF, 140));          // highlight line
        if (V.glare && !V.lite) glare(x1, y1, x2, y2);
    }
    /** "Liquid Glass" (like iOS 26): a nearly clear body, a light rim that is bright at the top-left and fades to the bottom-right, a soft shadow */
    static void glass(float x1, float y1, float x2, float y2, float r) {
        r = Math.max(r, Math.min(9, (y2 - y1) / 2));
        if (V.shadow) for (int i = 4; i >= 1; i--) round(x1 - i * 0.4f, y1 + i * 0.7f, x2 + i * 0.4f, y2 + i * 1.1f, r + i, alpha(0x000000, 14 / i));
        round(x1, y1, x2, y2, r, alpha(0x0B1320, (int) (38 + 60 * V.bg)), alpha(0x0B1320, (int) (52 + 70 * V.bg)));       // a light veil: text stays readable on any background
        round(x1, y1, x2, y2, r, alpha(0xFFFFFF, 30), alpha(0xFFFFFF, 10));                                              // clear glass body
        round(x1 + 1.2f, y1 + 1.2f, x2 - 1.2f, y1 + Math.min((y2 - y1) * 0.5f, 14), Math.max(0, r - 1.2f), alpha(0xFFFFFF, 22), alpha(0xFFFFFF, 0)); // top light
        rim(x1, y1, x2, y2, r, 0.9f, 190, 45);                                                                          // specular rim
        rim(x1 + 1.1f, y1 + 1.1f, x2 - 1.1f, y2 - 1.1f, Math.max(0, r - 1.1f), 0.8f, 40, 12);                           // inner refraction line
        if (V.glare) glare(x1, y1, x2, y2);
    }
    /** a rounded outline w thick, its white alpha going from aTop (top-left) to aBottom (bottom-right) */
    static void rim(float x1, float y1, float x2, float y2, float r, float w, int aTop, int aBottom) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)); int n = 6; float[] px = new float[4 * (n + 1)], py = new float[px.length]; int k = 0;
        float[][] cs = {{x2 - r, y1 + r, -90}, {x2 - r, y2 - r, 0}, {x1 + r, y2 - r, 90}, {x1 + r, y1 + r, 180}};
        for (float[] c : cs) for (int i = 0; i <= n; i++) { double a = Math.toRadians(c[2] + 90.0 * i / n); px[k] = c[0] + (float) Math.cos(a) * r; py[k] = c[1] + (float) Math.sin(a) * r; k++; }
        float cx = (x1 + x2) / 2, cy = (y1 + y2) / 2, span = (x2 - x1) + (y2 - y1);
        for (int i = 0; i < k; i++) { int j = (i + 1) % k; float[] o = {px[i], py[i], px[j], py[j]}, in = new float[4];
            for (int q = 0; q < 2; q++) { float dx = cx - o[q * 2], dy = cy - o[q * 2 + 1], l = (float) Math.max(1e-3, Math.sqrt(dx * dx + dy * dy)), rr = Math.max(r, 0.01f);
                float nx = r > 0 ? (cs[Math.min(3, (q == 0 ? i : j) / (n + 1))][0] - o[q * 2]) / rr : dx / l, ny = r > 0 ? (cs[Math.min(3, (q == 0 ? i : j) / (n + 1))][1] - o[q * 2 + 1]) / rr : dy / l;
                in[q * 2] = o[q * 2] + nx * w; in[q * 2 + 1] = o[q * 2 + 1] + ny * w; }
            int ca = alpha(0xFFFFFF, (int) (aTop + (aBottom - aTop) * Math.min(1, Math.max(0, ((o[0] - x1) + (o[1] - y1)) / span)))), cb = alpha(0xFFFFFF, (int) (aTop + (aBottom - aTop) * Math.min(1, Math.max(0, ((o[2] - x1) + (o[3] - y1)) / span))));
            g.poly(new float[]{o[0], o[1], o[2], o[3], in[2], in[3], in[0], in[1]}, new int[]{ca, cb, cb, ca}); }
    }
    /** a soft vertical sheen sliding across the panel every few seconds (all panels in sync by screen x) */
    static void glare(float x1, float y1, float x2, float y2) {
        float t = (System.currentTimeMillis() % 4200) / 1400f; if (t > 1) return;
        float w = x2 - x1, cx = x1 - 14 + (w + 28) * t, a = Math.max(x1, cx - 14), b = Math.min(x2, cx + 14); if (b <= a) return;
        float m = Math.max(a, Math.min(b, cx));
        hgrad(a, y1 + 1, m, y2 - 1, 0x00FFFFFF, 0x22FFFFFF); hgrad(m, y1 + 1, b, y2 - 1, 0x22FFFFFF, 0x00FFFFFF);
    }

    /** "Asteria lines" panel: barely-there backing, a stepped accent line on top (like the asset board) and a tiny diamond */
    /** "Constellation": dark card, gradient bar on the left, a star-chart line on top and a comet running along it */
    public static void lines(float x1, float y1, float x2, float y2) {
        float w = x2 - x1, h = y2 - y1;
        round(x1, y1, x2, y2, 5, alpha(0x07060C, (int) (235 * V.bg)));
        round(x1, y1, x2, y2, 5, D.a(V.a1(), 0.06f), D.a(V.a2(), 0.02f));
        // star chart along the top: three nodes joined by a hairline
        float n1 = x1 + 8, n2 = x1 + Math.min(w * 0.35f, 40), n3 = x2 - 8, ty = y1 + 0.6f;
        hgrad(n1, ty - 0.3f, n3, ty + 0.3f, D.a(V.a2(), 0.7f), D.a(V.a1(), 0.15f));
        float tw = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 500.0 + x1));
        sparkle(n1, ty, 2.2f, D.a(0xFFFFFFFF, 0.9f)); sparkle(n2, ty, 1.6f, D.a(V.a2(), tw)); circle(n3, ty, 0.9f, D.a(V.a1(), 0.8f));
        // comet: a bright dot with a fading tail, once every few seconds
        float t = ((System.currentTimeMillis() + (long) (x1 * 37)) % 3600) / 2200f;
        if (t < 1) { float cx = x1 + 4 + (w - 8) * t; hgrad(Math.max(x1 + 2, cx - 18), ty - 0.5f, cx, ty + 0.5f, D.a(V.a2(), 0f), D.a(0xFFFFFFFF, 0.9f)); circle(cx, ty, 1.1f, 0xFFFFFFFF); }
        if (h > 20) circle(x2 - 4, y2 - 4, 0.7f, D.a(V.a2(), 0.6f));
    }
    /** vertical gradient pill (accent bars) */
    static void vgradRound(float x1, float y1, float x2, float y2, int top, int bottom) {
        float r = (x2 - x1) / 2; if (y2 - y1 < r * 2) return;
        circle(x1 + r, y1 + r, r, top); circle(x1 + r, y2 - r, r, bottom); vgrad(x1, y1 + r, x2, y2 - r, top, bottom);
    }
    /** "Minimal": a calm dark card with a hairline border and a small accent tick (like modern clients) */
    public static void minimal(float x1, float y1, float x2, float y2, float r) {
        r = Math.max(4, r);
        if (V.shadow) shadow(x1, y1, x2, y2, r, 0.8f);
        round(x1, y1, x2, y2, r, alpha(0xFFFFFF, 18));                                              // hairline
        round(x1 + 0.6f, y1 + 0.6f, x2 - 0.6f, y2 - 0.6f, r - 0.6f, alpha(0x0F0D12, (int) (245 * V.bg)));
        round(x1 + 0.6f, y1 + 0.6f, x2 - 0.6f, y2 - 0.6f, r - 0.6f, D.a(V.a1(), 0.05f), D.a(V.a2(), 0.02f));
        if (V.glare) glare(x1, y1, x2, y2);
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
        round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), alpha(0x07111F, (int) (255 * V.bg)));
        if (p > 0.01f) round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), D.a(bright(V.a1(), 1.05f), p * 0.9f), D.a(V.a2(), p * 0.9f));
        if (y2 - y1 > 8) round(x1 + 2, y1 + 1.5f, x2 - 2, y1 + (y2 - y1) * 0.4f, Math.max(0, r - 2), alpha(0xFFFFFF, (int) (16 + 20 * p)));
    }
    /** the soft edge of every shape: about one real screen pixel, whatever the GUI scale (edges looked stair-stepped) */
    static float feather() { return Math.max(0.3f, 1.1f / Math.max(1f, g.pxScale())); }
    public static void circle(float cx, float cy, float rad, int c) { ring(cx, cy, 0, rad, c, c); if (rad > 1.2f) ring(cx, cy, rad, rad + feather(), c, c & 0x00FFFFFF); }
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
        float nx = -dy / l * w / 2, ny = dx / l * w / 2, fx = -dy / l * feather(), fy = dx / l * feather(); int t = c & 0x00FFFFFF;
        g.poly(new float[]{x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny}, new int[]{c, c, c, c});
        // soft sides: the line fades out over a pixel on both sides instead of a hard stair-stepped edge
        g.poly(new float[]{x1 + nx + fx, y1 + ny + fy, x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 + nx + fx, y2 + ny + fy}, new int[]{t, c, c, t});
        g.poly(new float[]{x1 - nx, y1 - ny, x1 - nx - fx, y1 - ny - fy, x2 - nx - fx, y2 - ny - fy, x2 - nx, y2 - ny}, new int[]{c, t, t, c});
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

    /** the AL mark (letters with the orbit, the launcher logo) in a size x size box */
    public static void mark(float x, float y, float size) { g.texture("textures/gui/logo.png", 512, 512, x, y, size, size); }

    // ------------------------------------------------------------------ icons (same set and look as the launcher's category icons)
    private static void path(float x, float y, float k, float w, int c, boolean closed, float... p) {
        for (int i = 0; i + 3 < p.length; i += 2) line(x + p[i] * k, y + p[i + 1] * k, x + p[i + 2] * k, y + p[i + 3] * k, w, c);
        if (closed && p.length >= 4) line(x + p[p.length - 2] * k, y + p[p.length - 1] * k, x + p[0] * k, y + p[1] * k, w, c);
        for (int i = 0; i + 1 < p.length; i += 2) circle(x + p[i] * k, y + p[i + 1] * k, w / 2, c); // round joints
    }
    private static void ringIcon(float x, float y, float k, float cx, float cy, float r, float w, int c) {
        int n = 18; float[] p = new float[n * 2]; for (int i = 0; i < n; i++) { double a = Math.PI * 2 * i / n; p[i * 2] = (float) (cx + Math.cos(a) * r); p[i * 2 + 1] = (float) (cy + Math.sin(a) * r); }
        for (int i = 0; i < n; i++) { int j = (i + 1) % n; line(x + p[i * 2] * k, y + p[i * 2 + 1] * k, x + p[j * 2] * k, y + p[j * 2 + 1] * k, w, c); }
    }
    /** icon "name" in a size x size box at x,y */
    public static void icon(String name, float x, float y, float size, int c) {
        float k = size / 24f, w = Math.max(0.9f, 2f * k);
        switch (name) {
            case "chevL": path(x, y, k, w, c, false, 15, 5, 8, 12, 15, 19); break;
            case "chevR": path(x, y, k, w, c, false, 9, 5, 16, 12, 9, 19); break;
            case "chevU": path(x, y, k, w, c, false, 5, 15, 12, 8, 19, 15); break;
            case "chevD": path(x, y, k, w, c, false, 5, 9, 12, 16, 19, 9); break;
            case "search": ringIcon(x, y, k, 10.5f, 10.5f, 6.5f, w, c); path(x, y, k, w, c, false, 15.5f, 15.5f, 20.5f, 20.5f); break;
            case "play": g.poly(new float[]{x + 7 * k, y + 4 * k, x + 7 * k, y + 20 * k, x + 20 * k, y + 12 * k}, new int[]{c, c, c}); break;
            case "pause": path(x, y, k, w * 1.3f, c, false, 8, 5, 8, 19); path(x, y, k, w * 1.3f, c, false, 16, 5, 16, 19); break;
            case "prev": g.poly(new float[]{x + 18 * k, y + 5 * k, x + 8 * k, y + 12 * k, x + 18 * k, y + 19 * k}, new int[]{c, c, c}); path(x, y, k, w, c, false, 6, 5, 6, 19); break;
            case "next": g.poly(new float[]{x + 6 * k, y + 5 * k, x + 6 * k, y + 19 * k, x + 16 * k, y + 12 * k}, new int[]{c, c, c}); path(x, y, k, w, c, false, 18, 5, 18, 19); break;
            case "keyboard": path(x, y, k, w, c, true, 3, 6, 21, 6, 21, 18, 3, 18); path(x, y, k, w, c, false, 7, 14, 17, 14); circle(x + 7 * k, y + 10 * k, w * 0.6f, c); circle(x + 12 * k, y + 10 * k, w * 0.6f, c); circle(x + 17 * k, y + 10 * k, w * 0.6f, c); break;
            case "grid": path(x, y, k, w, c, true, 4, 4, 10, 4, 10, 10, 4, 10); path(x, y, k, w, c, true, 14, 4, 20, 4, 20, 10, 14, 10); path(x, y, k, w, c, true, 4, 14, 10, 14, 10, 20, 4, 20); path(x, y, k, w, c, true, 14, 14, 20, 14, 20, 20, 14, 20); break;
            case "cube": path(x, y, k, w, c, true, 12, 3, 20, 7.5f, 20, 16.5f, 12, 21, 4, 16.5f, 4, 7.5f); path(x, y, k, w, c, false, 4, 7.5f, 12, 12, 20, 7.5f); path(x, y, k, w, c, false, 12, 12, 12, 21); break;
            case "swords": path(x, y, k, w, c, false, 4, 4, 16, 16); path(x, y, k, w, c, false, 20, 4, 8, 16); path(x, y, k, w, c, false, 13, 19, 19, 13); path(x, y, k, w, c, false, 5, 13, 11, 19); path(x, y, k, w, c, false, 17, 17, 20, 20); path(x, y, k, w, c, false, 7, 17, 4, 20); break;
            case "eye": path(x, y, k, w, c, true, 2, 12, 6, 7.5f, 12, 5.5f, 18, 7.5f, 22, 12, 18, 16.5f, 12, 18.5f, 6, 16.5f); ringIcon(x, y, k, 12, 12, 3, w, c); break;
            case "palette": ringIcon(x, y, k, 12, 12, 8.5f, w, c); circle(x + 8 * k, y + 9 * k, w * 0.9f, c); circle(x + 12.5f * k, y + 7.5f * k, w * 0.9f, c); circle(x + 16 * k, y + 10.5f * k, w * 0.9f, c); circle(x + 9 * k, y + 14 * k, w * 0.9f, c); break;
            case "folder": path(x, y, k, w, c, true, 3, 6, 9, 6, 11, 8.5f, 21, 8.5f, 21, 19, 3, 19); break;
            case "move": path(x, y, k, w, c, false, 12, 3, 12, 21); path(x, y, k, w, c, false, 3, 12, 21, 12); path(x, y, k, w, c, false, 9, 6, 12, 3, 15, 6); path(x, y, k, w, c, false, 9, 18, 12, 21, 15, 18); path(x, y, k, w, c, false, 6, 9, 3, 12, 6, 15); path(x, y, k, w, c, false, 18, 9, 21, 12, 18, 15); break;
            case "music": path(x, y, k, w, c, false, 9, 17, 9, 5, 19, 3, 19, 15); circle(x + 6.5f * k, y + 17 * k, 2.6f * k + w / 2, c); circle(x + 16.5f * k, y + 15 * k, 2.6f * k + w / 2, c); break;
            case "dot": circle(x + 12 * k, y + 12 * k, 3.5f * k, c); break;
            case "heart": circle(x + 8.5f * k, y + 9.5f * k, 4.6f * k, c); circle(x + 15.5f * k, y + 9.5f * k, 4.6f * k, c);
                g.poly(new float[]{x + 4.1f * k, y + 11 * k, x + 12 * k, y + 20.5f * k, x + 19.9f * k, y + 11 * k, x + 12 * k, y + 9 * k}, new int[]{c, c, c, c}); break;
            case "users": ringIcon(x, y, k, 9, 8, 3.5f, w, c); path(x, y, k, w, c, false, 2.5f, 20, 3.5f, 15.5f, 9, 13, 14.5f, 15.5f, 15.5f, 20); ringIcon(x, y, k, 17, 9, 2.8f, w, c); path(x, y, k, w, c, false, 17.5f, 13.5f, 20.5f, 15, 21.5f, 19); break;
            case "gear": ringIcon(x, y, k, 12, 12, 6, w, c); ringIcon(x, y, k, 12, 12, 2.2f, w, c);
                for (int i = 0; i < 8; i++) { double an = Math.PI * i / 4; path(x, y, k, w * 1.4f, c, false, (float) (12 + Math.cos(an) * 6.5), (float) (12 + Math.sin(an) * 6.5), (float) (12 + Math.cos(an) * 9.5), (float) (12 + Math.sin(an) * 9.5)); } break;
            case "exit": path(x, y, k, w, c, false, 14, 4, 5, 4, 5, 20, 14, 20); path(x, y, k, w, c, false, 10, 12, 21, 12); path(x, y, k, w, c, false, 17, 8, 21, 12, 17, 16); break;
            case "globe": ringIcon(x, y, k, 12, 12, 8.5f, w, c); path(x, y, k, w, c, false, 3.5f, 12, 20.5f, 12); path(x, y, k, w, c, false, 12, 3.5f, 9, 8, 8.5f, 12, 9, 16, 12, 20.5f); path(x, y, k, w, c, false, 12, 3.5f, 15, 8, 15.5f, 12, 15, 16, 12, 20.5f); break;
            case "check": path(x, y, k, w * 1.2f, c, false, 5, 12.5f, 10, 17.5f, 19.5f, 6.5f); break;
            case "back": path(x, y, k, w, c, false, 20, 12, 5, 12); path(x, y, k, w, c, false, 11, 6, 5, 12, 11, 18); break;
            case "plus": path(x, y, k, w * 1.2f, c, false, 12, 5, 12, 19); path(x, y, k, w * 1.2f, c, false, 5, 12, 19, 12); break;
            case "close": path(x, y, k, w * 1.2f, c, false, 6, 6, 18, 18); path(x, y, k, w * 1.2f, c, false, 18, 6, 6, 18); break;
            default: circle(x + 12 * k, y + 12 * k, 4 * k, c);
        }
    }

    // ------------------------------------------------------------------ animation
    private static final java.util.Map<String, float[]> AN = new java.util.HashMap<>();
    private static long lastFrame = System.nanoTime(); public static float dt = 0.016f;
    /** once per rendered frame. The HUD and the menu both call it in the same frame: a second call within 4 ms keeps the
     *  frame's dt (otherwise the menu got dt ~ 0 and every animation in it crawled) */
    public static void frame() { long n = System.nanoTime(); if (n - lastFrame < 4_000_000L) return; dt = Math.min(0.1f, (n - lastFrame) / 1e9f); lastFrame = n; }
    /** smoothly moves a named value towards target (exponential), speed ~ 1/seconds */
    public static float anim(String key, float target, float speed) {
        float[] v = AN.get(key); if (v == null) { AN.put(key, new float[]{target}); return target; }
        // 2x faster than the nominal speed and snaps once close, so nothing lingers in a long exponential tail
        v[0] += (target - v[0]) * Math.min(1f, dt * speed * 2); if (Math.abs(target - v[0]) < (Math.abs(target) > 2 ? 0.4f : 0.015f)) v[0] = target; return v[0];
    }
    public static void set(String key, float value) { AN.put(key, new float[]{value}); }
    public static float ease(float t) { t = Math.max(0, Math.min(1, t)); return 1 - (float) Math.pow(1 - t, 3); }
}
