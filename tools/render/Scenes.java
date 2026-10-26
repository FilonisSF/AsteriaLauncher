import static java.lang.Math.*;

/** The worlds behind every launcher picture. Each scene builds terrain, structures, lighting and the camera. */
final class Scenes {
    private Scenes() {}

    static void build(String s) {
        switch (s) {
            case "1.8.9": classic(); break;
            case "1.12.2": badlands(); break;
            case "1.16.5": crimson(); break;
            case "1.20.1": cherryGrove(); break;
            case "1.21.4": paleGarden(); break;
            case "1.21.11": coast(); break;
            case "26.1": aurora(); break;
            case "1.17.1": lushCave(); break;
            case "1.18.2": peaks(); break;
            case "1.19.4": deepDark(); break;
            case "wipe": wipe(); break;
            case "news": town(); break;
            case "ender": end(); break;
            case "spawners": dungeon(); break;
            case "apiary": apiary(); break;
            case "about": floatingIsland(); break;
            default: throw new IllegalArgumentException(s);
        }
    }
    static float[] v(float a, float b, float c) { return new float[]{a, b, c}; }
    static void glow(float x, float y, float z, float r, float g, float b, float size) { Voxel.GLOWS.add(new float[]{x, y, z, r, g, b, size}); }
    static void particles(int n, float x0, float x1, float y0, float y1, float z0, float z1, float[] col, float size, int seed) {
        for (int i = 0; i < n; i++) Voxel.PARTICLES.add(new float[]{x0 + Voxel.rnd(i, seed, 1) * (x1 - x0), y0 + Voxel.rnd(i, seed, 2) * (y1 - y0), z0 + Voxel.rnd(i, seed, 3) * (z1 - z0),
            col[0] * (0.8f + Voxel.rnd(i, seed, 4) * 0.4f), col[1] * (0.8f + Voxel.rnd(i, seed, 5) * 0.4f), col[2] * (0.8f + Voxel.rnd(i, seed, 6) * 0.4f), size * (0.6f + Voxel.rnd(i, seed, 7) * 0.8f), 0});
    }
    static int H(int x, int z) { return Voxel.ground(x, z); }

    // ------------------------------------------------------------------ 1.8.9 · classic hills, a lake and a watchtower at golden hour
    static void classic() {
        Voxel.world(200, 96, 230);
        for (int x = 0; x < 200; x++) for (int z = 0; z < 230; z++) {
            int h = (int) (28 + Voxel.fbm(x * 0.014f, z * 0.014f, 3, 4) * 22 + max(0, z - 150) * 0.14f * Voxel.fbm(x * 0.03f, z * 0.03f, 9, 3));
            Voxel.column(x, z, h, h <= 32 ? Voxel.SAND : (Voxel.rnd(x, 1, z) > 0.55f ? Voxel.FLOWER_GRASS : Voxel.GRASS), h <= 32 ? Voxel.SAND : Voxel.DIRT, Voxel.STONE);
            for (int y = h + 1; y <= 32; y++) Voxel.set(x, y, z, Voxel.WATER);
        }
        int tx = 112, tz = 120, tg = H(tx, tz); // watchtower
        for (int y = tg - 3; y < tg + 17; y++) for (int a = 0; a < 7; a++) for (int b = 0; b < 7; b++) {
            boolean wall = a == 0 || b == 0 || a == 6 || b == 6; if (!wall && y > tg) continue;
            if (y >= tg + 16 && (a + b) % 2 == 1) continue;
            Voxel.set(tx + a, y, tz + b, y > tg + 13 ? Voxel.STONE_BRICK : Voxel.rnd(a, y, b) > 0.75f ? Voxel.MOSSY : Voxel.COBBLE);
        }
        for (int y = tg + 7; y < tg + 13; y++) { Voxel.set(tx + 2, y, tz - 1, Voxel.WOOL_RED); Voxel.set(tx + 3, y, tz - 1, Voxel.WOOL_RED); Voxel.set(tx + 4, y, tz - 1, Voxel.WOOL_RED); }
        for (int y = tg + 2; y < tg + 5; y++) Voxel.set(tx + 3, y, tz, Voxel.AIR); // door
        glow(tx + 0.5f, tg + 16.5f, tz - 0.3f, 2.2f, 1.2f, 0.4f, 0.5f); glow(tx + 6.5f, tg + 16.5f, tz - 0.3f, 2.2f, 1.2f, 0.4f, 0.5f);
        for (int i = 0; i < 260; i++) { int x = (int) (Voxel.rnd(i, 5, 1) * 196) + 2, z = (int) (Voxel.rnd(i, 5, 2) * 226) + 2, g = H(x, z);
            if (g > 33 && Voxel.get(x, g, z) != Voxel.SAND && abs(x - tx - 3) + abs(z - tz - 3) > 12 && !(abs(x - 100) < 14 && z < 70)) Voxel.oak(x, g + 1, z, 4 + (int) (Voxel.rnd(i, 6, 3) * 3)); }
        Voxel.sun = Voxel.n3(-0.8f, 0.32f, 0.35f); Voxel.sunCol = v(2.2f, 1.5f, 0.95f);
        Voxel.skyTop = v(0.1f, 0.26f, 0.8f); Voxel.skyHor = v(0.95f, 0.72f, 0.55f); Voxel.fogCol = v(0.82f, 0.72f, 0.64f); Voxel.ambient = v(0.36f, 0.4f, 0.55f);
        Voxel.fogDensity = 0.0035f; Voxel.cloudY = 92; Voxel.dof = 2.5f;
        Voxel.look(104, 47, 66, 115, 43, 122, 54);
    }

    // ------------------------------------------------------------------ 1.12.2 · World of Color: badlands terraces and a concrete rainbow
    static void badlands() {
        Voxel.world(210, 110, 230);
        int[] bands = {0, 1, 1, 2, 7, 4, 0, 8, 3, 1, 9, 2, 5, 1, 4, 0};
        for (int x = 0; x < 210; x++) for (int z = 0; z < 230; z++) {
            float m = Voxel.fbm(x * 0.028f, z * 0.028f, 21, 5), base = 30 + Voxel.fbm(x * 0.05f, z * 0.05f, 4, 3) * 5;
            int h = (int) base;
            if (m > 0.5f && (abs(x - 104) > 36 || z > 140 || z < 40)) { float up = (m - 0.5f) * 170 * (0.6f + z / 400f); h = (int) (base + floor(up / 5) * 5 + (up % 5 > 4 ? up % 5 - 4 : 0)); }
            h = min(h, 98);
            for (int y = 0; y <= h; y++) Voxel.set(x, y, z, y < 26 ? Voxel.STONE : y == h && h < 36 ? Voxel.RED_SAND : Voxel.TERRACOTTA + bands[(y + (int) (Voxel.fbm(x * 0.02f, z * 0.02f, 8, 2) * 3)) % bands.length]);
        }
        int ax = 104, az = 112, ag = H(104, 112); // rainbow arch of concrete
        int[] rainbow = {14, 1, 4, 5, 3, 11, 10};
        for (int x = ax - 24; x <= ax + 24; x++) for (int y = ag; y <= ag + 26; y++) { float d = (float) hypot(x - ax, y - ag); int ring = (int) (24 - d);
            if (d <= 24 && d > 24 - 7) for (int z = az; z < az + 3; z++) Voxel.set(x, y, z, Voxel.CONCRETE + rainbow[max(0, min(6, ring))]); }
                Voxel.sun = Voxel.n3(0.55f, 0.62f, -0.45f); Voxel.sunCol = v(2.0f, 1.75f, 1.45f);
        Voxel.skyTop = v(0.12f, 0.32f, 0.85f); Voxel.skyHor = v(0.85f, 0.82f, 0.8f); Voxel.fogCol = v(0.92f, 0.78f, 0.66f); Voxel.ambient = v(0.42f, 0.44f, 0.55f);
        Voxel.fogDensity = 0.0045f; Voxel.cloudY = 115; Voxel.saturation = 1.12f; Voxel.dof = 2;
        Voxel.look(104, 50, 40, 104, 43, 116, 60);
    }

    // ------------------------------------------------------------------ 1.16.5 · Nether Update: crimson forest over a lava sea
    static void crimson() {
        Voxel.world(180, 84, 210);
        for (int x = 0; x < 180; x++) for (int z = 0; z < 210; z++) {
            int h = (int) (14 + Voxel.fbm(x * 0.03f, z * 0.03f, 31, 5) * 22 + max(0, abs(x - 90) - 20) * 0.08f);
            for (int y = 0; y <= h; y++) Voxel.set(x, y, z, y == h && h > 17 ? Voxel.NYLIUM : Voxel.NETHERRACK);
            for (int y = h + 1; y <= 17; y++) Voxel.set(x, y, z, Voxel.LAVA);
            int c = (int) (78 - Voxel.fbm(x * 0.04f, z * 0.04f, 33, 4) * 20); for (int y = c; y < 84; y++) Voxel.set(x, y, z, Voxel.NETHERRACK);
            if (Voxel.rnd(x, 7, z) > 0.992f) for (int y = c - 1; y > c - 1 - (int) (Voxel.rnd(x, 8, z) * 9); y--) Voxel.set(x, y, z, Voxel.WART);
        }
        for (int i = 0; i < 120; i++) { int x = (int) (Voxel.rnd(i, 4, 1) * 172) + 4, z = (int) (Voxel.rnd(i, 4, 2) * 200) + 6, g = H(x, z);
            g = Voxel.floorY(x, z); if (Voxel.get(x, g, z) == Voxel.NYLIUM && !(abs(x - 90) < 5 && z < 24)) Voxel.fungus(x, g + 1, z, 7 + (int) (Voxel.rnd(i, 4, 3) * 12)); }
        for (int x = 70; x < 112; x++) for (int z = 52; z < 70; z++) for (int y = 17; y <= 18; y++) if (Voxel.get(x, y, z) == Voxel.AIR && Voxel.vnoise(x * 0.2f, z * 0.2f, 5) > 0.3f) Voxel.set(x, y, z, Voxel.LAVA);
        Voxel.nether = true; Voxel.shadows = false; Voxel.ambient = v(0.6f, 0.26f, 0.24f); Voxel.blockCol = v(1.9f, 0.95f, 0.45f);
        Voxel.fogCol = v(0.2f, 0.035f, 0.04f); Voxel.skyTop = Voxel.fogCol; Voxel.skyHor = Voxel.fogCol; Voxel.fogDensity = 0.014f; Voxel.fogStart = 10; Voxel.exposure = 1.25f;
        Voxel.saturation = 0.95f; Voxel.contrast = 1.12f; Voxel.bloom = 0.5f; Voxel.dof = 2.5f; Voxel.vignette = 0.55f;
        particles(220, 70, 112, 22, 50, 8, 70, v(1.6f, 0.45f, 0.3f), 0.05f, 11);
        Voxel.look(90, 40, 4, 92, 31, 86, 62);
    }

    // ------------------------------------------------------------------ 1.20.1 · Trails & Tales: cherry grove valley with snowy peaks
    static void cherryGrove() {
        Voxel.world(230, 120, 250);
        for (int x = 0; x < 230; x++) for (int z = 0; z < 250; z++) {
            float hill = Voxel.fbm(x * 0.018f, z * 0.018f, 41, 5), mount = max(0, z - 150) / 100f;
            int h = (int) (32 + hill * 22 + mount * mount * 70 * Voxel.fbm(x * 0.03f, z * 0.03f, 43, 4) * 1.6f);
            h = min(h, 116);
            int top = h > 82 ? Voxel.SNOW : h > 70 ? Voxel.STONE : z < 150 && Voxel.vnoise(x * 0.05f, z * 0.05f, 2) > 0.35f ? Voxel.PETAL_GRASS : Voxel.GRASS;
            Voxel.column(x, z, h, top, h > 70 ? Voxel.STONE : Voxel.DIRT, Voxel.STONE);
            for (int y = h + 1; y <= 34; y++) Voxel.set(x, y, z, Voxel.WATER);
        }
        for (int i = 0; i < 230; i++) { int x = (int) (Voxel.rnd(i, 2, 1) * 220) + 5, z = (int) (Voxel.rnd(i, 2, 2) * 170) + 22, g = H(x, z);
            if (g > 35 && g < 70 && Voxel.get(x, g, z) != Voxel.WATER) Voxel.cherry(x, g + 1, z, 5 + (int) (Voxel.rnd(i, 2, 3) * 3), i); }
        Voxel.sun = Voxel.n3(-0.62f, 0.42f, 0.55f); Voxel.sunCol = v(1.9f, 1.62f, 1.45f);
        Voxel.skyTop = v(0.3f, 0.52f, 0.95f); Voxel.skyHor = v(1.0f, 0.86f, 0.9f); Voxel.fogCol = v(0.98f, 0.86f, 0.92f); Voxel.ambient = v(0.46f, 0.46f, 0.6f);
        Voxel.fogDensity = 0.0028f; Voxel.fogStart = 40; Voxel.cloudY = 125; Voxel.dof = 3; Voxel.saturation = 1.12f;
        particles(160, 95, 130, 50, 76, 14, 60, v(1.0f, 0.62f, 0.78f), 0.05f, 13);
        Voxel.look(112, 72, 10, 112, 56, 128, 58);
    }

    // ------------------------------------------------------------------ 1.21.4 · The Garden Awakens: pale garden in fog, a creaking heart glowing
    static void paleGarden() {
        Voxel.world(180, 90, 210);
        for (int x = 0; x < 180; x++) for (int z = 0; z < 210; z++) {
            int h = (int) (32 + Voxel.fbm(x * 0.03f, z * 0.03f, 51, 4) * 7);
            Voxel.column(x, z, h, Voxel.rnd(x, 3, z) > 0.04f ? Voxel.PALE_MOSS : Voxel.GRASS, Voxel.DIRT, Voxel.STONE);
        }
        int n = 0;
        for (int gx = 6; gx < 176; gx += 10) for (int gz = 18; gz < 206; gz += 10) {
            int x = gx + (int) (Voxel.rnd(gx, 1, gz) * 5) - 2, z = gz + (int) (Voxel.rnd(gx, 2, gz) * 5) - 2;
            if (hypot(x - 90, z - 8) < 22 || (abs(x - 90) < 5 && z < 46)) continue;
            boolean heart = abs(x - 95) < 6 && abs(z - 62) < 6;
            Voxel.paleOak(x, H(x, z) + 1, z, 7 + (int) (Voxel.rnd(gx, 3, gz) * 4), heart); if (heart) glow(x + 0.5f, H(x, z) + 1 + (7 + (int) (Voxel.rnd(gx, 3, gz) * 4)) / 2 + 0.5f, z + 0.5f, 2.6f, 1.0f, 0.3f, 0.6f); n++;
        }
        Voxel.sun = Voxel.n3(0.3f, 0.75f, 0.4f); Voxel.sunCol = v(0.55f, 0.56f, 0.6f); Voxel.ambient = v(0.62f, 0.65f, 0.7f);
        Voxel.skyTop = v(0.6f, 0.63f, 0.66f); Voxel.skyHor = v(0.72f, 0.74f, 0.76f); Voxel.fogCol = v(0.66f, 0.68f, 0.7f); Voxel.fogDensity = 0.011f; Voxel.fogStart = 8;
        Voxel.saturation = 0.75f; Voxel.contrast = 1.12f; Voxel.exposure = 0.95f; Voxel.tint = v(0.97f, 1f, 1.03f); Voxel.bloom = 0.5f; Voxel.vignette = 0.5f;
        int ey = H(90, 8) + 5; Voxel.look(90, ey, 6, 95, ey - 1, 70, 64);
    }

    // ------------------------------------------------------------------ 1.21.11 · coast at sunset, coral reef under clear water
    static void coast() {
        Voxel.world(230, 90, 250);
        for (int x = 0; x < 230; x++) for (int z = 0; z < 250; z++) {
            float shore = (70 - x) * 0.45f + Voxel.fbm(x * 0.03f, z * 0.03f, 61, 4) * 18;
            int h = (int) (20 + Voxel.fbm(x * 0.04f, z * 0.04f, 62, 4) * 9 + max(0, shore));
            h = min(h, 70);
            Voxel.column(x, z, h, h > 36 ? Voxel.GRASS : Voxel.SAND, h > 36 ? Voxel.DIRT : Voxel.SAND, Voxel.STONE);
            for (int y = h + 1; y <= 34; y++) Voxel.set(x, y, z, Voxel.WATER);
            if (h < 31 && h > 21 && Voxel.vnoise(x * 0.12f, z * 0.12f, 7) > 0.55f) { int len = 1 + (int) (Voxel.rnd(x, 4, z) * 3); int c = Voxel.CORAL + (int) (Voxel.rnd(x / 3, 5, z / 3) * 5); for (int y = 1; y <= len; y++) Voxel.set(x, h + y, z, c); }
        }
        for (int i = 0; i < 70; i++) { int x = (int) (Voxel.rnd(i, 8, 1) * 60) + 2, z = (int) (Voxel.rnd(i, 8, 2) * 240) + 5, g = H(x, z); if (g > 37 && Voxel.get(x, g, z) == Voxel.GRASS) Voxel.oak(x, g + 1, z, 5); }
        Voxel.sun = Voxel.n3(0.25f, 0.12f, 1f); Voxel.sunCol = v(2.0f, 1.15f, 0.7f); Voxel.sunSize = 0.9993f;
        Voxel.skyTop = v(0.12f, 0.12f, 0.42f); Voxel.skyHor = v(1.2f, 0.55f, 0.35f); Voxel.fogCol = v(0.95f, 0.62f, 0.52f); Voxel.ambient = v(0.36f, 0.34f, 0.5f);
        Voxel.saturation = 1.18f; Voxel.contrast = 1.1f; Voxel.seaLevel = 34; Voxel.fogDensity = 0.004f; Voxel.waterAbs = 0.09f; Voxel.waterCol = v(0.03f, 0.28f, 0.36f); Voxel.cloudY = 110; Voxel.bloom = 0.5f;
        Voxel.look(140, 50, 10, 118, 30, 130, 60);
    }

    // ------------------------------------------------------------------ 26.1 · snowy taiga under the northern lights
    static void aurora() {
        Voxel.world(230, 100, 250);
        for (int x = 0; x < 230; x++) for (int z = 0; z < 250; z++) {
            int h = (int) (34 + Voxel.fbm(x * 0.02f, z * 0.02f, 71, 5) * 24 + max(0, z - 170) * 0.25f);
            Voxel.column(x, z, h, Voxel.SNOW, Voxel.DIRT, Voxel.STONE);
            if (h < 39) { for (int y = h; y < 39; y++) Voxel.set(x, y, z, Voxel.ICE); }
        }
        for (int i = 0; i < 380; i++) { int x = (int) (Voxel.rnd(i, 7, 1) * 222) + 4, z = (int) (Voxel.rnd(i, 7, 2) * 240) + 6, g = H(x, z);
            if (Voxel.get(x, g, z) == Voxel.SNOW && !(abs(x - 112) < 10 && z < 50)) Voxel.spruce(x, g + 1, z, 7 + (int) (Voxel.rnd(i, 7, 3) * 6), true); }
        int cx = 96, cz = 74, cg = H(cx, cz); // cabin with a lit window
        for (int a = 0; a < 7; a++) for (int b = 0; b < 6; b++) for (int y = cg; y < cg + 5; y++) if (a == 0 || b == 0 || a == 6 || b == 5 || y == cg) Voxel.set(cx + a, y, cz + b, Voxel.SPRUCE_LOG);
        for (int k = 0; k < 4; k++) for (int a = -1 + k; a <= 7 - k; a++) for (int b = -1; b <= 6; b++) Voxel.set(cx + a, cg + 5 + k, cz + b, Voxel.SNOW);
        Voxel.set(cx + 3, cg + 2, cz, Voxel.LANTERN_GLASS); Voxel.set(cx + 2, cg + 2, cz, Voxel.LANTERN_GLASS);
        glow(cx + 3f, cg + 2.5f, cz - 0.2f, 2.4f, 1.4f, 0.6f, 0.6f);
        Voxel.sun = Voxel.n3(-0.4f, 0.45f, 0.7f); Voxel.sunCol = v(0.32f, 0.42f, 0.62f); Voxel.sunSize = 2;
        Voxel.skyTop = v(0.008f, 0.015f, 0.05f); Voxel.skyHor = v(0.04f, 0.08f, 0.14f); Voxel.fogCol = v(0.05f, 0.09f, 0.15f); Voxel.ambient = v(0.12f, 0.17f, 0.28f);
        Voxel.stars = 1; Voxel.aurora = 1.2f; Voxel.fogDensity = 0.004f; Voxel.exposure = 1.5f; Voxel.bloom = 0.6f; Voxel.blockCol = v(1.9f, 1.2f, 0.6f);
        Voxel.look(112, 50, 8, 108, 62, 120, 62);
    }

    // ------------------------------------------------------------------ 1.17 · Caves & Cliffs I: a lush cave with a pool, glow berries and moss
    static void lushCave() {
        Voxel.world(150, 80, 170);
        for (int x = 0; x < 150; x++) for (int z = 0; z < 170; z++) {
            float c = Voxel.fbm(x * 0.035f, z * 0.035f, 81, 4);
            int floor = (int) (14 + c * 8), ceil = (int) (52 - Voxel.fbm(x * 0.05f, z * 0.05f, 82, 4) * 14 + abs(x - 75) * 0.22f);
            for (int y = 0; y < 80; y++) if (y <= floor || y >= ceil) Voxel.set(x, y, z, y == floor ? (Voxel.rnd(x, 9, z) > 0.25f ? Voxel.MOSSY : Voxel.GRASS) : y == ceil ? Voxel.MOSSY : Voxel.STONE);
            if (floor < 17) for (int y = floor + 1; y <= 17; y++) Voxel.set(x, y, z, Voxel.WATER);
            if (Voxel.rnd(x, 11, z) > 0.965f && ceil < 70) { int len = 2 + (int) (Voxel.rnd(x, 12, z) * 9); for (int y = 1; y <= len && ceil - y > floor + 2; y++) Voxel.set(x, ceil - y, z, Voxel.HANGING_MOSS);
                if (Voxel.rnd(x, 13, z) > 0.45f) { Voxel.set(x, ceil - len - 1, z, Voxel.SHROOMLIGHT); glow(x + 0.5f, ceil - len - 0.5f, z + 0.5f, 2.2f, 1.3f, 0.45f, 0.35f); } }
            if (Voxel.rnd(x, 14, z) > 0.985f && floor >= 17) { Voxel.set(x, floor + 1, z, Voxel.FLOWER_GRASS); }
        }
        particles(120, 40, 110, 20, 45, 20, 120, v(0.75f, 1.0f, 0.55f), 0.04f, 17);
        Voxel.sun = Voxel.n3(0.2f, 0.95f, 0.1f); Voxel.sunCol = v(0.0f, 0.0f, 0.0f); Voxel.ambient = v(0.16f, 0.22f, 0.18f);
        Voxel.skyTop = v(0.02f, 0.03f, 0.03f); Voxel.skyHor = v(0.04f, 0.06f, 0.05f); Voxel.fogCol = v(0.08f, 0.14f, 0.1f); Voxel.fogDensity = 0.012f; Voxel.fogStart = 10;
        Voxel.exposure = 1.6f; Voxel.bloom = 0.8f; Voxel.saturation = 1.2f; Voxel.blockCol = v(1.9f, 1.3f, 0.6f); Voxel.vignette = 0.55f;
        Voxel.look(75, 26, 4, 75, 24, 110, 66);
    }

    // ------------------------------------------------------------------ 1.18 · Caves & Cliffs II: jagged snowy peaks over a spruce valley
    static void peaks() {
        Voxel.world(240, 130, 260);
        for (int x = 0; x < 240; x++) for (int z = 0; z < 260; z++) {
            float r = 1 - abs(Voxel.fbm(x * 0.012f, z * 0.012f, 91, 5) * 2 - 1), far = max(0, z - 60) / 200f;
            int h = (int) (30 + Voxel.fbm(x * 0.03f, z * 0.03f, 92, 4) * 10 + pow(r, 2.2f) * 95 * far);
            h = min(h, 126);
            int top = h > 84 ? Voxel.SNOW : h > 64 ? (Voxel.rnd(x, 3, z) > 0.4f ? Voxel.STONE : Voxel.GRAVEL) : Voxel.GRASS;
            Voxel.column(x, z, h, top, h > 64 ? Voxel.STONE : Voxel.DIRT, Voxel.STONE);
            for (int y = h + 1; y <= 31; y++) Voxel.set(x, y, z, Voxel.WATER);
        }
        for (int i = 0; i < 420; i++) { int x = (int) (Voxel.rnd(i, 6, 1) * 232) + 4, z = (int) (Voxel.rnd(i, 6, 2) * 150) + 6, g = H(x, z);
            if (Voxel.get(x, g, z) == Voxel.GRASS && g < 60 && z > 45) Voxel.spruce(x, g + 1, z, 8 + (int) (Voxel.rnd(i, 6, 3) * 7), false); }
        Voxel.sun = Voxel.n3(-0.5f, 0.38f, 0.75f); Voxel.sunCol = v(2.1f, 1.7f, 1.35f);
        Voxel.skyTop = v(0.16f, 0.36f, 0.85f); Voxel.skyHor = v(0.85f, 0.88f, 0.95f); Voxel.fogCol = v(0.78f, 0.84f, 0.92f); Voxel.ambient = v(0.45f, 0.5f, 0.62f);
        Voxel.fogDensity = 0.0012f; Voxel.fogStart = 60; Voxel.cloudY = 140; Voxel.saturation = 1.15f; Voxel.contrast = 1.1f; Voxel.dof = 2;
        Voxel.look(120, 66, 30, 120, 92, 190, 58);
    }

    // ------------------------------------------------------------------ 1.19 · The Wild Update: deep dark city with sculk and soul light
    static void deepDark() {
        Voxel.world(170, 70, 190);
        for (int x = 0; x < 170; x++) for (int z = 0; z < 190; z++) {
            int floor = (int) (12 + Voxel.fbm(x * 0.04f, z * 0.04f, 101, 3) * 4), ceil = (int) (48 - Voxel.fbm(x * 0.03f, z * 0.03f, 102, 4) * 10);
            boolean sculk = Voxel.vnoise(x * 0.08f, z * 0.08f, 103) > 0.42f;
            for (int y = 0; y < 70; y++) if (y <= floor || y >= ceil) Voxel.set(x, y, z, y == floor && sculk ? Voxel.CONCRETE + 15 : y == floor ? Voxel.OBSIDIAN : Voxel.STONE);
            if (sculk && Voxel.rnd(x, 21, z) > 0.988f) { Voxel.set(x, floor + 1, z, Voxel.CONCRETE + 9); glow(x + 0.5f, floor + 1.6f, z + 0.5f, 0.2f, 1.4f, 1.8f, 0.45f); }
        }
        int cx = 85, cz = 100, g = 1; while (Voxel.get(cx, g, cz) != Voxel.AIR) g++; // ancient city portal frame (on the cave floor)
        for (int a = -12; a <= 12; a++) for (int y = g; y <= g + 22; y++) { boolean frame = abs(a) >= 10 || y >= g + 20 || (y == g); if (frame && (abs(a) <= 12)) for (int k = 0; k < 3; k++) Voxel.set(cx + a, y, cz + k, Voxel.STONE_BRICK); }
        for (int a = -9; a <= 9; a++) for (int y = g + 1; y < g + 20; y++) if (Voxel.rnd(a, 31, y) > 0.9f) glow(cx + a + 0.5f, y + 0.5f, cz + 1.5f, 0.1f, 1.2f, 1.6f, 0.35f);
        for (int a = -10; a <= 10; a += 2) { glow(cx + a + 0.5f, g + 20.5f, cz - 0.4f, 0.2f, 1.4f, 1.8f, 0.5f); }
        for (int y = g; y <= g + 20; y += 2) { glow(cx - 10.5f, y + 0.5f, cz - 0.4f, 0.2f, 1.4f, 1.8f, 0.5f); glow(cx + 10.5f, y + 0.5f, cz - 0.4f, 0.2f, 1.4f, 1.8f, 0.5f); }
        particles(150, 30, 140, 14, 40, 20, 160, v(0.25f, 0.9f, 1.0f), 0.05f, 19);
        Voxel.sun = Voxel.n3(0.2f, 0.95f, 0.1f); Voxel.sunCol = v(0f, 0f, 0f); Voxel.ambient = v(0.1f, 0.16f, 0.22f);
        Voxel.skyTop = v(0.01f, 0.02f, 0.03f); Voxel.skyHor = v(0.02f, 0.04f, 0.06f); Voxel.fogCol = v(0.03f, 0.08f, 0.12f); Voxel.fogDensity = 0.009f; Voxel.fogStart = 14;
        Voxel.exposure = 2.2f; Voxel.bloom = 1.0f; Voxel.saturation = 1.2f; Voxel.vignette = 0.6f; Voxel.blockCol = v(0.3f, 1.4f, 1.9f);
        Voxel.look(85, 20, 40, 85, 26, 110, 66);
    }

    // ================================================================== news pictures (landscape)
    static void meadow(int X, int Z, int seed, int topA, int topB) {
        for (int x = 0; x < X; x++) for (int z = 0; z < Z; z++) {
            int h = (int) (30 + Voxel.fbm(x * 0.025f, z * 0.025f, seed, 5) * 16);
            Voxel.column(x, z, h, Voxel.rnd(x, seed, z) > 0.5f ? topA : topB, Voxel.DIRT, Voxel.STONE);
        }
    }
    static void wipe() { // TNT crater on fire: the world starts over
        Voxel.world(220, 80, 200); meadow(220, 200, 81, Voxel.GRASS, Voxel.GRASS);
        int cx = 110, cz = 90, cg = H(cx, cz);
        Voxel.carve(cx, cg + 4, cz, 19);
        for (int i = 0; i < 90; i++) { float a = (float) (Voxel.rnd(i, 1, 9) * PI * 2), r = 15 + Voxel.rnd(i, 2, 9) * 14; int x = (int) (cx + cos(a) * r), z = (int) (cz + sin(a) * r), g = H(x, z);
            if (Voxel.rnd(i, 3, 9) > 0.45f) { glow(x + 0.5f, g + 1.4f, z + 0.5f, 2.6f, 1.1f, 0.3f, 0.7f); Voxel.set(x, g + 1, z, Voxel.FIRE); } else Voxel.set(x, g + 1, z, Voxel.TNT); }
        for (int i = 0; i < 14; i++) { int x = cx - 40 + (int) (Voxel.rnd(i, 4, 1) * 80), z = cz - 44 + (int) (Voxel.rnd(i, 4, 2) * 30), g = H(x, z); for (int y = 1; y <= 1 + (int) (Voxel.rnd(i, 4, 3) * 3); y++) Voxel.set(x, g + y, z, Voxel.TNT); }
        for (int i = 0; i < 70; i++) { int x = (int) (Voxel.rnd(i, 6, 1) * 210) + 5, z = (int) (Voxel.rnd(i, 6, 2) * 190) + 5, g = H(x, z); if (hypot(x - cx, z - cz) > 40) Voxel.oak(x, g + 1, z, 5); }
        particles(300, cx - 40, cx + 40, cg, cg + 40, cz - 30, cz + 30, v(2.2f, 0.8f, 0.2f), 0.04f, 21);
        Voxel.sun = Voxel.n3(0.6f, 0.12f, 0.75f); Voxel.sunCol = v(2.2f, 1.0f, 0.55f);
        Voxel.skyTop = v(0.12f, 0.08f, 0.14f); Voxel.skyHor = v(1.0f, 0.42f, 0.2f); Voxel.fogCol = v(0.55f, 0.28f, 0.2f); Voxel.ambient = v(0.3f, 0.22f, 0.25f);
        Voxel.fogDensity = 0.007f; Voxel.cloudY = 100; Voxel.bloom = 0.6f; Voxel.blockCol = v(2f, 0.9f, 0.4f);
        Voxel.look(cx - 30, cg + 30, cz - 62, cx, cg - 2, cz, 52);
    }
    static void town() { // evening town square with a fountain: project news
        Voxel.world(160, 70, 160);
        for (int x = 0; x < 160; x++) for (int z = 0; z < 160; z++) Voxel.column(x, z, 30, Voxel.GRASS, Voxel.DIRT, Voxel.STONE);
        for (int x = 40; x < 120; x++) for (int z = 40; z < 120; z++) if (abs(x - 80) < 6 || abs(z - 80) < 6 || hypot(x - 80, z - 80) < 15) Voxel.set(x, 30, z, Voxel.rnd(x, 2, z) > 0.5f ? Voxel.GRAVEL : Voxel.COBBLE);
        for (int x = 72; x <= 88; x++) for (int z = 72; z <= 88; z++) { double d = hypot(x - 80, z - 80); if (d <= 8 && d > 6.6) Voxel.set(x, 31, z, Voxel.STONE_BRICK); else if (d <= 6.6) Voxel.set(x, 31, z, Voxel.WATER); }
        for (int y = 31; y < 35; y++) Voxel.set(80, y, 80, Voxel.STONE_BRICK); Voxel.set(80, 35, 80, Voxel.WATER);
        int[][] houses = {{48, 52}, {62, 46}, {98, 48}, {112, 56}, {46, 98}, {58, 108}, {100, 104}, {112, 92}, {84, 112}, {34, 76}, {120, 74}};
        for (int i = 0; i < houses.length; i++) { int hx = houses[i][0], hz = houses[i][1], w = 7 + (i % 3), d = 6 + (i % 2) * 2, h = 5 + (i % 2) * 3;
            for (int a = 0; a < w; a++) for (int b = 0; b < d; b++) for (int y = 31; y < 31 + h; y++) if (a == 0 || b == 0 || a == w - 1 || b == d - 1) Voxel.set(hx + a, y, hz + b, y == 31 ? Voxel.COBBLE : (a == 0 || a == w - 1) && (b == 0 || b == d - 1) ? Voxel.LOG : Voxel.PLANKS);
            for (int k = 0; k <= d / 2 + 1; k++) for (int a = -1; a <= w; a++) { Voxel.set(hx + a, 31 + h + k, hz + k - 1, Voxel.ROOF); Voxel.set(hx + a, 31 + h + k, hz + d - k, Voxel.ROOF); }
            for (int a = 2; a < w - 2; a += 3) for (int b : new int[]{0, d - 1}) { Voxel.set(hx + a, 33, hz + b, Voxel.LANTERN_GLASS); if (h > 6) Voxel.set(hx + a, 36, hz + b, Voxel.LANTERN_GLASS); } }
        for (int[] p : new int[][]{{72, 66}, {88, 66}, {66, 88}, {94, 92}, {72, 94}}) { for (int y = 31; y < 34; y++) Voxel.set(p[0], y, p[1], Voxel.LOG); Voxel.set(p[0], 34, p[1], Voxel.GLOWSTONE); glow(p[0] + 0.5f, 34.5f, p[1] + 0.5f, 2.2f, 1.5f, 0.7f, 0.5f); }
        for (int i = 0; i < 40; i++) { int x = (int) (Voxel.rnd(i, 9, 1) * 150) + 5, z = (int) (Voxel.rnd(i, 9, 2) * 150) + 5; if (hypot(x - 80, z - 80) > 52) Voxel.oak(x, 31, z, 5); }
        Voxel.sun = Voxel.n3(0.7f, 0.06f, 0.7f); Voxel.sunCol = v(0.9f, 0.45f, 0.35f);
        Voxel.skyTop = v(0.05f, 0.08f, 0.22f); Voxel.skyHor = v(0.85f, 0.45f, 0.35f); Voxel.fogCol = v(0.3f, 0.25f, 0.35f); Voxel.ambient = v(0.2f, 0.25f, 0.42f);
        Voxel.fogDensity = 0.006f; Voxel.stars = 0.35f; Voxel.bloom = 0.65f; Voxel.exposure = 1.25f; Voxel.blockCol = v(1.9f, 1.15f, 0.55f);
        Voxel.look(36, 56, 26, 84, 31, 86, 54);
    }
    static void end() { // the End: floating end stone islands, obsidian pillars with crystals
        Voxel.world(220, 110, 220);
        for (int i = 0; i < 9; i++) { float cx = i == 0 ? 110 : 30 + Voxel.rnd(i, 1, 5) * 160, cz = i == 0 ? 120 : 40 + Voxel.rnd(i, 2, 5) * 160, cy = i == 0 ? 40 : 30 + Voxel.rnd(i, 3, 5) * 30, r = i == 0 ? 44 : 6 + Voxel.rnd(i, 4, 5) * 9;
            for (int x = (int) (cx - r); x <= cx + r; x++) for (int z = (int) (cz - r); z <= cz + r; z++) { float d = (float) hypot(x - cx, z - cz) / r; if (d > 1) continue;
                int top = (int) (cy + (1 - d) * 3 + Voxel.vnoise(x * 0.2f, z * 0.2f, i) * 2), bot = (int) (cy - (1 - d * d) * r * 0.55f - Voxel.vnoise(x * 0.3f, z * 0.3f, i + 9) * 4);
                for (int y = bot; y <= top; y++) Voxel.set(x, y, z, Voxel.END_STONE); } }
        for (int k = 0; k < 8; k++) { double a = k * PI / 4 + 0.3; int px = (int) (110 + cos(a) * 30), pz = (int) (120 + sin(a) * 30), g = H(px, pz), ph = 18 + (k * 7) % 22;
            for (int y = g; y < g + ph; y++) for (int a2 = -2; a2 <= 2; a2++) for (int b = -2; b <= 2; b++) if (a2 * a2 + b * b <= 5) Voxel.set(px + a2, y, pz + b, Voxel.OBSIDIAN);
            Voxel.set(px, g + ph, pz, Voxel.CRYSTAL); glow(px + 0.5f, g + ph + 1.2f, pz + 0.5f, 2.4f, 1.4f, 2.8f, 1.1f); }
        particles(160, 40, 180, 30, 90, 40, 200, v(0.9f, 0.6f, 1.4f), 0.03f, 31);
        Voxel.shadows = false; Voxel.sun = Voxel.n3(0, 1, 0); Voxel.sunCol = v(0.25f, 0.22f, 0.3f); Voxel.sunSize = 2;
        Voxel.skyTop = v(0.008f, 0.0f, 0.025f); Voxel.skyHor = v(0.11f, 0.04f, 0.17f); Voxel.fogCol = v(0.1f, 0.04f, 0.14f); Voxel.ambient = v(0.66f, 0.58f, 0.66f);
        Voxel.stars = 1; Voxel.fogDensity = 0.004f; Voxel.bloom = 0.7f; Voxel.exposure = 1.2f; Voxel.saturation = 1.15f;
        Voxel.look(38, 66, 32, 108, 46, 122, 58);
    }
    static void dungeon() { // monster spawners in a mossy dungeon lit by torches
        Voxel.world(70, 40, 70);
        for (int x = 0; x < 70; x++) for (int z = 0; z < 70; z++) for (int y = 0; y < 40; y++) Voxel.set(x, y, z, Voxel.STONE);
        for (int x = 14; x < 56; x++) for (int z = 14; z < 56; z++) for (int y = 10; y < 22; y++) {
            boolean wall = x == 14 || z == 14 || x == 55 || z == 55 || y == 10 || y == 21;
            Voxel.set(x, y, z, wall ? (Voxel.rnd(x, y, z) > 0.5f ? Voxel.MOSSY : Voxel.COBBLE) : Voxel.AIR); }
        for (int[] p : new int[][]{{35, 35}, {24, 44}, {46, 26}}) { Voxel.set(p[0], 11, p[1], Voxel.SPAWNER); glow(p[0] + 0.5f, 11.5f, p[1] + 0.5f, 2.2f, 0.8f, 0.2f, 0.6f); }
        for (int[] p : new int[][]{{15, 15, 30}, {15, 15, 46}, {54, 15, 30}, {30, 15, 54}, {46, 15, 54}, {30, 15, 15}, {46, 15, 15}}) { Voxel.set(p[0], p[1], p[2], Voxel.TORCH); glow(p[0] + 0.5f, p[1] + 0.7f, p[2] + 0.5f, 2.6f, 1.6f, 0.7f, 0.35f); }
        for (int[] p : new int[][]{{20, 20}, {50, 50}, {50, 20}}) { Voxel.set(p[0], 11, p[1], Voxel.PLANKS); }
        particles(120, 20, 50, 11, 16, 20, 50, v(2f, 0.9f, 0.3f), 0.025f, 41);
        Voxel.nether = true; Voxel.shadows = false; Voxel.ambient = v(0.16f, 0.17f, 0.22f); Voxel.blockCol = v(2.1f, 1.25f, 0.6f);
        Voxel.fogCol = v(0.02f, 0.02f, 0.03f); Voxel.skyTop = Voxel.fogCol; Voxel.skyHor = Voxel.fogCol; Voxel.fogDensity = 0.015f; Voxel.fogStart = 6; Voxel.exposure = 2.1f; Voxel.bloom = 0.6f; Voxel.vignette = 0.6f;
        Voxel.look(26, 15.5f, 25, 35.5f, 11.4f, 35.5f, 64);
    }
    static void apiary() { // flower meadow with bee nests on the trees
        Voxel.world(200, 80, 200); meadow(200, 200, 91, Voxel.FLOWER_GRASS, Voxel.GRASS);
        for (int i = 0; i < 120; i++) { int x = (int) (Voxel.rnd(i, 3, 1) * 190) + 5, z = (int) (Voxel.rnd(i, 3, 2) * 190) + 5, g = H(x, z); if (abs(x - 100) < 8 && z < 50) continue;
            int h = 5 + (int) (Voxel.rnd(i, 3, 3) * 2); Voxel.oak(x, g + 1, z, h); if (Voxel.rnd(i, 3, 4) > 0.45f) { Voxel.set(x + 1, g + h - 3, z, Voxel.BEE_NEST); if (Voxel.rnd(i, 3, 5) > 0.5f) Voxel.set(x - 1, g + h - 2, z, Voxel.BEE_NEST); } }
        { int tx = 106, tz = 48, g = H(tx, tz); Voxel.oak(tx, g + 1, tz, 7); Voxel.set(tx + 1, g + 4, tz, Voxel.BEE_NEST); Voxel.set(tx - 1, g + 5, tz, Voxel.BEE_NEST); Voxel.set(tx, g + 4, tz - 1, Voxel.BEE_NEST); }
        for (int i = 0; i < 260; i++) { float x = 96 + Voxel.rnd(i, 5, 1) * 20, y = H(106, 48) + 1 + Voxel.rnd(i, 5, 2) * 8, z = 36 + Voxel.rnd(i, 5, 3) * 20; Voxel.PARTICLES.add(new float[]{x, y, z, 1.4f, 1.05f, 0.1f, 0.12f, 0}); Voxel.PARTICLES.add(new float[]{x + 0.1f, y + 0.02f, z, 0.06f, 0.05f, 0.03f, 0.06f, 0}); }
        Voxel.sun = Voxel.n3(-0.5f, 0.55f, 0.6f); Voxel.sunCol = v(2.0f, 1.75f, 1.2f);
        Voxel.skyTop = v(0.22f, 0.48f, 0.95f); Voxel.skyHor = v(0.95f, 0.92f, 0.82f); Voxel.fogCol = v(0.95f, 0.9f, 0.78f); Voxel.ambient = v(0.42f, 0.46f, 0.58f);
        Voxel.fogDensity = 0.0055f; Voxel.cloudY = 100; Voxel.dof = 3; Voxel.saturation = 1.15f;
        { int g = H(98, 26); Voxel.look(98, g + 6, 26, 106, g + 4, 48, 58); Voxel.focus = 24; }
    }
    static void floatingIsland() { // spawn island with a castle and a waterfall above the clouds
        Voxel.world(220, 130, 220);
        float cx = 110, cz = 115, cy = 70, r = 40;
        for (int x = (int) (cx - r); x <= cx + r; x++) for (int z = (int) (cz - r); z <= cz + r; z++) { float d = (float) hypot(x - cx, (z - cz) * 1.2f) / r + (Voxel.vnoise(x * 0.08f, z * 0.08f, 3) - 0.5f) * 0.25f; if (d > 1) continue;
            int top = (int) (cy + (1 - d) * 4), bot = (int) (cy - (1 - d) * (1 - d) * 46 - Voxel.vnoise(x * 0.25f, z * 0.25f, 4) * 10);
            for (int y = bot; y <= top; y++) Voxel.set(x, y, z, y == top ? Voxel.GRASS : y > top - 3 ? Voxel.DIRT : Voxel.STONE); }
        int kx = 98, kz = 108, kg = H(kx, kz); // castle: keep + 4 towers
        for (int a = 0; a < 24; a++) for (int b = 0; b < 20; b++) for (int y = kg; y < kg + 10; y++) if ((a == 0 || b == 0 || a == 23 || b == 19) && !(y == kg + 9 && (a + b) % 2 == 1)) Voxel.set(kx + a, y, kz + b, Voxel.STONE_BRICK);
        for (int[] t : new int[][]{{0, 0}, {23, 0}, {0, 19}, {23, 19}}) { for (int y = kg; y < kg + 17; y++) for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++) if (a * a + b * b <= 5) Voxel.set(kx + t[0] + a, y, kz + t[1] + b, Voxel.STONE_BRICK);
            for (int k = 0; k < 4; k++) for (int a = -3 + k; a <= 3 - k; a++) for (int b = -3 + k; b <= 3 - k; b++) Voxel.set(kx + t[0] + a, kg + 17 + k, kz + t[1] + b, Voxel.ROOF);
            Voxel.set(kx + t[0], kg + 12, kz + t[1] - 3, Voxel.WOOL_RED); Voxel.set(kx + t[0], kg + 11, kz + t[1] - 3, Voxel.WOOL_RED); }
        for (int a = 6; a < 18; a++) for (int b = 5; b < 15; b++) for (int y = kg; y < kg + 15; y++) if (a == 6 || b == 5 || a == 17 || b == 14) Voxel.set(kx + a, y, kz + b, Voxel.STONE_BRICK);
        for (int k = 0; k < 6; k++) for (int a = 5 + k; a <= 18 - k; a++) for (int b = 4 + k; b <= 15 - k; b++) Voxel.set(kx + a, kg + 15 + k, kz + b, Voxel.ROOF);
        for (int y = 0; y <= (int) cy + 2; y++) for (int w = 0; w < 3; w++) if (Voxel.get(78 + w, y, 96) == Voxel.AIR || y > cy - 4) Voxel.set(78 + w, y, 96, Voxel.WATER);
        for (int i = 0; i < 40; i++) { int x = (int) (cx - 34 + Voxel.rnd(i, 4, 1) * 68), z = (int) (cz - 30 + Voxel.rnd(i, 4, 2) * 60), g = H(x, z); if (Voxel.get(x, g, z) == Voxel.GRASS && (x < kx - 3 || x > kx + 27 || z < kz - 3 || z > kz + 23)) Voxel.oak(x, g + 1, z, 5); }
        Voxel.sun = Voxel.n3(-0.7f, 0.25f, 0.55f); Voxel.sunCol = v(2.3f, 1.55f, 1.05f);
        Voxel.skyTop = v(0.14f, 0.34f, 0.88f); Voxel.skyHor = v(0.95f, 0.78f, 0.72f); Voxel.fogCol = v(0.85f, 0.78f, 0.78f); Voxel.ambient = v(0.38f, 0.42f, 0.58f);
        Voxel.fogDensity = 0.0015f; Voxel.fogStart = 60; Voxel.cloudY = 30; Voxel.saturation = 1.12f;
        Voxel.look(46, 88, 40, 108, 70, 116, 54);
    }
}
