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
        /** additive glowing textured quads: then 4 vt() per quad, then end(). texture = path in asteria_visuals:textures/fx/ without .png */
        void beginTex(String texture, boolean depth);
        void vt(double x, double y, double z, float u, float v, int argb);
        double[] cam();
        float camYaw();
        float camPitch();
        float partial();
    }

    // ------------------------------------------------------------------ target
    private static Object target; private static long targetAt;
    public static void onAttack(Game g, Object e) {
        if (!g.living(e)) return;
        target = e; targetAt = System.currentTimeMillis(); game = g;
        if (V.on("hitparticles")) burst(g, e);
        if (V.on("sounds") && V.mode("sounds", "hit") > 0) sound(HIT_SOUNDS[V.mode("sounds", "hit") - 1], (float) V.num("sounds", "volume"));
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
    private static final List<double[]> JPARTS = new ArrayList<>();  // jump particles: x,y,z,vx,vy,vz,born,life,seed
    private static final List<double[]> PTRAIL = new ArrayList<>();  // walking particle trail: same layout
    private static final List<double[]> SONAR = new ArrayList<>();   // x,y,z,start
    private static final List<double[]> DMG = new ArrayList<>();     // floating damage numbers: x,y,z,born,value,drift
    private static Object dmgEnt; private static float dmgHp = -1; private static long sonarAt;
    private static double[] lastStep;
    private static final List<double[]> DASH = new ArrayList<>();    // sprint-start streaks: x,y,z,dx,dz,start
    private static boolean wasSprint; private static double[] petPos, petPrev;
    /** pets of other launcher players: name -> {kind, size}; from <api>/cosmetics (the launcher sends each player's pet with its heartbeat) */
    private static volatile java.util.Map<String, double[]> PET_OF = new java.util.HashMap<>();
    private static final java.util.Map<String, double[]> OTHER = new java.util.HashMap<>(); // name -> prev x,y,z, cur x,y,z
    private static long petsAt;
    private static void fetchPets() {
        final String api = System.getProperty("asteria.api"); if (api == null || api.isEmpty()) return;
        Thread t = new Thread(() -> {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(api + "/cosmetics").openConnection();
                c.setConnectTimeout(5000); c.setReadTimeout(8000); c.setRequestProperty("User-Agent", "AsteriaVisuals");
                if (c.getResponseCode() != 200) return;
                String body; try (java.io.InputStream in = c.getInputStream()) { java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); byte[] b = new byte[8192]; int r; while ((r = in.read(b)) > 0) o.write(b, 0, r); body = new String(o.toByteArray(), java.nio.charset.StandardCharsets.UTF_8); }
                com.google.gson.JsonObject all = new com.google.gson.JsonParser().parse(body).getAsJsonObject();
                java.util.Map<String, double[]> m = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> en : all.entrySet()) {
                    com.google.gson.JsonObject v = en.getValue().getAsJsonObject(); String p = v.has("pet") ? v.get("pet").getAsString() : "";
                    int i = p.indexOf(':'); if (i <= 0) continue;
                    try { m.put(en.getKey().toLowerCase(), new double[]{Integer.parseInt(p.substring(0, i)), Double.parseDouble(p.substring(i + 1))}); } catch (Exception ignored) {}
                }
                PET_OF = m;
            } catch (Throwable ignored) {}
        }, "asteria-pets"); t.setDaemon(true); t.start();
    }
    static final String[] PETS = {"trinket/cat","trinket/ghost","trinket/flame","trinket/gem","trinket/atom","trinket/flower","trinket/donut","trinket/ball","trinket/banana"};

    public static void tick(Game g) {
        game = g;
        Object self = g.self(); if (self == null) { TRAIL.clear(); return; }
        double[] p = g.pos(self, 1);
        long now0 = System.currentTimeMillis();
        // kill effect: the entity we hit last died within a few seconds
        if (target != null) {
            if (g.alive(target)) { targetPos = g.pos(target, 1); targetH = g.height(target); }
            else { if (V.on("killeffect") && targetPos != null && now0 - targetAt < 5000) kill(targetPos, targetH, now0);
                if (V.on("sounds") && V.mode("sounds", "kill") > 0 && now0 - targetAt < 5000) sound(KILL_SOUNDS[V.mode("sounds", "kill") - 1], (float) V.num("sounds", "volume"));
                target = null; }
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
        if (ground != wasGround) {
            boolean jump = !ground; // left the ground = jump, touched it = landing
            if (V.on("jumpparticles") && (jump || V.bool("jumpparticles", "land"))) jumpBurst(p, jump, now0);
            if (V.on("sonar") && jump && V.mode("sonar", "trigger") != 1) SONAR.add(new double[]{p[0], p[1], p[2], now0});
        }
        if (V.on("sonar") && V.mode("sonar", "trigger") != 0 && now0 - sonarAt > V.num("sonar", "interval") * 1000) { sonarAt = now0; SONAR.add(new double[]{p[0], p[1], p[2], now0}); }
        SONAR.removeIf(q -> now0 - q[3] > V.num("sonar", "time") * 1000); // the totem effect also adds waves, even with "Sonar" off
        wasGround = ground;
        // dash: a short streak when sprinting starts
        boolean spr = g.sprinting();
        if (V.on("dash") && spr && !wasSprint) { double yr = Math.toRadians(g.yaw()); DASH.add(new double[]{p[0], p[1], p[2], -Math.sin(yr), Math.cos(yr), now0}); }
        wasSprint = spr; DASH.removeIf(q -> now0 - q[5] > 600);
        // pet follows a spot behind the right shoulder
        if (V.on("pet")) { double yr = Math.toRadians(g.yaw()), tx = p[0] - Math.sin(yr) * -0.2 + Math.cos(yr) * -0.75, tz = p[2] + Math.cos(yr) * -0.2 + Math.sin(yr) * -0.75, ty = p[1] + 1.9;
            if (petPos == null || dist(petPos, new double[]{tx, ty, tz}) > 12) petPos = new double[]{tx, ty, tz};
            petPrev = petPos.clone(); // drawn between petPrev and petPos with the partial tick: smooth, not 20 jumps a second
            petPos[0] += (tx - petPos[0]) * 0.18; petPos[1] += (ty - petPos[1]) * 0.18; petPos[2] += (tz - petPos[2]) * 0.18; } else petPos = null;
        // pets of other Asteria players (refreshed every 30 s)
        if (now0 - petsAt > 30000) { petsAt = now0; fetchPets(); }
        if (V.bool("pet", "others") && !PET_OF.isEmpty()) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Object e : g.players()) { String n = g.playerName(e).toLowerCase(); if (!PET_OF.containsKey(n)) continue; seen.add(n);
                double[] q = g.pos(e, 1); double yr = Math.toRadians(g.yawOf(e)), tx = q[0] + 0.2 * Math.sin(yr) - 0.75 * Math.cos(yr), tz = q[2] - 0.2 * Math.cos(yr) - 0.75 * Math.sin(yr), ty = q[1] + 1.9;
                double[] st = OTHER.get(n);
                if (st == null || Math.abs(st[3] - tx) + Math.abs(st[5] - tz) > 12) st = new double[]{tx, ty, tz, tx, ty, tz};
                st[0] = st[3]; st[1] = st[4]; st[2] = st[5];
                st[3] += (tx - st[3]) * 0.18; st[4] += (ty - st[4]) * 0.18; st[5] += (tz - st[5]) * 0.18; OTHER.put(n, st); }
            OTHER.keySet().retainAll(seen);
        } else OTHER.clear();
        // particles left behind while moving
        if (V.on("particletrail")) {
            if (lastStep == null || dist(lastStep, p) > 0.9 / V.num("particletrail", "density")) { lastStep = p.clone();
                for (int i = 0; i < 2; i++) PTRAIL.add(new double[]{p[0] + (Math.random() - .5) * .5, p[1] + Math.random() * .35, p[2] + (Math.random() - .5) * .5,
                    (Math.random() - .5) * .01, .004 + Math.random() * .012, (Math.random() - .5) * .01, now0, V.num("particletrail", "life") * 1000 * (0.7 + Math.random() * 0.6), Math.random()}); }
        } else { PTRAIL.clear(); lastStep = null; }
        for (double[] q : PTRAIL) { q[0] += q[3]; q[1] += q[4]; q[2] += q[5]; }
        PTRAIL.removeIf(q -> now0 - q[6] > q[7]);
        for (double[] q : JPARTS) { q[3] *= 0.9; q[5] *= 0.9; q[4] = q[4] * 0.9 - 0.01; q[0] += q[3]; q[1] += q[4]; q[2] += q[5]; }
        JPARTS.removeIf(q -> now0 - q[6] > q[7]);
        // damage numbers: health drop of the entity we fight
        Object dt = target(g);
        if (V.on("damagenumbers") && dt != null && dt != self) {
            float hp = g.hp(dt);
            if (dt == dmgEnt && dmgHp - hp > 0.05f) { double[] tp = g.pos(dt, 1); double a = Math.random() * Math.PI * 2;
                DMG.add(new double[]{tp[0] + Math.cos(a) * 0.35, tp[1] + g.height(dt) + 0.15, tp[2] + Math.sin(a) * 0.35, now0, dmgHp - hp, Math.random()}); }
            dmgEnt = dt; dmgHp = hp;
        } else { dmgEnt = null; dmgHp = -1; }
        DMG.removeIf(q -> now0 - q[3] > 1200);
        for (double[] q : PARTS) { q[3] *= 0.92; q[5] *= 0.92; q[4] = q[4] * 0.92 - 0.012; q[0] += q[3]; q[1] += q[4]; q[2] += q[5]; }
        long now = System.currentTimeMillis();
        PARTS.removeIf(q -> now - q[6] > q[7]);
    }
    /** textured particle styles (index - 5) and sounds, files come from resources/assets/asteria_visuals */
    static final String[] TEX = {"particles/bloom/bloom_soft","particles/firefly","particles/star","particles/heart","particles/snowflake","particles/coron","particles/flame","particles/blink","particles/geometric","particles/dollar","particles/glyph_alt/flower"};
    static final String[] HIT_SOUNDS = {"pop","bubble","blip","bell","glass","bonk","squish","plastic_ball","magic_pok","8bit","aimbooster_hit","trinket_hit","drum_bass","brick","get"}, KILL_SOUNDS = {"kill.whii","kill.neptune","kill.critow","kill.final_blink","kill.final_pok","kill.abmiss","kill.rust_headshot"};
    private static Game game;
    /** camera-facing textured quad */
    static void tq(R3 r, double x, double y, double z, double s, double ang, int c) {
        double[] rt = right(r), up = up(r); double ca = Math.cos(ang) * s, sa = Math.sin(ang) * s;
        double[] ax = {rt[0] * ca + up[0] * sa, rt[1] * ca + up[1] * sa, rt[2] * ca + up[2] * sa}, ay = {-rt[0] * sa + up[0] * ca, -rt[1] * sa + up[1] * ca, -rt[2] * sa + up[2] * ca};
        r.vt(x - ax[0] - ay[0], y - ax[1] - ay[1], z - ax[2] - ay[2], 0, 1, c); r.vt(x - ax[0] + ay[0], y - ax[1] + ay[1], z - ax[2] + ay[2], 0, 0, c);
        r.vt(x + ax[0] + ay[0], y + ax[1] + ay[1], z + ax[2] + ay[2], 1, 0, c); r.vt(x + ax[0] - ay[0], y + ax[1] - ay[1], z + ax[2] - ay[2], 1, 1, c);
    }
    /** a list of particles drawn with one texture + a soft glow under each */
    private static void texParticles(R3 r, List<double[]> list, int tex, double size, long now, float pt, boolean grow) {
        for (int pass = 0; pass < 2; pass++) {
            r.beginTex(pass == 0 ? "particles/bloom/bloom_small" : TEX[Math.min(TEX.length - 1, tex)], true);
            for (double[] q : list) { float life = (float) ((now - q[6]) / q[7]), a = Math.min(1, Math.min(life * 6, (1 - life) * 1.8f)); int c = V.grad((float) q[8]);
                double x = q[0] + q[3] * pt, y = q[1] + q[4] * pt, z = q[2] + q[5] * pt, s = size * (0.7 + q[8] * 0.6) * (grow ? 1 + life * 0.5 : 1 - life * 0.3);
                if (pass == 0) tq(r, x, y, z, s * 2.6, 0, D.a(c, a * 0.35f)); else tq(r, x, y, z, s, q[8] * 6.28 + life * 2, D.a(D.mix(c, 0xFFFFFFFF, 0.25f), a)); }
            r.end();
        }
    }
    public static void sound(String id, float vol) { if (game != null) try { game.sound(id, vol, 0.95f + (float) Math.random() * 0.1f); } catch (Throwable ignored) {} }
    private static void jumpBurst(double[] p, boolean jump, long now) {
        int n = (int) V.num("jumpparticles", "count");
        for (int i = 0; i < n; i++) { double a = Math.PI * 2 * i / n + Math.random() * 0.3, sp = 0.05 + Math.random() * 0.08;
            JPARTS.add(new double[]{p[0], p[1] + 0.05, p[2], Math.cos(a) * sp, jump ? 0.03 + Math.random() * 0.08 : 0.08 + Math.random() * 0.1, Math.sin(a) * sp, now, 700 + Math.random() * 600, Math.random()}); }
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
        if (!PARTS.isEmpty()) { if (V.mode("hitparticles", "style") >= 5) texParticles(r, PARTS, V.mode("hitparticles", "style") - 5, 0.16, now, pt, false); else particles(r, now); }
        if (!KPARTS.isEmpty()) { r.begin(R3.QUADS, true); for (double[] q : KPARTS) { float life = (float) ((now - q[6]) / q[7]), al = 1 - life; int c = D.a(V.grad((float) q[8]), al);
            double x = q[0] + q[3] * pt, y = q[1] + q[4] * pt, z = q[2] + q[5] * pt; shard(r, x, y, z, 0.16 * (0.6 + q[8]), q[8] * 6.28 + life * 4, c, D.a(0xFFFFFFFF, al)); } r.end(); }
        if (!PILLARS.isEmpty()) pillars(r, now);
        if (!AMB.isEmpty()) { if (V.mode("ambient", "style") >= 3) texParticles(r, AMB, V.mode("ambient", "style") - 3, 0.09, now, pt, false); else ambient(r, now, pt); }
        if (V.on("blockhighlight")) { double[] bb = g.targetBlock(); if (bb != null) blockBox(r, bb, now); }
        if (!JPARTS.isEmpty()) shapes(r, JPARTS, V.mode("jumpparticles", "style"), 0.06, now, pt);
        if (!PTRAIL.isEmpty()) shapes(r, PTRAIL, V.mode("particletrail", "style"), 0.045, now, pt);
        if (!SONAR.isEmpty()) sonar(r, now);
        if (V.on("halo") && g.thirdPerson()) halo(r, g, self, pt, now);
        if (!DMG.isEmpty()) damage(r, now);
        if (!DASH.isEmpty()) dash(r, now);
        if (petPos != null && (g.thirdPerson() || !V.bool("pet", "third"))) pet(r, now);
        if (!OTHER.isEmpty()) { float pf = r.partial();
            for (java.util.Map.Entry<String, double[]> en : OTHER.entrySet()) { double[] st = en.getValue(), k = PET_OF.get(en.getKey()); if (k == null) continue;
                drawPet(r, new double[]{st[0] + (st[3] - st[0]) * pf, st[1] + (st[4] - st[1]) * pf, st[2] + (st[5] - st[2]) * pf}, (int) k[0], k[1], now + en.getKey().hashCode()); } }
    }

    /** small particle shapes shared by jump particles and the particle trail: 0 sparks, 1 stars, 2 hearts, 3 snow, 4 bubbles */
    private static void shapes(R3 r, List<double[]> list, int style, double size, long now, float pt) {
        if (style >= 5) { texParticles(r, list, style - 5, size * 2.2, now, pt, false); return; }
        r.begin(R3.QUADS, true);
        for (double[] q : list) {
            float life = (float) ((now - q[6]) / q[7]), a = Math.min(1, (1 - life) * 1.6f); int c = D.a(V.grad((float) q[8]), a);
            double x = q[0] + q[3] * pt, y = q[1] + q[4] * pt, z = q[2] + q[5] * pt, s = size * (0.6 + q[8] * 0.7) * (style == 4 ? 1 + life : 1 - life * 0.4);
            if (style == 1) starBillboard(r, x, y, z, s * 1.6, c);
            else if (style == 2) { billboard(r, x - s * 0.5, y + s * 0.4, z, s * 0.6, c); billboard(r, x + s * 0.5, y + s * 0.4, z, s * 0.6, c); billboard(r, x, y - s * 0.3, z, s * 0.7, c); }
            else if (style == 3) { billboard(r, x, y, z, s * 0.45, D.a(0xFFF0F9FF, a)); starBillboard(r, x, y, z, s * 1.1, D.a(0xFFE0F2FE, a * 0.7f)); }
            else if (style == 4) bubble(r, x, y, z, s * 1.4, c);
            else { billboard(r, x, y, z, s * 0.3, D.mix(c, D.a(0xFFFFFFFF, a), 0.6f)); billboard(r, x, y, z, s * 0.8, D.a(c, a * 0.25f)); }
        }
        r.end();
    }
    /** expanding scan wave: a bright ring on the ground with a short fading wall above it */
    private static void sonar(R3 r, long now) {
        double life = V.num("sonar", "time") * 1000, R = V.num("sonar", "radius"); int seg = 96;
        for (double[] q : SONAR) {
            double t = (now - q[3]) / life; if (t >= 1) continue;
            double rad = 0.3 + R * (1 - Math.pow(1 - t, 2)), wall = 0.5 * (1 - t); float a = (float) Math.min(1, (1 - t) * 1.4);
            r.begin(R3.STRIP, true);
            for (int i = 0; i <= seg; i++) { double ang = Math.PI * 2 * i / seg, cx = q[0] + Math.cos(ang) * rad, cz = q[2] + Math.sin(ang) * rad; int c = V.grad(i / (float) seg);
                r.v(cx, q[1] + 0.03, cz, D.a(c, a * 0.8f)); r.v(cx, q[1] + 0.03 + wall, cz, D.a(c, 0)); }
            r.end();
            r.begin(R3.STRIP, true);
            for (int i = 0; i <= seg; i++) { double ang = Math.PI * 2 * i / seg; int c = D.mix(V.grad(i / (float) seg), 0xFFFFFFFF, 0.4f);
                r.v(q[0] + Math.cos(ang) * rad, q[1] + 0.025, q[2] + Math.sin(ang) * rad, D.a(c, a));
                r.v(q[0] + Math.cos(ang) * (rad - 0.6), q[1] + 0.025, q[2] + Math.sin(ang) * (rad - 0.6), D.a(c, 0)); }
            r.end();
        }
    }
    /** our block outline: gradient edges and an optional soft fill, slightly inflated so it never z-fights */
    private static void blockBox(R3 r, double[] b, long now) {
        double e = 0.003, x0 = b[0] - e, y0 = b[1] - e, z0 = b[2] - e, x1 = b[3] + e, y1 = b[4] + e, z1 = b[5] + e; int st = V.mode("blockhighlight", "style");
        float pulse = V.bool("blockhighlight", "pulse") ? 0.75f + 0.25f * (float) Math.sin(now / 260.0) : 1;
        if (st != 0) { float fa = (float) V.num("blockhighlight", "fill") * pulse; int c1 = D.a(V.a1(), fa), c2 = D.a(V.a2(), fa);
            r.begin(R3.QUADS, true);
            r.v(x0, y0, z0, c1); r.v(x1, y0, z0, c2); r.v(x1, y0, z1, c1); r.v(x0, y0, z1, c2); // bottom
            r.v(x0, y1, z0, c1); r.v(x0, y1, z1, c2); r.v(x1, y1, z1, c1); r.v(x1, y1, z0, c2); // top
            r.v(x0, y0, z0, c1); r.v(x0, y1, z0, c2); r.v(x1, y1, z0, c1); r.v(x1, y0, z0, c2);
            r.v(x0, y0, z1, c1); r.v(x1, y0, z1, c2); r.v(x1, y1, z1, c1); r.v(x0, y1, z1, c2);
            r.v(x0, y0, z0, c1); r.v(x0, y0, z1, c2); r.v(x0, y1, z1, c1); r.v(x0, y1, z0, c2);
            r.v(x1, y0, z0, c1); r.v(x1, y1, z0, c2); r.v(x1, y1, z1, c1); r.v(x1, y0, z1, c2);
            r.end(); }
        if (st != 1) { double[][] ed = {{x0,y0,z0,x1,y0,z0},{x1,y0,z0,x1,y0,z1},{x1,y0,z1,x0,y0,z1},{x0,y0,z1,x0,y0,z0},{x0,y1,z0,x1,y1,z0},{x1,y1,z0,x1,y1,z1},{x1,y1,z1,x0,y1,z1},{x0,y1,z1,x0,y1,z0},{x0,y0,z0,x0,y1,z0},{x1,y0,z0,x1,y1,z0},{x1,y0,z1,x1,y1,z1},{x0,y0,z1,x0,y1,z1}};
            r.begin(R3.LINES, true);
            for (int i = 0; i < ed.length; i++) { double[] q = ed[i]; r.v(q[0], q[1], q[2], D.a(V.grad(i / 12f), pulse)); r.v(q[3], q[4], q[5], D.a(V.grad(i / 12f + 0.3f), pulse)); }
            r.end(); }
    }
    private static void dash(R3 r, long now) {
        r.begin(R3.QUADS, true);
        for (double[] q : DASH) { float t = (now - (long) q[5]) / 600f, a = 1 - t;
            for (int k = 0; k < 5; k++) { double off = (k - 2) * 0.18, sx = q[0] + q[4] * off * -1, sz = q[2] + q[3] * off, len = 1.6 * (0.6 + 0.4 * Math.sin(k * 1.7)), back = 0.3 + t * 1.2;
                double x0 = sx - q[3] * back, z0 = sz - q[4] * back, x1 = x0 - q[3] * len, z1 = z0 - q[4] * len, y = q[1] + 0.25 + k * 0.28; int c = V.grad(k / 5f);
                r.v(x0, y - 0.02, z0, D.a(c, a * 0.85f)); r.v(x0, y + 0.02, z0, D.a(c, a * 0.85f)); r.v(x1, y + 0.02, z1, D.a(c, 0)); r.v(x1, y - 0.02, z1, D.a(c, 0)); } }
        r.end();
    }
    /** a little glowing companion: a soft glow plus its picture, bobbing */
    private static void pet(R3 r, long now) {
        float t = r.partial(); double[] pp = petPrev == null ? petPos : new double[]{petPrev[0] + (petPos[0] - petPrev[0]) * t, petPrev[1] + (petPos[1] - petPrev[1]) * t, petPrev[2] + (petPos[2] - petPrev[2]) * t};
        drawPet(r, pp, V.mode("pet", "kind"), V.num("pet", "size"), now);
    }
    static void drawPet(R3 r, double[] pp, int kind, double size, long now) {
        double bob = Math.sin(now / 420.0) * 0.08, s = 0.22 * size; int c = V.grad(0.2f);
        r.beginTex("particles/bloom/bloom_soft", true); tq(r, pp[0], pp[1] + bob, pp[2], s * 2.6, 0, D.a(c, 0.5f)); r.end();
        r.beginTex(PETS[Math.max(0, Math.min(PETS.length - 1, kind))], true); tq(r, pp[0], pp[1] + bob, pp[2], s, Math.sin(now / 900.0) * 0.15, 0xFFFFFFFF); r.end();
    }
    /** glowing ring floating above the head, slowly turning and bobbing */
    private static void halo(R3 r, Game g, Object self, float pt, long now) {
        double[] p = g.pos(self, pt); double rad = V.num("halo", "size"), y = p[1] + g.height(self) + V.num("halo", "height") + Math.sin(now / 700.0) * 0.03;
        double spin = now / 1000.0 * V.num("halo", "spin"), w = rad * 0.22; int seg = 48;
        r.begin(R3.STRIP, true);
        for (int i = 0; i <= seg; i++) { double a = spin + Math.PI * 2 * i / seg; int c = V.grad(i / (float) seg);
            r.v(p[0] + Math.cos(a) * (rad + w), y, p[2] + Math.sin(a) * (rad + w), D.a(c, 0)); r.v(p[0] + Math.cos(a) * rad, y, p[2] + Math.sin(a) * rad, D.a(c, 0.95f)); }
        r.end();
        r.begin(R3.STRIP, true);
        for (int i = 0; i <= seg; i++) { double a = spin + Math.PI * 2 * i / seg; int c = V.grad(i / (float) seg);
            r.v(p[0] + Math.cos(a) * rad, y, p[2] + Math.sin(a) * rad, D.a(D.mix(c, 0xFFFFFFFF, 0.5f), 0.95f)); r.v(p[0] + Math.cos(a) * (rad - w), y, p[2] + Math.sin(a) * (rad - w), D.a(c, 0)); }
        r.end();
    }
    // 7-segment digits drawn as camera-facing quads: segments a b c d e f g
    private static final int[] SEG = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
    private static void damage(R3 r, long now) {
        double[] rt = right(r), up = up(r); double k = V.num("damagenumbers", "size");
        r.begin(R3.QUADS, false);
        for (double[] q : DMG) {
            float t = (now - (long) q[3]) / 1200f, a = t < 0.7f ? 1 : (1 - t) / 0.3f; double y = q[1] + t * 0.6, h = 0.24 * k * (t < 0.15 ? 0.6 + t / 0.15 * 0.4 : 1), w = h * 0.55, th = h * 0.13;
            String txt = q[4] >= 10 || !V.bool("damagenumbers", "decimals") ? String.valueOf(Math.round(q[4])) : String.format(java.util.Locale.ROOT, "%.1f", q[4]);
            double full = txt.length() * (w + th * 1.6), cx = -full / 2; int col = q[4] >= 6 ? 0xFFFF5470 : q[4] >= 3 ? 0xFFFFB547 : V.a1();
            for (char ch : txt.toCharArray()) {
                if (ch == '.') { segQ(r, rt, up, q[0], y, q[2], cx, 0, cx + th, th, D.a(col, a)); cx += th * 2.2; continue; }
                int m = SEG[ch - '0'];
                double[][] sg = {{0, h - th, w, h}, {w - th, h / 2, w, h}, {w - th, 0, w, h / 2}, {0, 0, w, th}, {0, 0, th, h / 2}, {0, h / 2, th, h}, {0, h / 2 - th / 2, w, h / 2 + th / 2}};
                for (int i = 0; i < 7; i++) if ((m >> i & 1) != 0) { double[] b = sg[i];
                    segQ(r, rt, up, q[0], y, q[2], cx + b[0] - th * 0.4, b[1] - th * 0.4, cx + b[2] + th * 0.4, b[3] + th * 0.4, D.a(0xFF000000, a * 0.45f)); // outline
                    segQ(r, rt, up, q[0], y, q[2], cx + b[0], b[1], cx + b[2], b[3], D.a(D.mix(col, 0xFFFFFFFF, (float) (b[1] / h) * 0.5f), a)); }
                cx += w + th * 1.6;
            }
        }
        r.end();
    }
    private static void segQ(R3 r, double[] rt, double[] up, double x, double y, double z, double x0, double y0, double x1, double y1, int c) {
        r.v(x + rt[0] * x0 + up[0] * y0, y + rt[1] * x0 + up[1] * y0, z + rt[2] * x0 + up[2] * y0, c);
        r.v(x + rt[0] * x0 + up[0] * y1, y + rt[1] * x0 + up[1] * y1, z + rt[2] * x0 + up[2] * y1, c);
        r.v(x + rt[0] * x1 + up[0] * y1, y + rt[1] * x1 + up[1] * y1, z + rt[2] * x1 + up[2] * y1, c);
        r.v(x + rt[0] * x1 + up[0] * y0, y + rt[1] * x1 + up[1] * y0, z + rt[2] * x1 + up[2] * y0, c);
    }

    private static void trail(R3 r, Game g, Object self, float pt) {
        List<double[]> pts = new ArrayList<>(TRAIL); pts.add(g.pos(self, pt));
        int n = pts.size(); boolean ribbon = V.mode("trails", "mode") == 0; double h = V.num("trails", "height");
        if (V.mode("trails", "mode") == 2) { // spiral: two twisting ribbons around the path
            double t0 = System.currentTimeMillis() / 300.0;
            for (int k = 0; k < 2; k++) { r.begin(R3.STRIP, true);
                for (int i = 0; i < n; i++) { double[] p = pts.get(i); float f = i / (float) (n - 1); int c = V.grad(f * 0.6f + k * 0.5f); double a = t0 + i * 0.45 + k * Math.PI, ox = Math.cos(a) * 0.35, oz = Math.sin(a) * 0.35, yy = p[1] + h * 0.5 + Math.sin(a) * 0.15;
                    r.v(p[0] + ox, yy, p[2] + oz, D.a(c, 0.8f * f)); r.v(p[0] + ox, yy + 0.08, p[2] + oz, D.a(c, 0.15f * f)); }
                r.end(); }
            return;
        }
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
            int style = V.mode("jumpcircles", "style"); double spin = (now - c[3]) / 1000.0 * V.num("jumpcircles", "spin");
            if (style >= 3) { // textured ring lying on the ground
                String[] ct = {"circles/circle", "circles/circle_bold", "circles/portal"}; r.beginTex(ct[Math.min(2, style - 3)], true);
                double rr = rad * 1.15, cs = Math.cos(spin) * rr, sn = Math.sin(spin) * rr; int col = D.a(V.grad((float) t), a);
                r.vt(c[0] - cs + sn, c[1] + 0.02, c[2] - sn - cs, 0, 0, col); r.vt(c[0] + cs + sn, c[1] + 0.02, c[2] + sn - cs, 1, 0, col);
                r.vt(c[0] + cs - sn, c[1] + 0.02, c[2] + sn + cs, 1, 1, col); r.vt(c[0] - cs - sn, c[1] + 0.02, c[2] - sn + cs, 0, 1, col);
                r.end(); continue;
            }
            int rings = style == 1 ? 2 : 1;
            for (int k = 0; k < rings; k++) { double rr = rad * (k == 0 ? 1 : 0.6);
                r.begin(R3.STRIP, true);
                int seg = style == 2 ? 10 : 48; // style 2: a ten-point star outline
                for (int i = 0; i <= seg; i++) { double ang = spin + Math.PI * 2 * i / seg, rx = style == 2 ? rr * (i % 2 == 0 ? 1 : 0.55) : rr; int col = V.grad(i / (float) seg + k * 0.3f);
                    r.v(c[0] + Math.cos(ang) * rx, c[1] + 0.02, c[2] + Math.sin(ang) * rx, D.a(col, a * 0.9f));
                    r.v(c[0] + Math.cos(ang) * (rx - w), c[1] + 0.02, c[2] + Math.sin(ang) * (rx - w), D.a(col, 0)); }
                r.end(); }
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
        } else if (V.mode("targetesp", "style") == 2) { // spiral: two helixes of glowing dots climbing around the target
            r.beginTex("particles/bloom/bloom_small", true);
            for (int k = 0; k < 2; k++) for (int j = 0; j < 22; j++) { double a = now / 1000.0 * sp * 3 + j * 0.45 + k * Math.PI, yy = p[1] + h * (j / 22.0);
                tq(r, p[0] + Math.cos(a) * rad, yy, p[2] + Math.sin(a) * rad, 0.12, 0, D.a(V.grad(j / 22f + k * 0.5f), 0.9f * (0.4f + 0.6f * j / 22f))); }
            r.end();
        } else if (V.mode("targetesp", "style") == 3) { // portal: a turning textured circle under the feet + a soft column
            double rr = rad * 1.6, a = now / 1000.0 * sp * 1.5, cs = Math.cos(a) * rr, sn = Math.sin(a) * rr; int col = D.a(V.grad((float) phase), 0.9f);
            r.beginTex("circles/portal", true);
            r.vt(p[0] - cs + sn, p[1] + 0.03, p[2] - sn - cs, 0, 0, col); r.vt(p[0] + cs + sn, p[1] + 0.03, p[2] + sn - cs, 1, 0, col);
            r.vt(p[0] + cs - sn, p[1] + 0.03, p[2] + sn + cs, 1, 1, col); r.vt(p[0] - cs - sn, p[1] + 0.03, p[2] - sn + cs, 0, 1, col);
            r.end();
            r.begin(R3.STRIP, true);
            for (int i = 0; i <= seg; i++) { double an = Math.PI * 2 * i / seg; int c = V.grad(i / (float) seg);
                r.v(p[0] + Math.cos(an) * rad, p[1], p[2] + Math.sin(an) * rad, D.a(c, 0.4f)); r.v(p[0] + Math.cos(an) * rad, p[1] + h * 0.6, p[2] + Math.sin(an) * rad, D.a(c, 0)); }
            r.end();
        } else if (V.mode("targetesp", "style") == 4) { // stars: textured stars orbiting at three heights
            r.beginTex("particles/star", true);
            for (int k = 0; k < 3; k++) for (int j = 0; j < 4; j++) { double a = now / 1000.0 * sp * (k % 2 == 0 ? 2 : -2) + j * Math.PI / 2 + k;
                tq(r, p[0] + Math.cos(a) * rad, p[1] + h * (0.25 + k * 0.3), p[2] + Math.sin(a) * rad, 0.14, a, D.a(V.grad(k / 3f + j * 0.1f), 0.95f)); }
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
        double[] p = g.pos(self, pt); double top = p[1] + g.height(self) - 0.05, rad = V.num("chinahat", "size"); float a = (float) V.num("chinahat", "alpha");
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

    // ------------------------------------------------------------------ wave 4 hooks called from the version mixins
    /** sky tint: mixes the vanilla sky colour (0xRRGGBB) towards the chosen colour */
    public static int sky(int rgb) {
        if (!V.on("skycolor")) return rgb;
        return D.mix(0xFF000000 | rgb, V.color("skycolor", "color"), (float) V.num("skycolor", "strength")) & 0xFFFFFF;
    }
    private static final float[] RATIOS = {4 / 3f, 5 / 4f, 16 / 10f, 1f};
    /** horizontal stretch of the projection for "stretched resolution" (1 = off) */
    public static float stretch(int w, int h) {
        if (!V.on("aspect") || h <= 0) return 1;
        return (w / (float) h) / RATIOS[Math.min(RATIOS.length - 1, V.mode("aspect", "ratio"))];
    }
    /** third person camera distance */
    public static double camDist(double d) { return V.on("camera") ? V.num("camera", "distance") : d; }
    /** a totem popped on us: our burst + sound; true = hide the vanilla totem animation */
    public static boolean totem() {
        if (!V.on("totem")) return false;
        Object self = game == null ? null : game.self();
        if (self != null && V.bool("totem", "particles")) { double[] p = game.pos(self, 1); long now = System.currentTimeMillis();
            for (int i = 0; i < 60; i++) { double a = Math.random() * Math.PI * 2, sp = 0.1 + Math.random() * 0.25;
                KPARTS.add(new double[]{p[0], p[1] + 0.2 + Math.random() * 1.6, p[2], Math.cos(a) * sp, 0.05 + Math.random() * 0.3, Math.sin(a) * sp, now, 1200 + Math.random() * 900, Math.random()}); }
            SONAR.add(new double[]{p[0], p[1], p[2], now}); }
        if (V.bool("totem", "sound")) sound("magic_pok", (float) V.num("sounds", "volume"));
        return V.mode("totem", "style") == 1;
    }
    /** the computer's clock as Minecraft time of day (6:00 = 0 ticks) */
    public static long realTime() { java.util.Calendar c = java.util.Calendar.getInstance(); return (((c.get(java.util.Calendar.HOUR_OF_DAY) - 6 + 24) % 24) * 1000L + c.get(java.util.Calendar.MINUTE) * 1000L / 60); }
    /** hand position and rotation for the main (0) or off hand (1): {x, y, z, rotX, rotY, rotZ, scale} */
    public static float[] hand(int which) {
        boolean split = which == 1 && V.bool("viewmodel", "split"); String p = split ? "l" : "";
        return new float[]{(float) V.num("viewmodel", p + "x"), (float) V.num("viewmodel", p + "y"), (float) V.num("viewmodel", p + "z"),
            (float) V.num("viewmodel", p + "rx"), (float) V.num("viewmodel", p + "ry"), (float) V.num("viewmodel", p + "rz"), (float) V.num("viewmodel", "scale")};
    }
    /** first person swing presets: {tx, ty, tz, rotX, rotY, rotZ} for swing progress 0..1 (main hand) */
    public static float[] swingPose(float swing) {
        if (!V.on("swing")) return null;
        int preset = V.mode("swing", "preset");
        float s = (float) Math.sin(Math.max(0, swing) * Math.PI), s2 = (float) Math.sin(Math.sqrt(Math.max(0, swing)) * Math.PI);
        if (preset == 5) return new float[]{0.02f, 0.06f - 0.05f * s, -0.05f, -70 + 45 * s2, 8, 0}; // flat: lies forward, chops down (also when idle)
        if (swing <= 0) return null;
        switch (preset) {
            case 1: return new float[]{0, -0.05f * s, 0, -18 * s2, 0, 0};                       // smooth: hand dips down
            case 2: return new float[]{-0.05f * s, 0.03f * s, 0, -12 * s, -35 * s2, 15 * s};     // "1.7 block": sword turns across the screen
            case 3: return new float[]{0, 0, -0.05f * s, 0, -360 * swing, 0};                    // spin
            case 4: return new float[]{0, 0.06f * s, 0.04f * s, 28 * s2, 0, 0};                  // chop: up and down
            default: return null;
        }
    }
    /** name protect: your nick in any text becomes the alias */
    public static String protect(String s) {
        if (s == null || !V.on("nameprotect") || V.user.isEmpty() || s.indexOf(V.user) < 0) return s;
        String[] al = {"Asteria", "Игрок", "★"}; return s.replace(V.user, al[Math.min(2, V.mode("nameprotect", "alias"))]);
    }

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
