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
        /** opaque textured quads with a texture id of the game (a player skin): 4 vt() per quad, then end() */
        default void beginSkin(Object texture) {}
        /** opaque coloured quads (writes depth): v() x4 per quad, then end() */
        default void beginSolid() { begin(QUADS, true); }
        /** a texture of our mod (textures/fx/<path>.png) as a texture id for beginSkin */
        default Object asset(String path) { return null; }
        /** a png from disk as a texture id (re-uploaded when the key changes; one texture per key prefix before ':') */
        default Object fileTex(String key, java.io.File png) { return null; }
        float camYaw();
        float camPitch();
        float partial();
    }

    // ------------------------------------------------------------------ markers: B puts a mark where you look (or removes it), Shift+B clears; per server
    /** a marker: position, name, colour (0 = theme), placed at */
    public static final class Mark { public double x, y, z; public String name = ""; public int color; public long at; }
    public static final List<Mark> MARKS = new ArrayList<>();
    private static String marksFor = null; private static long marksMtime = -2, marksCheck;
    private static java.io.File marksFile() { return new java.io.File(V.dir(), "asteria-marks.json"); }
    /** loads the marks of the current server (single player = "local") */
    public static void marksServer(Game g) {
        String srv = g.server(); if (srv == null) srv = "local"; long now = System.currentTimeMillis();
        if (srv.equals(marksFor) && now - marksCheck < 1500) return; marksCheck = now; long m = marksFile().exists() ? marksFile().lastModified() : -1;
        if (srv.equals(marksFor) && m == marksMtime) return; marksFor = srv; marksMtime = m; MARKS.clear(); // the launcher edits the same file
        try { com.google.gson.JsonObject o = new com.google.gson.JsonParser().parse(new String(java.nio.file.Files.readAllBytes(marksFile().toPath()), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if (o.has(srv)) for (com.google.gson.JsonElement e : o.getAsJsonArray(srv)) { Mark k = new Mark(); k.at = now - 5000;
                if (e.isJsonArray()) { com.google.gson.JsonArray a = e.getAsJsonArray(); k.x = a.get(0).getAsDouble(); k.y = a.get(1).getAsDouble(); k.z = a.get(2).getAsDouble(); }
                else { com.google.gson.JsonObject a = e.getAsJsonObject(); k.x = a.get("x").getAsDouble(); k.y = a.get("y").getAsDouble(); k.z = a.get("z").getAsDouble(); k.name = a.has("name") ? a.get("name").getAsString() : ""; k.color = a.has("color") ? V.parse(a.get("color").getAsString(), 0) : 0; }
                MARKS.add(k); } } catch (Exception ignored) {}
    }
    private static void saveMarks() {
        try { java.io.File f = marksFile(); com.google.gson.JsonObject o = f.exists() ? new com.google.gson.JsonParser().parse(new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject() : new com.google.gson.JsonObject();
            com.google.gson.JsonArray l = new com.google.gson.JsonArray(); for (Mark m : MARKS) { com.google.gson.JsonObject a = new com.google.gson.JsonObject(); a.addProperty("x", m.x); a.addProperty("y", m.y); a.addProperty("z", m.z); a.addProperty("name", m.name); if (m.color != 0) a.addProperty("color", V.hex(m.color)); l.add(a); }
            o.add(marksFor == null ? "local" : marksFor, l); java.nio.file.Files.write(f.toPath(), new com.google.gson.Gson().toJson(o).getBytes(java.nio.charset.StandardCharsets.UTF_8)); marksMtime = f.lastModified(); } catch (Exception ignored) {}
    }
    public static void markKey(Game g, boolean clear) {
        if (!V.on("markers")) return; marksServer(g);
        if (clear) { if (!MARKS.isEmpty()) { MARKS.clear(); saveMarks(); Hud.notify("Метки", "Все метки убраны", false); sound("pop", 0.5f); } return; }
        double[] at = g.lookAt(256); if (at == null) { Hud.notify("Метки", "Посмотрите на блок", false); return; }
        // the marker nearest to where you look (within 6 blocks of that spot) is removed
        Mark near = null; double best = 6; for (Mark m : MARKS) { double d = Math.abs(m.x - at[0]) + Math.abs(m.y - at[1]) + Math.abs(m.z - at[2]); if (d < best) { best = d; near = m; } }
        if (near != null) { MARKS.remove(near); saveMarks(); Hud.notify("Метки", "Метка «" + (near.name.isEmpty() ? "без названия" : near.name) + "» убрана", false); sound("pop", 0.5f); return; }
        addMark(at[0], at[1], at[2], "Метка " + (MARKS.size() + 1));
    }
    static void addMark(double x, double y, double z, String name) {
        Mark k = new Mark(); k.x = x; k.y = y; k.z = z; k.name = name; k.at = System.currentTimeMillis(); MARKS.add(k);
        while (MARKS.size() > (int) V.num("markers", "max")) MARKS.remove(0);
        saveMarks(); Hud.notify("Метки", "«" + name + "» поставлена", true); sound("blip", 0.5f);
    }
    /** chat: ".метка X Y Z [название]" / ".mark ..." adds a marker, ".метки очистить" removes all; true = the message is ours, don't send it */
    public static boolean chatCommand(Game g, String msg) {
        String s = msg.trim(), low = s.toLowerCase();
        if (!(low.startsWith(".метк") || low.startsWith(".mark"))) return false;
        marksServer(g);
        if (low.contains("очист") || low.contains("clear")) { MARKS.clear(); saveMarks(); Hud.notify("Метки", "Все метки убраны", false); return true; }
        String[] p = s.split("\\s+");
        try { double x = Double.parseDouble(p[1].replace(",", "")), y = Double.parseDouble(p[2].replace(",", "")), z = Double.parseDouble(p[3].replace(",", ""));
            StringBuilder n = new StringBuilder(); for (int i = 4; i < p.length; i++) n.append(i > 4 ? " " : "").append(p[i]);
            addMark(Math.floor(x) + 0.5, Math.floor(y), Math.floor(z) + 0.5, n.length() == 0 ? "Метка " + (MARKS.size() + 1) : n.toString());
        } catch (Exception e) { Hud.notify("Метки", "Пример: .метка 100 64 -200 База", false); }
        return true;
    }
    private static void markBeams(R3 r, Game g, long now) {
        if (!V.bool("markers", "beam")) return;
        for (int k = 0; k < MARKS.size(); k++) { Mark m = MARKS.get(k); float in = (float) Math.min(1, (now - m.at) / 500.0); int c = m.color != 0 ? m.color : V.grad(k * 0.17f);
            r.begin(R3.STRIP, true);
            for (int i = 0; i <= 16; i++) { double a = Math.PI * 2 * i / 16, rr = 0.16; r.v(m.x + Math.cos(a) * rr, m.y, m.z + Math.sin(a) * rr, D.a(c, 0.6f * in)); r.v(m.x + Math.cos(a) * rr, m.y + 90 * in, m.z + Math.sin(a) * rr, D.a(c, 0)); }
            r.end();
            r.begin(R3.STRIP, true); double pr = 0.5 + 0.12 * Math.sin(now / 300.0 + k); // a soft pulsing ring on the ground
            for (int i = 0; i <= 32; i++) { double a = Math.PI * 2 * i / 32; r.v(m.x + Math.cos(a) * pr, m.y + 0.02, m.z + Math.sin(a) * pr, D.a(c, 0.75f * in)); r.v(m.x + Math.cos(a) * (pr - 0.18), m.y + 0.02, m.z + Math.sin(a) * (pr - 0.18), D.a(c, 0)); }
            r.end(); }
    }

    // ------------------------------------------------------------------ target
    private static Object target; private static long targetAt, killMsgAt;
    // ------------------------------------------------------------------ alerts
    private static final java.util.Map<String, Long> ALERTED = new java.util.HashMap<>(); private static int lastTotems = -1; private static long alertAll;
    private static boolean once(String k, long now, long gap) { Long t = ALERTED.get(k); if (t != null && now - t < gap) return false; ALERTED.put(k, now); return true; }
    private static void alerts(Game g, long now) {
        if (!V.on("alerts") || now - alertAll < 300) return; alertAll = now; Object self = g.self(); if (self == null) return;
        if (V.bool("alerts", "armor")) for (Game.Item it : g.armor()) if (it.max > 0) { float left = 1f - it.damage / (float) it.max;
            if (left * 100 < V.num("alerts", "armorpct") && once("arm" + it.name, now, 60000)) Hud.alert("Броня ломается!", it.name + " — осталось " + Math.round(left * 100) + "%", 0xFFFFB547, "shield"); }
        if (V.bool("alerts", "hp")) { float hp = g.hp(self); if (hp > 0 && hp <= V.num("alerts", "hpval") * 2 && once("hp", now, 8000)) Hud.alert("Мало здоровья!", String.format(java.util.Locale.ROOT, "%.1f ❤", hp / 2).replace("❤", "сердца"), 0xFFFF4D6A, "heart"); }
        if (V.bool("alerts", "food")) { int f = g.food(); if (f >= 0 && f <= 6 && once("food", now, 30000)) Hud.alert("Голод", "Поешьте — бег скоро отключится", 0xFFF59E0B, "shop"); }
        if (V.bool("alerts", "totem")) { int tot = 0; for (Object st : g.inventory()) if (st != null && g.itemId(st).endsWith("totem_of_undying")) tot += g.count(st); Object off = g.offhand(); if (off != null && g.itemId(off).endsWith("totem_of_undying")) tot += g.count(off);
            if (lastTotems >= 0 && tot < lastTotems) Hud.alert("Тотем использован", tot == 0 ? "Тотемов больше нет!" : "Осталось: " + tot, tot == 0 ? 0xFFFF4D6A : 0xFFFACC15, "shield"); lastTotems = tot; }
        if (V.bool("alerts", "effect")) for (Game.Effect e : g.effects()) if (e.ticks > 0 && e.ticks < 100 && once("ef" + e.name, now, 20000)) Hud.alert("Эффект заканчивается", e.name + " — 5 сек", 0xFF60A5FA, "clock");
    }
    // ------------------------------------------------------------------ pets: following and looks
    static final class Follow { double[] pos, prev, owner, spot; double phase, speed, yaw, idleYaw; long wanderAt; }
    private static Follow selfF; private static final java.util.Map<String, Follow> OTHER_F = new java.util.HashMap<>();
    static boolean onGroundKind(int k) { return k == MINI || (k >= 10 && k != 14); } // 3D pets walk, the pictures and the bee float
    /** one tick of a pet near its owner: wanders around when the owner stands, follows behind (never in front) when the owner moves */
    static void follow(Follow f, double[] o, boolean ground, long seed) {
        double oy = o[1] + (ground ? 0 : 1.35);
        if (f.pos == null || Math.abs(f.pos[0] - o[0]) + Math.abs(f.pos[2] - o[2]) > 16) { f.pos = new double[]{o[0] - 1.5, oy, o[2] - 1.5}; f.prev = f.pos.clone(); f.owner = o.clone(); }
        f.prev = f.pos.clone();
        double vx = o[0] - f.owner[0], vz = o[2] - f.owner[2], v = Math.sqrt(vx * vx + vz * vz); f.owner = o.clone();
        long now = System.currentTimeMillis(); java.util.Random rn = new java.util.Random(seed / 3000);
        if (v > 0.06) { double nx = vx / v, nz = vz / v, side = (seed & 1) == 0 ? 1 : -1; // behind and a bit aside: out of the way in a fight
            f.spot = new double[]{o[0] - nx * 2.1 - nz * 0.8 * side, o[2] - nz * 2.1 + nx * 0.8 * side}; f.wanderAt = now + 1500; }
        else if (f.spot == null || now > f.wanderAt) { double a = rn.nextDouble() * Math.PI * 2, d = 1.3 + rn.nextDouble() * 1.1; f.spot = new double[]{o[0] + Math.cos(a) * d, o[2] + Math.sin(a) * d}; f.wanderAt = now + 2500 + rn.nextInt(3500); }
        double dx = f.spot[0] - f.pos[0], dz = f.spot[1] - f.pos[2], d = Math.sqrt(dx * dx + dz * dz), far = Math.sqrt(Math.pow(o[0] - f.pos[0], 2) + Math.pow(o[2] - f.pos[2], 2));
        double max = Math.max(0.12, Math.min(0.6, v * 1.15 + (far > 4 ? 0.2 : 0))), step = d < 0.12 ? 0 : Math.min(max, d * 0.16 + 0.02);
        if (step > 0) { f.pos[0] += dx / d * step; f.pos[2] += dz / d * step; double want = Math.toDegrees(Math.atan2(-dx, dz)), dd = ((want - f.yaw) % 360 + 540) % 360 - 180; f.yaw += dd * 0.3; }
        else { double want = Math.toDegrees(Math.atan2(-(o[0] - f.pos[0]), o[2] - f.pos[2])), dd = ((want - f.yaw) % 360 + 540) % 360 - 180; f.yaw += dd * 0.08; } // standing: looks at you
        f.pos[1] += (oy - f.pos[1]) * 0.25;
        f.speed = f.speed * 0.7 + step * 0.3; f.phase += f.speed * 5.5;
    }
    // ------------------------------------------------------------------ free look: the camera turns around the player, the player keeps looking ahead
    public static boolean freeLook; public static float flYaw, flPitch; private static int flPrev = -1;
    /** called every tick with the free-look key state */
    public static void freeLookKey(Game g, boolean held, float yaw, float pitch) {
        boolean on = held && V.on("freelook");
        if (on && !freeLook) { freeLook = true; flYaw = yaw; flPitch = pitch; flPrev = g.perspective(); if (flPrev == 0) g.setPerspective(1); }
        else if (!on && freeLook) { freeLook = false; if (flPrev == 0) g.setPerspective(0); flPrev = -1; }
    }
    /** mouse movement while free look is on (degrees, as Entity.changeLookDirection gets them * 0.15) */
    public static void freeLookTurn(double dx, double dy) { flYaw += (float) dx * 0.15f; flPitch = Math.max(-90, Math.min(90, flPitch + (float) dy * 0.15f * (V.bool("freelook", "invert") ? -1 : 1))); }
    public static void onAttack(Game g, Object e) {
        if (!g.living(e)) return;
        target = e; targetAt = System.currentTimeMillis(); game = g;
        if (V.on("hitparticles")) burst(g, e);
        if (V.on("sounds") && V.mode("sounds", "hit") > 0) sound(HIT_SOUNDS[V.mode("sounds", "hit") - 1], (float) V.num("sounds", "volume"));
    }
    /** what the target HUD / marker show: last attacked (3 s) or the living entity under the crosshair */
    public static Object target(Game g) {
        if (target != null && g.alive(target) && System.currentTimeMillis() - targetAt > 3500) target = null;
        if (target != null && g.alive(target)) return target;
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
    public static final List<double[]> DMG = new ArrayList<>();     // floating damage numbers: x,y,z,born,value,drift
    private static Object dmgEnt; private static float dmgHp = -1; private static long sonarAt;
    private static double[] lastStep;
    private static final List<double[]> DASH = new ArrayList<>();    // sprint-start streaks: x,y,z,dx,dz,start
    private static boolean wasSprint; public static long dashAt; private static double[] petPos, petPrev;
    /** pets of other launcher players: name -> {kind, size}; from <api>/cosmetics (the launcher sends each player's pet with its heartbeat) */
    private static volatile java.util.Map<String, double[]> PET_OF = new java.util.HashMap<>();
    /** halo / hat of other launcher players: name -> "halo,hat" */
    private static volatile java.util.Map<String, String> FX_OF = new java.util.HashMap<>();
    private static final java.util.Map<String, Object> OTHER_E = new java.util.HashMap<>(); // name -> player entity (for the mini-me skin)
    private static final java.util.Map<String, double[]> WALK = new java.util.HashMap<>();  // mini-me: name -> phase, last x, last z, yaw, speed
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
                java.util.Map<String, double[]> m = new java.util.HashMap<>(); java.util.Map<String, String> f = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> en : all.entrySet()) {
                    com.google.gson.JsonObject v = en.getValue().getAsJsonObject(); String p = v.has("pet") ? v.get("pet").getAsString() : "";
                    String fx = v.has("fx") ? v.get("fx").getAsString() : ""; if (!fx.isEmpty()) f.put(en.getKey().toLowerCase(), fx);
                    String[] q = p.split(":"); if (q.length < 2) continue;
                    try { m.put(en.getKey().toLowerCase(), new double[]{Integer.parseInt(q[0]), Double.parseDouble(q[1]), q.length > 2 && q[2].equals("m") ? 1 : 0}); } catch (Exception ignored) {}
                }
                PET_OF = m; FX_OF = f;
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
                if (V.on("killmsg") && now0 - targetAt < 5000 && now0 - killMsgAt > 3000 && g.isPlayer(target)) { killMsgAt = now0;
                    String txt = V.text("killmsg", "text").replace("{name}", g.playerName(target)).trim();
                    if (!txt.isEmpty()) { if (V.mode("killmsg", "chat") == 1 && !txt.startsWith("!")) txt = "!" + txt; final String m = txt; try { g.chat(m); } catch (Throwable ignored) {} } }
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
        if (!FX_OF.isEmpty()) { java.util.Set<String> seen = new java.util.HashSet<>();
            for (Object e : g.players()) { String n = g.playerName(e).toLowerCase(), fx = FX_OF.get(n); if (fx == null || !fx.contains("trail")) continue; seen.add(n);
                List<double[]> l = OTRAIL.computeIfAbsent(n, k -> new ArrayList<>()); double[] q = g.pos(e, 1);
                if (l.isEmpty() || dist(l.get(l.size() - 1), q) > 0.02) l.add(q); else l.remove(0); while (l.size() > 30) l.remove(0); }
            OTRAIL.keySet().retainAll(seen);
        } else OTRAIL.clear();
        boolean ground = g.onGround();
        if (V.on("jumpcircles") && ground != wasGround && (ground ? V.bool("jumpcircles", "land") : V.bool("jumpcircles", "jump"))) CIRCLES.add(new double[]{p[0], p[1], p[2], System.currentTimeMillis()});
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
        if (V.on("dash") && spr && !wasSprint) { dashAt = now0; double yr = Math.toRadians(g.yaw()); DASH.add(new double[]{p[0], p[1], p[2], -Math.sin(yr), Math.cos(yr), now0}); }
        wasSprint = spr; DASH.removeIf(q -> now0 - q[5] > 600);
        alerts(g, now0);
        // our pet
        if (V.on("pet")) { if (selfF == null) selfF = new Follow(); follow(selfF, p, onGroundKind(V.mode("pet", "kind")), now0); petPos = selfF.pos; petPrev = selfF.prev; } else { selfF = null; petPos = null; }
        // pets of other Asteria players (list refreshed every 10 s)
        if (now0 - petsAt > 10000) { petsAt = now0; fetchPets(); }
        if (V.bool("pet", "others") && !PET_OF.isEmpty()) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Object e : g.players()) { String n = g.playerName(e).toLowerCase(); double[] k = PET_OF.get(n); if (k == null) continue; seen.add(n); OTHER_E.put(n, e);
                Follow f = OTHER_F.computeIfAbsent(n, x -> new Follow()); follow(f, g.pos(e, 1), onGroundKind((int) k[0]), now0 + n.hashCode()); }
            OTHER_F.keySet().retainAll(seen); OTHER_E.keySet().retainAll(seen);
        } else OTHER_F.clear();
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
    /** true = don't play this Minecraft sound (our hit sounds replace the vanilla attack sounds) */
    public static boolean muteVanilla(String id) { return id != null && id.contains("entity.player.attack") && V.on("sounds") && V.bool("sounds", "mute") && V.mode("sounds", "hit") > 0; }
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
    /** camera of the last world frame (x, y, z, yaw, pitch, fov, partial): the HUD projects world points with it */
    private static volatile double[] CAM; private static double lastFov = 70;
    public static float partial() { double[] c = CAM; return c == null ? 1 : (float) c[6]; }
    /** world point -> GUI pixels {x, y, depth}; null when behind the camera */
    public static float[] project(double x, double y, double z, int W, int H) {
        double[] c = CAM; if (c == null) return null;
        double yaw = Math.toRadians(c[3]), pit = Math.toRadians(c[4]);
        double fx = -Math.sin(yaw) * Math.cos(pit), fy = -Math.sin(pit), fz = Math.cos(yaw) * Math.cos(pit), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double ux = -Math.sin(yaw) * Math.sin(pit), uy = Math.cos(pit), uz = Math.cos(yaw) * Math.sin(pit);
        double dx = x - c[0], dy = y - c[1], dz = z - c[2], d = dx * fx + dy * fy + dz * fz; if (d < 0.1) return null;
        double k = (H / 2.0) / Math.tan(Math.toRadians(c[5]) / 2);
        return new float[]{(float) (W / 2.0 + (dx * rx + dz * rz) / d * k), (float) (H / 2.0 - (dx * ux + dy * uy + dz * uz) / d * k), (float) d};
    }
    public static void render(R3 r, Game g) {
        long now = System.currentTimeMillis(); float pt = r.partial();
        { double[] cp = r.cam(); CAM = new double[]{cp[0], cp[1], cp[2], r.camYaw(), r.camPitch(), lastFov, pt}; }
        Object self = g.self(); if (self == null) return;
        if (V.on("trails") && TRAIL.size() > 1) trail(r, g, self, pt);
        for (List<double[]> l : OTRAIL.values()) if (l.size() > 1) trail(r, l);
        if (V.on("jumpcircles")) circles(r, now);
        Object t = target(g);
        if (V.on("targetesp") && t != null && t != self) targetEsp(r, g, t, pt, now);
        if (V.on("chinahat") && g.thirdPerson()) hat(r, g, self, pt, now, g.bodyYaw(pt));
        if (!FX_OF.isEmpty()) for (Object e : g.players()) { String fx = FX_OF.get(g.playerName(e).toLowerCase()); if (fx == null || !g.alive(e)) continue;
            if (fx.contains("halo")) halo(r, g, e, pt, now); if (fx.contains("hat,") || fx.endsWith("hat")) hat(r, g, e, pt, now, g.yawOf(e));
            if (V.bool("cosmetics", "others") && fx.contains("c:")) try { String[] c = fx.substring(fx.indexOf("c:") + 2).split("[,;]")[0].split("\\."); double[] q = g.pos(e, pt);
                Cosmetics.draw(r, q, g.yawOf(e), g.yawOf(e), g.height(e), Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), c.length > 3 ? (int) Long.parseLong(c[3], 16) : -1, false, Math.max(0.2f, g.light(q[0], q[1] + 1, q[2])), now); } catch (Throwable ignored) {} }
        if (V.on("cosmetics") && g.thirdPerson()) try { double[] q = g.pos(self, pt);
            Cosmetics.draw(r, q, g.bodyYaw(pt), g.yaw(), g.height(self), V.mode("cosmetics", "wings"), V.mode("cosmetics", "hat"), V.mode("cosmetics", "back"), V.bool("cosmetics", "tint") ? -1 : V.color("cosmetics", "color"), !g.onGround(), Math.max(0.2f, g.light(q[0], q[1] + 1, q[2])), now); } catch (Throwable ignored) {}
        if (V.on("markers") && !MARKS.isEmpty()) markBeams(r, g, now);
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

        if (!DASH.isEmpty()) dash(r, now);
        if (selfF != null && selfF.pos != null && (g.thirdPerson() || !V.bool("pet", "third"))) {
            Object skin = null; int kind = V.mode("pet", "kind");
            if (kind == MINI) { if (V.mode("pet", "miniskin") == 1) { java.io.File f = new java.io.File(System.getProperty("asteria.root", "."), "skins/mini.png"); if (f.exists()) skin = r.fileTex("mini:" + f.lastModified(), f); } if (skin == null) skin = g.skin(self); }
            drawAny(r, g, selfF, kind, V.num("pet", "size"), skin, "", now); }
        for (java.util.Map.Entry<String, Follow> en : OTHER_F.entrySet()) { double[] k = PET_OF.get(en.getKey()); if (k == null || en.getValue().pos == null) continue; Object skin = null;
            if ((int) k[0] == MINI) { Object e = OTHER_E.get(en.getKey()); skin = k[2] > 0 ? miniSkinOf(r, en.getKey()) : null; if (skin == null && e != null) skin = g.skin(e); }
            drawAny(r, g, en.getValue(), (int) k[0], k[1], skin, en.getKey(), now + en.getKey().hashCode()); }
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
    /** a pet of any kind at its interpolated place, lit like the world around it, with a soft shadow under it */
    static void drawAny(R3 r, Game g, Follow f, int kind, double size, Object skin, String who, long now) {
        float t = r.partial(); double[] pp = {f.prev[0] + (f.pos[0] - f.prev[0]) * t, f.prev[1] + (f.pos[1] - f.prev[1]) * t, f.prev[2] + (f.pos[2] - f.prev[2]) * t};
        float lt = Math.max(0.16f, g.light(pp[0], pp[1] + 0.4, pp[2]));
        shadow(r, pp[0], pp[1] - (onGroundKind(kind) ? 0 : 1.35) + 0.02, pp[2], 0.32 * size, lt);
        if (kind == MINI && skin != null) { drawMini(r, who, pp, skin, size, lt); return; }
        if (kind >= 10 && kind - 10 < VOX.length) { drawVox(r, VOX[kind - 10], pp, f, size, lt, now); return; }
        drawPet(r, pp, kind, size, now, f, lt);
    }
    private static void shadow(R3 r, double x, double y, double z, double rad, float lt) {
        r.begin(R3.TRIS, true); int n = 20;
        for (int i = 0; i < n; i++) { double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n; r.v(x, y, z, D.alpha(0, (int) (90 * lt))); r.v(x + Math.cos(a0) * rad, y, z + Math.sin(a0) * rad, 0); r.v(x + Math.cos(a1) * rad, y, z + Math.sin(a1) * rad, 0); }
        r.end();
    }
    static int shade(int argb, float f) { int a = argb >>> 24, rr = (int) (((argb >> 16) & 255) * f), gg = (int) (((argb >> 8) & 255) * f), bb = (int) ((argb & 255) * f); return (a << 24) | (Math.min(255, rr) << 16) | (Math.min(255, gg) << 8) | Math.min(255, bb); }
    // ------------------------------------------------------------------ 3D pets made of boxes (pixel units, feet at y = 0, looking at +z)
    // box: x0,y0,z0,x1,y1,z1, colour, part (0 body, 1 head, 2 tail, 3 ears, 4..7 legs FL FR BL BR, 8 wings, 9 eyes)
    // pivots: head x,y,z; tail x,y,z; leg top y; style (0 walk, 1 hop, 2 squash, 3 fly)
    static final class Vox { final float[][] b; final float[] pv; Vox(float[] pv, float[]... b) { this.pv = pv; this.b = b; } }
    private static float[] bx(double x0, double y0, double z0, double x1, double y1, double z1, int c, int part) { return new float[]{(float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, c >>> 16, part, c & 0xFFFF}; } // colour kept exactly as two 16-bit halves
    static final Vox[] VOX = {
        new Vox(new float[]{0, 6, 4.5f, 0, 6.8f, -4, 3, 0}, // kitten
            bx(-2.5, 3, -4, 2.5, 7.5, 4, 0xFFF0A050, 0), bx(-2.6, 7.4, -2.6, 2.6, 7.6, -1.8, 0xFFD27F2E, 0), bx(-2.6, 7.4, 0.2, 2.6, 7.6, 1, 0xFFD27F2E, 0), bx(-1.5, 3.4, 4, 1.5, 6.6, 4.3, 0xFFFFF4E6, 0),
            bx(-3, 5, 3, 3, 10.5, 8.5, 0xFFF4AE60, 1), bx(-1.6, 5.5, 8.5, 1.6, 7.4, 9.4, 0xFFFFF0DC, 1), bx(-0.5, 7.1, 9.4, 0.5, 7.7, 9.6, 0xFFFF8FA8, 1),
            bx(-2.3, 8, 8.5, -0.9, 9.4, 8.7, 0xFF20242E, 9), bx(0.9, 8, 8.5, 2.3, 9.4, 8.7, 0xFF20242E, 9), bx(-1.9, 8.9, 8.7, -1.5, 9.3, 8.8, 0xFFFFFFFF, 9), bx(1.3, 8.9, 8.7, 1.7, 9.3, 8.8, 0xFFFFFFFF, 9),
            bx(-3, 10.5, 4.5, -1.2, 12.5, 6.5, 0xFFE08840, 3), bx(1.2, 10.5, 4.5, 3, 12.5, 6.5, 0xFFE08840, 3), bx(-2.6, 10.5, 6.5, -1.6, 11.8, 6.7, 0xFFFFA6BF, 3), bx(1.6, 10.5, 6.5, 2.6, 11.8, 6.7, 0xFFFFA6BF, 3),
            bx(-2.3, 0, 2, -0.8, 3, 3.5, 0xFFE89A4C, 4), bx(0.8, 0, 2, 2.3, 3, 3.5, 0xFFE89A4C, 5), bx(-2.3, 0, -3.5, -0.8, 3, -2, 0xFFE89A4C, 6), bx(0.8, 0, -3.5, 2.3, 3, -2, 0xFFE89A4C, 7),
            bx(-0.7, 6, -9, 0.7, 7.4, -4, 0xFFE08840, 2), bx(-0.75, 6, -10, 0.75, 7.45, -9, 0xFFFFF4E6, 2)),
        new Vox(new float[]{0, 6.5f, 4.5f, 0, 6.5f, -4, 3, 0}, // fox
            bx(-2.4, 3, -4, 2.4, 7.4, 4, 0xFFE8742C, 0), bx(-1.6, 3.2, 3.9, 1.6, 6.2, 4.2, 0xFFFFF4EA, 0),
            bx(-3, 5, 3, 3, 10.3, 8, 0xFFEE8236, 1), bx(-1.8, 5, 8, 1.8, 7.3, 10.2, 0xFFFFF4EA, 1), bx(-0.6, 6.6, 10.2, 0.6, 7.4, 10.5, 0xFF231A18, 1),
            bx(-2.3, 8, 8, -1, 9.3, 8.2, 0xFF231A18, 9), bx(1, 8, 8, 2.3, 9.3, 8.2, 0xFF231A18, 9), bx(-1.9, 8.8, 8.2, -1.5, 9.2, 8.3, 0xFFFFFFFF, 9), bx(1.4, 8.8, 8.2, 1.8, 9.2, 8.3, 0xFFFFFFFF, 9),
            bx(-3, 10.3, 4.5, -1, 13.4, 6.3, 0xFFEE8236, 3), bx(1, 10.3, 4.5, 3, 13.4, 6.3, 0xFFEE8236, 3), bx(-3, 12.6, 4.5, -1, 13.4, 6.3, 0xFF231A18, 3), bx(1, 12.6, 4.5, 3, 13.4, 6.3, 0xFF231A18, 3),
            bx(-2.2, 0, 2, -0.8, 3, 3.4, 0xFF2A2020, 4), bx(0.8, 0, 2, 2.2, 3, 3.4, 0xFF2A2020, 5), bx(-2.2, 0, -3.4, -0.8, 3, -2, 0xFF2A2020, 6), bx(0.8, 0, -3.4, 2.2, 3, -2, 0xFF2A2020, 7),
            bx(-1.6, 4.6, -10, 1.6, 7.6, -4, 0xFFEE8236, 2), bx(-1.65, 4.6, -12, 1.65, 7.65, -10, 0xFFFFF4EA, 2)),
        new Vox(new float[]{0, 6, 3.5f, 0, 5, -3.5f, 2, 1}, // bunny
            bx(-2.6, 1.6, -3.6, 2.6, 6.6, 3, 0xFFF6F0E8, 0),
            bx(-2.4, 5, 2, 2.4, 9.6, 6.6, 0xFFFBF6F0, 1), bx(-0.5, 6.6, 6.6, 0.5, 7.3, 6.8, 0xFFFF8FA8, 1), bx(-1.2, 5.6, 6.6, 1.2, 6.6, 6.8, 0xFFFFFFFF, 1),
            bx(-2, 7.4, 6.6, -0.8, 8.7, 6.8, 0xFF2A2430, 9), bx(0.8, 7.4, 6.6, 2, 8.7, 6.8, 0xFF2A2430, 9), bx(-1.7, 8.2, 6.8, -1.3, 8.6, 6.9, 0xFFFFFFFF, 9), bx(1.1, 8.2, 6.8, 1.5, 8.6, 6.9, 0xFFFFFFFF, 9),
            bx(-1.9, 9.4, 3.6, -0.6, 15, 4.6, 0xFFF6F0E8, 3), bx(0.6, 9.4, 3.6, 1.9, 15, 4.6, 0xFFF6F0E8, 3), bx(-1.6, 9.8, 4.6, -0.9, 14.4, 4.7, 0xFFFFB3C6, 3), bx(0.9, 9.8, 4.6, 1.6, 14.4, 4.7, 0xFFFFB3C6, 3),
            bx(-2.2, 0, 1.4, -0.9, 2, 2.8, 0xFFEFE6DA, 4), bx(0.9, 0, 1.4, 2.2, 2, 2.8, 0xFFEFE6DA, 5), bx(-2.4, 0, -3.4, -0.8, 2, -1, 0xFFEFE6DA, 6), bx(0.8, 0, -3.4, 2.4, 2, -1, 0xFFEFE6DA, 7),
            bx(-1.2, 3.6, -4.8, 1.2, 6, -3.6, 0xFFFFFFFF, 2)),
        new Vox(new float[]{0, 4, 0, 0, 4, 0, 0, 2}, // slime
            bx(-2.2, 1.8, -2.2, 2.2, 6.2, 2.2, 0xFF3F8F33, 0), bx(-4, 0, -4, 4, 8, 4, 0x9C7CDB5E, 0),
            bx(-2.6, 4.4, 4.02, -0.8, 6.2, 4.12, 0xFF1E3A1A, 9), bx(0.8, 4.4, 4.02, 2.6, 6.2, 4.12, 0xFF1E3A1A, 9), bx(-0.8, 2.4, 4.02, 0.8, 3, 4.12, 0xFF1E3A1A, 0)),
        new Vox(new float[]{0, 10, 3, 0, 10, -3.5f, 8, 3}, // bee
            bx(-2.5, 8, -3.5, 2.5, 12, 3.5, 0xFFF7C948, 0), bx(-2.6, 7.9, -1.4, 2.6, 12.1, -0.4, 0xFF2B2118, 0), bx(-2.6, 7.9, 0.8, 2.6, 12.1, 1.8, 0xFF2B2118, 0), bx(-0.4, 9.6, -4.4, 0.4, 10.4, -3.5, 0xFF2B2118, 2),
            bx(-2, 10, 3.5, -0.8, 11.3, 3.7, 0xFF2B2118, 9), bx(0.8, 10, 3.5, 2, 11.3, 3.7, 0xFF2B2118, 9), bx(-1.6, 10.8, 3.7, -1.2, 11.2, 3.8, 0xFFFFFFFF, 9), bx(1.2, 10.8, 3.7, 1.6, 11.2, 3.8, 0xFFFFFFFF, 9),
            bx(-1.6, 12, 2.6, -1.2, 14, 3, 0xFF2B2118, 3), bx(1.2, 12, 2.6, 1.6, 14, 3, 0xFF2B2118, 3),
            bx(-6, 12, -1.6, -0.5, 12.2, 1.6, 0xB8E8F6FF, 8), bx(0.5, 12, -1.6, 6, 12.2, 1.6, 0xB8E8F6FF, 8)),
        new Vox(new float[]{0, 2.2f, 3, 0, 2.2f, -5, 1.2f, 0}, // axolotl
            bx(-2, 0.6, -5, 2, 3.6, 3, 0xFFF7A6C4, 0),
            bx(-3, 0.5, 3, 3, 4, 7, 0xFFF9B4CE, 1), bx(-1.6, 1.4, 7, 1.6, 1.8, 7.1, 0xFFC2507E, 1),
            bx(-2.4, 2.4, 7, -1.2, 3.4, 7.15, 0xFF2A1A24, 9), bx(1.2, 2.4, 7, 2.4, 3.4, 7.15, 0xFF2A1A24, 9),
            bx(-4.6, 2.4, 4, -3, 4.8, 5.4, 0xFFE5508A, 3), bx(3, 2.4, 4, 4.6, 4.8, 5.4, 0xFFE5508A, 3), bx(-1, 4, 4.6, 1, 5.4, 5.4, 0xFFE5508A, 3),
            bx(-3, 0, 1.4, -2, 1.2, 2.4, 0xFFF7A6C4, 4), bx(2, 0, 1.4, 3, 1.2, 2.4, 0xFFF7A6C4, 5), bx(-3, 0, -3.6, -2, 1.2, -2.6, 0xFFF7A6C4, 6), bx(2, 0, -3.6, 3, 1.2, -2.6, 0xFFF7A6C4, 7),
            bx(-0.4, 0.8, -11, 0.4, 4.4, -5, 0xFFF3C0D4, 2)),
    };
    static void drawVox(R3 r, Vox m, double[] pos, Follow f, double size, float lt, long now) {
        double t = now / 1000.0, mv = Math.min(1, f.speed * 9), ph = f.phase, style = m.pv[7], k = size * 0.04, yaw = Math.toRadians(f.yaw), cy = Math.cos(yaw), sy = Math.sin(yaw);
        double hop = style == 1 ? Math.abs(Math.sin(ph * 0.8)) * 3.2 * mv : style == 3 ? 1.2 * Math.sin(t * 2.2) : style == 2 ? Math.abs(Math.sin(ph * 0.7)) * 2.4 * mv : Math.abs(Math.sin(ph)) * 0.5 * mv;
        double squash = style == 2 ? 1 - 0.18 * Math.sin(ph * 1.4) * mv + 0.04 * Math.sin(t * 3) : 1 + 0.03 * Math.sin(t * 2.4);
        boolean blink = (now / 100) % 38 == 0;
        double headTilt = Math.sin(t * 1.3) * 0.12 * (1 - mv), tailWag = Math.sin(t * (mv > 0.2 ? 9 : 3.5)) * (style == 2 ? 0 : 0.55), earTw = ((now / 1700) % 5 == 0) ? Math.sin(t * 30) * 0.3 : 0, wing = Math.sin(t * 38) * 0.9;
        r.beginSolid();
        for (float[] b : m.b) { int part = (int) b[7], col = ((int) b[6] << 16) | (int) b[8];
            double[][] c = new double[8][];
            for (int n = 0; n < 8; n++) { double x = (n & 1) == 0 ? b[0] : b[3], y = (n & 2) == 0 ? b[1] : b[4], z = (n & 4) == 0 ? b[2] : b[5];
                if (part == 9 && blink) y = (b[1] + b[4]) / 2 + (y - (b[1] + b[4]) / 2) * 0.15;
                if (part == 1 || part == 3 || part == 9) { double[] q = rotX(y, z, m.pv[1], m.pv[2], headTilt); y = q[0]; z = q[1]; if (part == 3) { double[] e = rotX(y, z, m.pv[1] + 5, m.pv[2], earTw); y = e[0]; z = e[1]; } }
                else if (part == 2) { double[] q = rotY(x, z, m.pv[3], m.pv[5], tailWag); x = q[0]; z = q[1]; }
                else if (part >= 4 && part <= 7) { double sgn = (part == 4 || part == 7) ? 1 : -1; double[] q = rotX(y, z, m.pv[6], (b[2] + b[5]) / 2, Math.sin(ph) * 0.8 * mv * sgn); y = q[0]; z = q[1]; }
                else if (part == 8) { double side = b[0] < 0 ? -1 : 1; double ang = wing * side; double yy = m.pv[6] + 4 + (y - (m.pv[6] + 4)) * Math.cos(ang) + Math.abs(x) * Math.sin(ang * 0.6); y = yy; }
                y = y * squash; double hx = x * (style == 2 ? 2 - squash : 1);
                c[n] = new double[]{pos[0] + (hx * cy - z * sy) * k, pos[1] + (y + hop) * k, pos[2] + (hx * sy + z * cy) * k}; }
            quad(r, c[6], c[7], c[5], c[4], shade(col, 0.95f * lt)); quad(r, c[3], c[2], c[0], c[1], shade(col, 0.8f * lt));
            quad(r, c[2], c[6], c[4], c[0], shade(col, 0.85f * lt)); quad(r, c[7], c[3], c[1], c[5], shade(col, 0.85f * lt));
            quad(r, c[2], c[3], c[7], c[6], shade(col, 1.05f * lt)); quad(r, c[4], c[5], c[1], c[0], shade(col, 0.6f * lt)); }
        r.end();
    }
    private static double[] rotX(double y, double z, double py, double pz, double a) { double dy = y - py, dz = z - pz; return new double[]{py + dy * Math.cos(a) - dz * Math.sin(a), pz + dy * Math.sin(a) + dz * Math.cos(a)}; }
    private static double[] rotY(double x, double z, double px, double pz, double a) { double dx = x - px, dz = z - pz; return new double[]{px + dx * Math.cos(a) + dz * Math.sin(a), pz - dx * Math.sin(a) + dz * Math.cos(a)}; }
    private static void quad(R3 r, double[] a, double[] b, double[] c, double[] d, int col) { r.v(a[0], a[1], a[2], col); r.v(b[0], b[1], b[2], col); r.v(c[0], c[1], c[2], col); r.v(d[0], d[1], d[2], col); }
    static final int MINI = 9;
    private static long miniAt;
    /** the custom mini-me skin of another player (uploaded in their launcher), cached in config/asteria-minis */
    private static Object miniSkinOf(R3 r, String name) {
        java.io.File f = new java.io.File(new java.io.File(V.dir(), "asteria-minis"), name + ".png");
        if ((!f.exists() || System.currentTimeMillis() - f.lastModified() > 600000) && System.currentTimeMillis() - miniAt > 5000) { miniAt = System.currentTimeMillis();
            final String api = System.getProperty("asteria.api"); if (api != null && !api.isEmpty()) { Thread t = new Thread(() -> { try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(api + "/mini/" + name + ".png").openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(8000);
                if (c.getResponseCode() == 200) { f.getParentFile().mkdirs(); try (java.io.InputStream in = c.getInputStream()) { java.nio.file.Files.copy(in, f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } } } catch (Throwable ignored) {} }, "asteria-mini"); t.setDaemon(true); t.start(); } }
        return f.exists() ? r.fileTex("m_" + name + ":" + f.lastModified(), f) : null;
    }
    // ------------------------------------------------------------------ mini-me: a small player with a skin that runs after its owner
    /** skin boxes: texture offset u, v, size w, h, d (pixels of a 64x64 skin), position x0, y0, z0 (feet = 0, facing +z), pivot y, swing sign */
    private static final float[][] BODY = {
        {0, 0, 8, 8, 8, -4, 24, -4, 24, 0}, {16, 16, 8, 12, 4, -4, 12, -2, 12, 0},      // head, body
        {40, 16, 4, 12, 4, -8, 12, -2, 22, 1}, {32, 48, 4, 12, 4, 4, 12, -2, 22, -1},   // right arm, left arm
        {0, 16, 4, 12, 4, -4, 0, -2, 12, -1}, {16, 48, 4, 12, 4, 0, 0, -2, 12, 1}};     // right leg, left leg
    static void drawMini(R3 r, String who, double[] pos, Object skin, double size) { drawMini(r, who, pos, skin, size, 1); }
    static void drawMini(R3 r, String who, double[] pos, Object skin, double size, float lt) {
        double[] w = WALK.get(who); if (w == null) { w = new double[]{0, pos[0], pos[2], 0, 0}; WALK.put(who, w); }
        double dx = pos[0] - w[1], dz = pos[2] - w[2], sp = Math.sqrt(dx * dx + dz * dz); w[1] = pos[0]; w[2] = pos[2];
        w[4] = w[4] * 0.85 + Math.min(0.3, sp) * 0.15; w[0] += w[4] * 4.5;
        if (sp > 0.004) { double want = Math.toDegrees(Math.atan2(-dx, dz)), d = ((want - w[3]) % 360 + 540) % 360 - 180; w[3] += d * 0.25; }
        double swing = Math.sin(w[0]) * Math.min(1, w[4] * 25) * 0.9, k = 0.9 * size / 32, yaw = Math.toRadians(w[3]), cy = Math.cos(yaw), sy = Math.sin(yaw);
        double bob = Math.abs(Math.sin(w[0])) * Math.min(1, w[4] * 25) * 0.6;
        r.beginSkin(skin);
        for (int layer = 0; layer < 2; layer++) for (float[] pt : BODY) {
            if (layer == 1 && pt[1] != 0) continue; // second layer: only the hat
            double a = pt[9] * swing; float u = pt[0] + (layer == 1 ? 32 : 0), v = pt[1], bw = pt[2], bh = pt[3], bd = pt[4]; double inf = layer == 1 ? 0.5 : 0;
            double x0 = pt[5] - inf, y0 = pt[6] - inf, z0 = pt[7] - inf, x1 = pt[5] + bw + inf, y1 = pt[6] + bh + inf, z1 = pt[7] + bd + inf, py = pt[8];
            double[][] c = new double[8][]; // corners: bit0 x, bit1 y, bit2 z
            for (int n = 0; n < 8; n++) { double lx = (n & 1) == 0 ? x0 : x1, ly = (n & 2) == 0 ? y0 : y1, lz = (n & 4) == 0 ? z0 : z1;
                double ry = py + (ly - py) * Math.cos(a) - lz * Math.sin(a), rz = (ly - py) * Math.sin(a) + lz * Math.cos(a);
                c[n] = new double[]{pos[0] + (lx * cy - rz * sy) * k, pos[1] + (ry + bob) * k, pos[2] + (lx * sy + rz * cy) * k}; }
            // faces seen from outside: top-left, top-right, bottom-right, bottom-left + their skin rectangle
            face(r, c[6], c[7], c[5], c[4], u + bd, v + bd, bw, bh, shade(0xFFFFFFFF, lt));                 // front (+z)
            face(r, c[3], c[2], c[0], c[1], u + bd * 2 + bw, v + bd, bw, bh, shade(0xFFC8C8C8, lt));        // back
            face(r, c[2], c[6], c[4], c[0], u, v + bd, bd, bh, shade(0xFFDCDCDC, lt));                      // right side (-x)
            face(r, c[7], c[3], c[1], c[5], u + bd + bw, v + bd, bd, bh, shade(0xFFDCDCDC, lt));            // left side (+x)
            face(r, c[2], c[3], c[7], c[6], u + bd, v, bw, bd, shade(0xFFFFFFFF, lt));                      // top
            face(r, c[4], c[5], c[1], c[0], u + bd + bw, v, bw, bd, shade(0xFFA8A8A8, lt));                 // bottom
        }
        r.end();
    }
    private static void face(R3 r, double[] tl, double[] tr, double[] br, double[] bl, float u, float v, float w, float h, int c) {
        float u0 = u / 64f, v0 = v / 64f, u1 = (u + w) / 64f, v1 = (v + h) / 64f;
        r.vt(tl[0], tl[1], tl[2], u0, v0, c); r.vt(tr[0], tr[1], tr[2], u1, v0, c); r.vt(br[0], br[1], br[2], u1, v1, c); r.vt(bl[0], bl[1], bl[2], u0, v1, c);
    }
    static void drawPet(R3 r, double[] pp, int kind, double size, long now) { Follow f = new Follow(); drawPet(r, pp, kind, size, now, f, 1); }
    static void drawPet(R3 r, double[] pp, int kind, double size, long now, Follow f, float lt) {
        double mv = Math.min(1, f.speed * 9), bob = Math.sin(now / 420.0) * 0.06 + Math.abs(Math.sin(f.phase)) * 0.12 * mv, s = 0.22 * size; int c = V.grad(0.2f);
        if (lt > 0.5f) { r.beginTex("particles/bloom/bloom_soft", true); tq(r, pp[0], pp[1] + bob, pp[2], s * 2.2, 0, D.a(c, 0.18f * lt)); r.end(); } // a faint glow only in the light
        Object tex = r.asset(PETS[Math.max(0, Math.min(PETS.length - 1, kind))]);
        if (tex != null) { r.beginSkin(tex); tq(r, pp[0], pp[1] + bob, pp[2], s, Math.sin(now / 700.0) * 0.12 - mv * 0.12, shade(0xFFFFFFFF, lt)); r.end(); }
        else { r.beginTex(PETS[Math.max(0, Math.min(PETS.length - 1, kind))], true); tq(r, pp[0], pp[1] + bob, pp[2], s, Math.sin(now / 900.0) * 0.15, shade(0xFFFFFFFF, lt)); r.end(); }
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

    private static void trail(R3 r, Game g, Object self, float pt) { List<double[]> pts = new ArrayList<>(TRAIL); pts.add(g.pos(self, pt)); trail(r, pts); }
    private static final java.util.Map<String, List<double[]>> OTRAIL = new java.util.HashMap<>();
    private static void trail(R3 r, List<double[]> pts) {
        int n = pts.size(); boolean ribbon = V.mode("trails", "mode") == 0; double h = V.num("trails", "height");
        if (V.mode("trails", "mode") == 2) { // spiral: two twisting ribbons around the path
            double t0 = System.currentTimeMillis() / 420.0;
            double[] dh = new double[n]; for (int i = n - 2; i >= 0; i--) dh[i] = dh[i + 1] + dist(pts.get(i), pts.get(i + 1));
            for (int k = 0; k < 2; k++) { r.begin(R3.STRIP, true);
                for (int i = 0; i < n; i++) { double[] p = pts.get(i); float f = i / (float) (n - 1); int c = V.grad(f * 0.6f + k * 0.5f); double a = t0 - dh[i] * 2.4 + k * Math.PI, ox = Math.cos(a) * 0.35, oz = Math.sin(a) * 0.35, yy = p[1] + h * 0.5 + Math.sin(a) * 0.15;
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
            int n = 72; double spin = now / 1000.0 * sp * 1.6; // continuous ribbons (no separate dots that seem to jump)
            for (int k = 0; k < 2; k++) for (int pass = 0; pass < 2; pass++) { double half = pass == 0 ? 0.09 : 0.03;
                r.begin(R3.STRIP, true);
                for (int j = 0; j <= n; j++) { double f = j / (double) n, a = spin + f * Math.PI * 3 + k * Math.PI, yy = p[1] + h * f, x = p[0] + Math.cos(a) * rad, z = p[2] + Math.sin(a) * rad;
                    float fade = (float) Math.sin(f * Math.PI); int c = pass == 0 ? D.a(V.grad((float) f * 0.6f + k * 0.5f), 0.22f * fade) : D.a(D.mix(V.grad((float) f * 0.6f + k * 0.5f), 0xFFFFFFFF, 0.25f), 0.95f * fade);
                    r.v(x, yy - half, z, c); r.v(x, yy + half, z, c); }
                r.end(); }
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
    private static void hat(R3 r, Game g, Object self, float pt, long now, float bodyYaw) {
        double[] p = g.pos(self, pt); double top = p[1] + g.height(self) + 0.02, rad = V.num("chinahat", "size"); float a = (float) V.num("chinahat", "alpha");
        int seg = 40; double yaw = Math.toRadians(-bodyYaw);
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
    public static double camDist(double d) { return freeLook ? V.num("freelook", "distance") : V.on("camera") ? V.num("camera", "distance") : d; }
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
    private static float ease(float x) { x = Math.max(0, Math.min(1, x)); return x < 0.5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2; }
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
            case 6: { float e = ease(swing); return new float[]{0.04f * s, 0.02f * s, 0, -25 * s2, -40 + 80 * e, -20 * s}; } // wide sweep from right to left
            case 7: return new float[]{0, -0.02f * s, -0.18f * s, -10 * s, 0, 0};                // stab: straight forward
            case 8: return new float[]{-0.04f, 0.02f, 0, -20 - 25 * s2, -30, 20 * s};            // soft block: sword held across, a gentle tap
            default: return null;
        }
    }
    /** name protect: your nick in any text becomes the alias */
    public static String protect(String s) {
        if (s == null || !V.on("nameprotect") || V.user.isEmpty() || s.indexOf(V.user) < 0) return s;
        String[] al = {"Asteria", "Игрок", "AL"}; return s.replace(V.user, al[Math.min(2, V.mode("nameprotect", "alias"))]);
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
        if (zoomT < 0.002f) { zoomT = 0; lastFov = base; return base; }
        double z = zoomLevel > 0 ? zoomLevel : V.num("zoom", "fov");
        return lastFov = base + (z - base) * zoomT;
    }
    /** mouse sensitivity multiplier while zoomed (finer aim) */
    public static double sensitivity() { return zoomT > 0 ? 1 - zoomT * 0.55 : 1; }
}
