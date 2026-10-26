import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Small voxel path renderer for the launcher artwork (version posters and news pictures).
 * Minecraft-style worlds, rendered "cinematic": sun + shadows, Minecraft smooth lighting (vertex AO),
 * block light from lava/torches/shroomlights, sky light, water with reflections, clouds, fog, bloom, depth of field.
 *   javac Voxel.java && java Voxel <scene> <out.png> <width> <height>
 */
public class Voxel {
    // ------------------------------------------------------------------ blocks
    static final int AIR = 0, STONE = 1, DIRT = 2, GRASS = 3, SAND = 4, WATER = 5, LOG = 6, LEAVES = 7, PLANKS = 8, COBBLE = 9, SNOW = 10,
        RED_SAND = 12, NETHERRACK = 13, NYLIUM = 14, CRIMSON_STEM = 15, WART = 16, SHROOMLIGHT = 17, LAVA = 18, CHERRY_LOG = 19, CHERRY_LEAVES = 20,
        PALE_LOG = 21, PALE_LEAVES = 22, PALE_MOSS = 23, HANGING_MOSS = 24, CREAKING_HEART = 25, END_STONE = 26, OBSIDIAN = 27, CRYSTAL = 28, SPAWNER = 29,
        MOSSY = 30, TORCH = 31, TNT = 32, FIRE = 33, BEE_NEST = 34, FLOWER_GRASS = 35, PETAL_GRASS = 36, KELP = 37, SANDSTONE = 38, GLOWSTONE = 39,
        WOOL_RED = 40, STONE_BRICK = 41, SPRUCE_LOG = 42, SPRUCE_LEAVES = 43, SNOW_LEAVES = 44, ICE = 45, ROOF = 46, LANTERN_GLASS = 47, GRAVEL = 48,
        CONCRETE = 60 /* 60..75 */, TERRACOTTA = 80 /* 80..89 */, CORAL = 90 /* 90..94 */;
    static final int[] COL = new int[256];
    static final float[] EMIT = new float[256];
    static final boolean[] SOLID = new boolean[256], PLANT = new boolean[256];
    static {
        def(STONE, 0x7d7d7d); def(DIRT, 0x8a5e3c); def(GRASS, 0x6aa84f); def(SAND, 0xe0d39a); def(LOG, 0x6b5233); def(LEAVES, 0x3f7d2a); def(PLANKS, 0xa8834f);
        def(COBBLE, 0x7a7a7a); def(SNOW, 0xf2f6fa); def(RED_SAND, 0xbd6630); def(NETHERRACK, 0x6b2b2b); def(NYLIUM, 0x8f1f24); def(CRIMSON_STEM, 0x7a2f45);
        def(WART, 0x8e1a1a); def(SHROOMLIGHT, 0xf3a14b); EMIT[SHROOMLIGHT] = 1.6f; def(LAVA, 0xff7a1a); EMIT[LAVA] = 2.4f; def(CHERRY_LOG, 0x3b2333);
        def(CHERRY_LEAVES, 0xf2a7c9); def(PALE_LOG, 0xd9d2cc); def(PALE_LEAVES, 0x353d37); def(PALE_MOSS, 0x8c948a); def(HANGING_MOSS, 0x6d756c);
        def(CREAKING_HEART, 0xe8762c); EMIT[CREAKING_HEART] = 2.2f; def(END_STONE, 0xe9e2a2); def(OBSIDIAN, 0x1b1426); def(CRYSTAL, 0xe7b8ff); EMIT[CRYSTAL] = 3f;
        def(SPAWNER, 0x262a33); EMIT[SPAWNER] = 0.6f; def(MOSSY, 0x6c7d5a); def(TNT, 0xc8382c); def(BEE_NEST, 0xd9a43c); def(FLOWER_GRASS, 0x6aa84f);
        def(PETAL_GRASS, 0x6aa84f); def(KELP, 0x3e7a2c); def(SANDSTONE, 0xd8c98f); def(GLOWSTONE, 0xffd27a); EMIT[GLOWSTONE] = 2f; def(WOOL_RED, 0xa1272a);
        def(STONE_BRICK, 0x7e7e80); def(SPRUCE_LOG, 0x4a3420); def(SPRUCE_LEAVES, 0x2f5236); def(SNOW_LEAVES, 0xe8eef2); def(ICE, 0x9cc4f5); def(ROOF, 0x5a3a2a);
        def(LANTERN_GLASS, 0xffc56e); EMIT[LANTERN_GLASS] = 1.8f; def(GRAVEL, 0x8a8480);
        int[] conc = {0xcfd5d6, 0xe06101, 0xa9309f, 0x2489c7, 0xf1af15, 0x5ea918, 0xd5658f, 0x36393d, 0x7d7d73, 0x157788, 0x64209c, 0x2d2f8f, 0x603c20, 0x495b24, 0x8e2121, 0x080a0f};
        for (int i = 0; i < 16; i++) def(CONCRETE + i, conc[i]);
        int[] terra = {0xd1b1a1, 0xa15325, 0xba8523, 0x985e43, 0x8e3c2e, 0x87695f, 0x4d3324, 0xa25b36, 0xc4a28a, 0x5f3a2b};
        for (int i = 0; i < 10; i++) def(TERRACOTTA + i, terra[i]);
        int[] cor = {0x2f6ff5, 0xd84e9c, 0xd9b52b, 0xc23b3b, 0xa53fd6};
        for (int i = 0; i < 5; i++) def(CORAL + i, cor[i]);
        TORCH_EMIT();
    }
    static void TORCH_EMIT() { EMIT[TORCH] = 0; EMIT[FIRE] = 0; SOLID[TORCH] = false; SOLID[FIRE] = false; }
    static void def(int id, int rgb) { COL[id] = rgb; SOLID[id] = true; }

    // ------------------------------------------------------------------ world
    static int X, Y, Z;
    static byte[] W;
    static byte[] SKY, BLK; // light levels 0..15
    static int idx(int x, int y, int z) { return (y * Z + z) * X + x; }
    static int get(int x, int y, int z) { return x < 0 || y < 0 || z < 0 || x >= X || y >= Y || z >= Z ? AIR : W[idx(x, y, z)] & 255; }
    static void set(int x, int y, int z, int b) { if (x >= 0 && y >= 0 && z >= 0 && x < X && y < Y && z < Z) W[idx(x, y, z)] = (byte) b; }
    static void setIfAir(int x, int y, int z, int b) { if (get(x, y, z) == AIR) set(x, y, z, b); }
    static boolean solid(int x, int y, int z) { int b = get(x, y, z); return b != AIR && SOLID[b]; }
    static void world(int x, int y, int z) { X = x; Y = y; Z = z; W = new byte[x * y * z]; }

    // ------------------------------------------------------------------ noise
    static int hash(int x, int y, int z) { int h = x * 374761393 + y * 668265263 + z * 1274126177; h = (h ^ (h >>> 13)) * 1274126177; return h ^ (h >>> 16); }
    static float rnd(int x, int y, int z) { return (hash(x, y, z) & 0xFFFFFF) / (float) 0xFFFFFF; }
    static float vnoise(float x, float z, int seed) {
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z); float fx = x - xi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        float a = rnd(xi, seed, zi), b = rnd(xi + 1, seed, zi), c = rnd(xi, seed, zi + 1), d = rnd(xi + 1, seed, zi + 1);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }
    static float fbm(float x, float z, int seed, int oct) { float s = 0, amp = 0.5f, f = 1; for (int i = 0; i < oct; i++) { s += amp * vnoise(x * f, z * f, seed + i * 31); f *= 2.03f; amp *= 0.5f; } return s; }

    // ------------------------------------------------------------------ builders
    static void column(int x, int z, int h, int top, int sub, int deep) { for (int y = 0; y <= h && y < Y; y++) set(x, y, z, y == h ? top : y > h - 4 ? sub : deep); }
    static void ball(float cx, float cy, float cz, float r, int b, float holes, boolean onlyAir) {
        for (int x = (int) (cx - r - 1); x <= cx + r + 1; x++) for (int y = (int) (cy - r - 1); y <= cy + r + 1; y++) for (int z = (int) (cz - r - 1); z <= cz + r + 1; z++) {
            float d = (x - cx) * (x - cx) + (y - cy) * (y - cy) * 1.2f + (z - cz) * (z - cz);
            if (d <= r * r && rnd(x, y, z) >= holes * (d / (r * r))) { if (onlyAir) setIfAir(x, y, z, b); else set(x, y, z, b); } }
    }
    static void carve(float cx, float cy, float cz, float r) { for (int x = (int) (cx - r); x <= cx + r; x++) for (int y = (int) (cy - r); y <= cy + r; y++) for (int z = (int) (cz - r); z <= cz + r; z++) if ((x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz) <= r * r) set(x, y, z, AIR); }
    static int top(int x, int z) { for (int y = Y - 1; y >= 0; y--) if (solid(x, y, z) || get(x, y, z) == WATER) return y; return -1; }
    static int ground(int x, int z) { for (int y = Y - 1; y >= 0; y--) { int b = get(x, y, z); if (b != AIR && b != LEAVES && b != CHERRY_LEAVES && b != PALE_LEAVES && b != SPRUCE_LEAVES && b != SNOW_LEAVES && b != HANGING_MOSS && !PLANT[b]) return y; } return -1; }
    /** first air block from the bottom (for worlds with a ceiling, like the Nether) */
    static int floorY(int x, int z) { for (int y = 0; y < Y; y++) if (get(x, y, z) == AIR) return y - 1; return Y - 1; }
    static void oak(int x, int y, int z, int h) { for (int i = 0; i < h; i++) set(x, y + i, z, LOG); ball(x, y + h - 1, z, 2.7f, LEAVES, 0.5f, true); ball(x, y + h + 1, z, 1.7f, LEAVES, 0.3f, true); }
    static void spruce(int x, int y, int z, int h, boolean snow) {
        for (int i = 0; i < h; i++) set(x, y + i, z, SPRUCE_LOG);
        for (int i = 2; i <= h + 1; i++) { float r = (h + 1 - i) * 0.42f + (i % 2 == 0 ? 0.6f : 0f);
            for (int dx = (int) -r; dx <= r; dx++) for (int dz = (int) -r; dz <= r; dz++) if (dx * dx + dz * dz <= r * r + 0.5f && (dx != 0 || dz != 0 || i > h - 1)) {
                setIfAir(x + dx, y + i, z + dz, SPRUCE_LEAVES); if (snow && get(x + dx, y + i + 1, z + dz) == AIR) set(x + dx, y + i + 1, z + dz, SNOW_LEAVES); } }
    }
    static void cherry(int x, int y, int z, int h, int seed) {
        int cx = x, cz = z; float dx = rnd(seed, 1, 2) - 0.5f, dz = rnd(seed, 3, 4) - 0.5f;
        for (int i = 0; i < h; i++) { set(cx, y + i, cz, CHERRY_LOG); if (i > h / 2) { cx = Math.round(x + dx * 2 * (i - h / 2f)); cz = Math.round(z + dz * 2 * (i - h / 2f)); set(cx, y + i, cz, CHERRY_LOG); } }
        int ty = y + h;
        for (int dy = -1; dy <= 2; dy++) { float r = dy == 2 ? 2.5f : dy == -1 ? 3.5f : 4.6f;
            for (int ax = (int) -r; ax <= r; ax++) for (int az = (int) -r; az <= r; az++) if (ax * ax + az * az <= r * r && rnd(cx + ax, ty + dy, cz + az) > (dy == -1 ? 0.35f : 0.05f)) setIfAir(cx + ax, ty + dy, cz + az, CHERRY_LEAVES); }
        for (int k = 0; k < 10; k++) { int ax = (int) ((rnd(seed, k, 9) - 0.5f) * 8), az = (int) ((rnd(seed, 9, k) - 0.5f) * 8); int len = 1 + (int) (rnd(k, seed, 1) * 2); for (int i = 1; i <= len; i++) if (get(cx + ax, ty - 1, cz + az) == CHERRY_LEAVES) setIfAir(cx + ax, ty - 1 - i, cz + az, CHERRY_LEAVES); }
    }
    static void paleOak(int x, int y, int z, int h, boolean heart) {
        for (int i = 0; i < h; i++) for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) set(x + a, y + i, z + b, PALE_LOG);
        if (heart) set(x, y + h / 2, z, CREAKING_HEART);
        for (int dy = -2; dy <= 1; dy++) { float r = dy == 1 ? 3f : 5.2f;
            for (int ax = (int) -r; ax <= r + 1; ax++) for (int az = (int) -r; az <= r + 1; az++) if ((ax - .5f) * (ax - .5f) + (az - .5f) * (az - .5f) <= r * r && rnd(x + ax, y + h + dy, z + az) > 0.08f) setIfAir(x + ax, y + h + dy, z + az, PALE_LEAVES); }
        for (int ax = -5; ax <= 6; ax++) for (int az = -5; az <= 6; az++) if (get(x + ax, y + h - 2, z + az) == PALE_LEAVES && rnd(ax, az, x + z) > 0.62f) { int len = 1 + (int) (rnd(az, ax, 7) * 4); for (int i = 1; i <= len; i++) setIfAir(x + ax, y + h - 2 - i, z + az, HANGING_MOSS); }
    }
    static void fungus(int x, int y, int z, int h) {
        for (int i = 0; i < h; i++) set(x, y + i, z, CRIMSON_STEM);
        int r = 3 + h / 6;
        for (int dy = 0; dy <= 3; dy++) for (int ax = -r; ax <= r; ax++) for (int az = -r; az <= r; az++) {
            float d = (float) Math.sqrt(ax * ax + az * az); int yy = y + h - dy;
            if (dy == 0 ? d <= r - 1 : (d <= r && d > r - 1.5f && rnd(ax, yy, az) > 0.25f * dy)) setIfAir(x + ax, yy, z + az, rnd(x + ax, yy, z + az) > 0.9f ? SHROOMLIGHT : WART);
        }
        setIfAir(x, y + h + 1, z, WART);
    }

    // ------------------------------------------------------------------ lighting (Minecraft style block & sky light)
    static void computeLight(boolean hasSky) {
        int n = X * Y * Z; SKY = new byte[n]; BLK = new byte[n];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        if (hasSky) for (int x = 0; x < X; x++) for (int z = 0; z < Z; z++) for (int y = Y - 1; y >= 0; y--) { int b = get(x, y, z); if (b != AIR && SOLID[b] && !leafy(b)) break; SKY[idx(x, y, z)] = 15; q.add(new int[]{x, y, z, 15}); if (leafy(b)) break; }
        spread(q, SKY);
        for (int x = 0; x < X; x++) for (int y = 0; y < Y; y++) for (int z = 0; z < Z; z++) { int b = get(x, y, z); int lv = b == LAVA || b == GLOWSTONE || b == SHROOMLIGHT || b == CRYSTAL || b == LANTERN_GLASS ? 15 : b == TORCH || b == FIRE ? 14 : b == CREAKING_HEART ? 13 : b == SPAWNER ? 9 : 0;
            if (lv > 0) { BLK[idx(x, y, z)] = (byte) lv; q.add(new int[]{x, y, z, lv}); } }
        spread(q, BLK);
    }
    static boolean leafy(int b) { return b == LEAVES || b == CHERRY_LEAVES || b == PALE_LEAVES || b == SPRUCE_LEAVES || b == SNOW_LEAVES || b == HANGING_MOSS; }
    static void spread(ArrayDeque<int[]> q, byte[] L) {
        int[][] D = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) { int[] c = q.poll(); for (int[] d : D) { int x = c[0] + d[0], y = c[1] + d[1], z = c[2] + d[2]; if (x < 0 || y < 0 || z < 0 || x >= X || y >= Y || z >= Z) continue;
            int b = get(x, y, z); if (b != AIR && SOLID[b] && !leafy(b) && b != WATER) continue; int lv = c[3] - (leafy(b) || b == WATER ? 2 : 1); if (lv <= (L[idx(x, y, z)] & 255)) continue; L[idx(x, y, z)] = (byte) lv; q.add(new int[]{x, y, z, lv}); } }
    }
    static float light(byte[] L, int x, int y, int z) { return x < 0 || y < 0 || z < 0 || x >= X || y >= Y || z >= Z ? (L == SKY ? 15 : 0) : L[idx(x, y, z)] & 255; }

    // ------------------------------------------------------------------ scene settings
    static float[] sun = n3(-0.5f, 0.35f, 0.7f), sunCol = {1.6f, 1.45f, 1.25f}, skyTop = {0.25f, 0.5f, 0.95f}, skyHor = {0.75f, 0.85f, 1f}, fogCol = {0.75f, 0.85f, 1f}, ambient = {0.38f, 0.45f, 0.58f}, blockCol = {1.7f, 1.05f, 0.55f};
    static float seaLevel = -1, fogStart = 25, fogDensity = 0.004f, heightFog = 0, heightFogY = 0, cloudY = 0, sunSize = 0.9995f, exposure = 0.85f, stars = 0, aurora = 0, saturation = 1.05f, contrast = 1.05f, vignette = 0.35f, bloom = 0.35f, dof = 0, focus = 60, waterAbs = 0.18f;
    static float[] waterCol = {0.08f, 0.32f, 0.5f}, tint = {1, 1, 1};
    static boolean shadows = true, nether = false;
    static float[] cam = {0, 0, 0}; static float yaw, pitch, fov = 60;
    static final List<float[]> GLOWS = new ArrayList<>(); // x,y,z,r,g,b,size
    static final List<float[]> PARTICLES = new ArrayList<>(); // x,y,z,r,g,b,size,kind

    static float[] n3(float x, float y, float z) { float l = (float) Math.sqrt(x * x + y * y + z * z); return new float[]{x / l, y / l, z / l}; }

    // ------------------------------------------------------------------ sky
    static float[] sky(float[] d) { return sky(d, false); }
    static float[] sky(float[] d, boolean reflection) {
        float t = Math.max(0, d[1]);
        float[] c = new float[3];
        for (int i = 0; i < 3; i++) c[i] = skyHor[i] + (skyTop[i] - skyHor[i]) * (float) Math.pow(t, 0.42);
        if (!nether) {
            float sd = Math.max(0, d[0] * sun[0] + d[1] * sun[1] + d[2] * sun[2]);
            float glow = (float) Math.pow(sd, 6) * 0.35f + (float) Math.pow(sd, 90) * 1.2f + (sd > sunSize && !reflection ? 22f : 0);
            for (int i = 0; i < 3; i++) c[i] += glow * sunCol[i] * 0.55f;
            float ck = cloudY > 0 && Math.abs(d[1]) > 0.02f ? (cloudY - cam[1]) / d[1] : -1;
            if (ck > 0) { float k = ck, px = cam[0] + d[0] * k, pz = cam[2] + d[2] * k;
                float n = fbm(px * 0.01f, pz * 0.018f, 77, 5); float cov = smooth(0.56f, 0.74f, n) * Math.min(1, Math.abs(d[1]) * 6) * (float) Math.exp(-k * 0.0012f);
                float lit = 0.55f + 0.45f * smooth(0.56f, 0.8f, n); float[] cc = {(sunCol[0] * 0.45f + ambient[0]) * lit, (sunCol[1] * 0.45f + ambient[1]) * lit, (sunCol[2] * 0.45f + ambient[2]) * lit};
                for (int i = 0; i < 3; i++) c[i] = c[i] * (1 - cov * 0.9f) + cc[i] * cov * 0.9f; }
            if (stars > 0 && d[1] > 0) { int sx = (int) (d[0] * 900), sy = (int) (d[1] * 900), sz = (int) (d[2] * 900); float r = rnd(sx, sy, sz); if (r > 0.9965f) { float b = (r - 0.9965f) / 0.0035f * stars * 2.5f; c[0] += b; c[1] += b; c[2] += b * 1.1f; } }
            if (aurora > 0 && d[1] > 0.05f) { float k = 1 / d[1], px = d[0] * k, pz = d[2] * k;
                float band = (float) Math.exp(-Math.pow((pz + 0.6f * (float) Math.sin(px * 1.3f) - 1.4f) * 2.2f, 2)) + 0.7f * (float) Math.exp(-Math.pow((pz + 0.5f * (float) Math.sin(px * 0.9f + 2) - 2.6f) * 2.5f, 2));
                float rays = 0.6f + 0.4f * vnoise(px * 9, 0, 5); float a = band * rays * aurora * Math.min(1, d[1] * 3) * (1 - Math.min(1, d[1]));
                c[0] += a * 0.1f; c[1] += a * 1.0f; c[2] += a * 0.55f; c[0] += a * a * 0.3f; }
        }
        return c;
    }
    static float smooth(float a, float b, float x) { float t = Math.max(0, Math.min(1, (x - a) / (b - a))); return t * t * (3 - 2 * t); }

    // ------------------------------------------------------------------ textures (procedural 16x16, low contrast)
    static float[] albedo(int b, int face, int u, int v, int x, int y, int z) {
        int rgb = COL[b]; float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, bl = (rgb & 255) / 255f;
        r = (float) Math.pow(r, 2.2); g = (float) Math.pow(g, 2.2); bl = (float) Math.pow(bl, 2.2);
        float n = rnd(x * 16 + u, y * 16 + v + face * 997, z * 16 + u * 7 + v), m = 1;
        boolean topFace = face == 2, side = face != 2 && face != 3;
        switch (b) {
            case GRASS: case FLOWER_GRASS: case PETAL_GRASS:
                if (side && v > 2 + (int) (rnd(x, u, z) * 2.5f)) { int d = COL[DIRT]; r = lin(d >> 16); g = lin(d >> 8); bl = lin(d); m = 0.82f + n * 0.25f; }
                else { m = 0.8f + n * 0.35f; float hue = vnoise(x * 0.08f, z * 0.08f, 3); g *= 0.9f + hue * 0.2f; r *= 0.85f + hue * 0.3f;
                    if (topFace && b == FLOWER_GRASS && rnd(x * 16 + u / 3, 5, z * 16 + v / 3) > 0.93f) { float k = rnd(x, z, u / 3 + v); if (k < 0.33f) { r = 0.8f; g = 0.08f; bl = 0.05f; } else if (k < 0.66f) { r = 0.9f; g = 0.75f; bl = 0.05f; } else { r = 0.55f; g = 0.35f; bl = 0.9f; } m = 1; }
                    if (topFace && b == PETAL_GRASS && rnd(x * 16 + u / 2, 9, z * 16 + v / 2) > 0.72f) { r = 0.92f; g = 0.55f; bl = 0.7f; m = 0.9f + n * 0.15f; } }
                break;
            case LOG: case CHERRY_LOG: case PALE_LOG: case SPRUCE_LOG: case CRIMSON_STEM:
                if (face == 2 || face == 3) { float d = (float) Math.hypot(u - 7.5, v - 7.5); m = d > 6.5f ? 0.7f : 1.15f - (((int) d) % 3 == 0 ? 0.15f : 0) + n * 0.06f; }
                else m = 0.82f + (rnd(x * 16 + u, z, 1) * 0.35f) * (u % 3 == 0 ? 0.6f : 1) + n * 0.06f;
                break;
            case LEAVES: case CHERRY_LEAVES: case PALE_LEAVES: case SPRUCE_LEAVES: case HANGING_MOSS: m = n < 0.18f ? 0.55f : 0.85f + n * 0.3f; break;
            case PLANKS: m = (v % 4 == 0 ? 0.72f : 0.95f + n * 0.12f) * (((v / 4) + (u > 7 ? 1 : 0)) % 2 == 0 && u % 8 == 0 ? 0.8f : 1); break;
            case COBBLE: case MOSSY: { float c = vnoise((x * 16 + u) * 0.45f, (y * 16 + v + z * 16) * 0.45f, 11); m = c < 0.32f ? 0.6f : 0.85f + c * 0.35f; if (b == MOSSY && vnoise((x * 16 + u) * 0.3f, (y * 16 + v) * 0.3f + z, 4) > 0.55f) { r = 0.22f; g = 0.32f; bl = 0.12f; } break; }
            case STONE_BRICK: m = (v % 8 == 0 || (u + (v / 8 % 2) * 8) % 16 == 0) ? 0.62f : 0.9f + n * 0.14f; break;
            case TNT: if (side && v >= 5 && v <= 10) { r = 0.92f; g = 0.92f; bl = 0.9f; m = (u > 3 && u < 12 && v > 6 && v < 9) ? 0.15f : 1; } else m = side ? (u % 4 == 0 ? 0.75f : 1) : 0.8f; break;
            case BEE_NEST: m = side ? (v % 4 == 0 ? 0.7f : 1) * (face == 0 && u > 5 && u < 10 && v > 6 && v < 11 ? 0.25f : 1) : 0.95f; break;
            case SPAWNER: { boolean bar = u % 5 == 0 || v % 5 == 0; if (!bar) { r = 1.6f; g = 0.55f; bl = 0.12f; m = 0.4f + n * 0.6f; } else m = 0.9f; break; }
            case TERRACOTTA: case TERRACOTTA + 1: case TERRACOTTA + 2: case TERRACOTTA + 3: case TERRACOTTA + 4: case TERRACOTTA + 5: case TERRACOTTA + 6: case TERRACOTTA + 7: case TERRACOTTA + 8: case TERRACOTTA + 9: m = 0.93f + n * 0.08f; break;
            case LAVA: { float w = vnoise((x * 16 + u) * 0.18f, (z * 16 + v) * 0.18f, 21); m = 0.75f + w * 0.6f; break; }
            case WART: case NYLIUM: m = 0.8f + n * 0.35f; if (b == NYLIUM && side && v > 3) { int d = COL[NETHERRACK]; r = lin(d >> 16); g = lin(d >> 8); bl = lin(d); } break;
            case WOOL_RED: m = 0.85f + n * 0.2f; if (side && v > 4 && v < 10 && u > 4 && u < 11) { r = 0.9f; g = 0.75f; bl = 0.15f; } break;
            case ROOF: m = (v % 4 == 0 ? 0.6f : 0.9f + n * 0.15f); break;
            case LANTERN_GLASS: m = (u % 8 == 0 || v % 8 == 0) ? 0.35f : 1f; break;
            default: m = 0.86f + n * 0.22f;
        }
        return new float[]{r * m, g * m, bl * m};
    }
    static float lin(int c) { return (float) Math.pow((c & 255) / 255f, 2.2); }

    // ------------------------------------------------------------------ ray traversal
    static final class Hit { int b = AIR, x, y, z, face; float t, u, v; float waterT = -1; }
    static Hit trace(float ox, float oy, float oz, float dx, float dy, float dz, float maxT, boolean shadow) {
        Hit h = new Hit();
        int x = (int) Math.floor(ox), y = (int) Math.floor(oy), z = (int) Math.floor(oz);
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
        float tdx = Math.abs(1 / dx), tdy = Math.abs(1 / dy), tdz = Math.abs(1 / dz);
        float tmx = (dx > 0 ? (x + 1 - ox) : (ox - x)) * tdx, tmy = (dy > 0 ? (y + 1 - oy) : (oy - y)) * tdy, tmz = (dz > 0 ? (z + 1 - oz) : (oz - z)) * tdz;
        float t = 0; int face = -1;
        for (int i = 0; i < 900 && t < maxT; i++) {
            if (y >= Y && dy > 0) break; if ((x < 0 && dx < 0) || (x >= X && dx > 0) || (z < 0 && dz < 0) || (z >= Z && dz > 0) || (y < 0 && dy < 0)) break;
            int b = get(x, y, z);
            if (b == WATER) { if (!shadow && h.waterT < 0) h.waterT = t; }
            else if (b != AIR && SOLID[b] && !(shadow && (b == HANGING_MOSS || EMIT[b] > 1.5f)) && !(shadow && leafy(b) && rnd(x, y, z) < 0.35f)) {
                h.b = b; h.x = x; h.y = y; h.z = z; h.t = t; h.face = face; return h; }
            if (tmx < tmy && tmx < tmz) { x += sx; t = tmx; tmx += tdx; face = sx > 0 ? 4 : 5; } // face: 0 +z,1 -z,2 +y(top),3 -y,4 -x side hit from -x (normal -x),5 +x
            else if (tmy < tmz) { y += sy; t = tmy; tmy += tdy; face = sy > 0 ? 3 : 2; }
            else { z += sz; t = tmz; tmz += tdz; face = sz > 0 ? 1 : 0; }
        }
        h.t = t; return h;
    }
    static final int[][] NRM = {{0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}, {-1, 0, 0}, {1, 0, 0}};

    /** Minecraft smooth lighting: AO per corner of the face, interpolated */
    static float ao(Hit h, float fu, float fv) {
        int[] n = NRM[h.face]; int ax, ay, az, bx, by, bz; // tangent axes
        if (n[1] != 0) { ax = 1; ay = 0; az = 0; bx = 0; by = 0; bz = 1; } else if (n[0] != 0) { ax = 0; ay = 0; az = 1; bx = 0; by = 1; bz = 0; } else { ax = 1; ay = 0; az = 0; bx = 0; by = 1; bz = 0; }
        int px = h.x + n[0], py = h.y + n[1], pz = h.z + n[2];
        float[] c = new float[4]; int k = 0;
        for (int sb = -1; sb <= 1; sb += 2) for (int sa = -1; sa <= 1; sa += 2) {
            int s1 = solid(px + ax * sa, py + ay * sa, pz + az * sa) ? 1 : 0, s2 = solid(px + bx * sb, py + by * sb, pz + bz * sb) ? 1 : 0, co = solid(px + ax * sa + bx * sb, py + ay * sa + by * sb, pz + az * sa + bz * sb) ? 1 : 0;
            int lvl = s1 == 1 && s2 == 1 ? 0 : 3 - (s1 + s2 + co); c[k++] = new float[]{0.42f, 0.62f, 0.8f, 1f}[lvl]; }
        if (n[1] == 0) fv = 1 - fv; // side faces: texture v grows downwards, the b axis (y) upwards
        float top = c[0] + (c[1] - c[0]) * fu, bot = c[2] + (c[3] - c[2]) * fu; return top + (bot - top) * fv;
    }

    static float[] shade(float[] o, float[] d, int depth, float[] outDepth) {
        Hit h = trace(o[0], o[1], o[2], d[0], d[1], d[2], 600, false);
        float[] col;
        if (h.b == AIR && seaLevel > 0 && d[1] < 0 && h.waterT < 0) { float ts = (seaLevel + 1 - o[1]) / d[1]; if (ts > 0) h.waterT = ts; } // endless ocean to the horizon
        if (h.b == AIR) { col = sky(d); outDepth[0] = 1e4f; }
        else {
            float px = o[0] + d[0] * h.t, py = o[1] + d[1] * h.t, pz = o[2] + d[2] * h.t;
            int[] n = NRM[Math.max(0, h.face)];
            float fu, fv; // position on the face (0..1)
            if (n[1] != 0) { fu = px - h.x; fv = pz - h.z; } else if (n[0] != 0) { fu = pz - h.z; fv = 1 - (py - h.y); } else { fu = px - h.x; fv = 1 - (py - h.y); }
            fu = clamp(fu); fv = clamp(fv);
            int u = Math.min(15, (int) (fu * 16)), v = Math.min(15, (int) (fv * 16));
            float[] a = albedo(h.b, Math.max(0, h.face), u, v, h.x, h.y, h.z);
            float occ = ao(h, fu, fv);
            int lx = h.x + n[0], ly = h.y + n[1], lz = h.z + n[2];
            float sl = light(SKY, lx, ly, lz) / 15f, bl = light(BLK, lx, ly, lz) / 15f;
            if (nether) sl = Math.max(sl, 0.45f); // the Nether and caves have no sky: keep a dim ambient
            float ndl = Math.max(0, n[0] * sun[0] + n[1] * sun[1] + n[2] * sun[2]), sh = 0;
            if (ndl > 0 && shadows && !nether) { Hit s = trace(px + n[0] * 0.002f, py + n[1] * 0.002f, pz + n[2] * 0.002f, sun[0], sun[1], sun[2], 220, true); sh = s.b == AIR ? 1 : 0; }
            float faceShade = n[1] > 0 ? 1f : n[1] < 0 ? 0.55f : n[0] != 0 ? 0.72f : 0.86f; // Minecraft's per-face brightness
            col = new float[3];
            for (int i = 0; i < 3; i++) {
                float l = sunCol[i] * ndl * sh * sl * 0.95f + ambient[i] * (float) Math.pow(sl, 1.6) * occ * faceShade + blockCol[i] * (float) Math.pow(bl, 2.4) * 1.5f * occ;
                col[i] = a[i] * l + a[i] * EMIT[h.b] * (h.b == LAVA ? 1.2f : 1);
            }
            float dist = h.t; outDepth[0] = dist;
            float f = 1 - (float) Math.exp(-Math.max(0, dist - fogStart) * fogDensity);
            if (heightFog > 0) f = Math.max(f, Math.min(0.92f, (1 - (float) Math.exp(-dist * heightFog)) * smooth(heightFogY + 14, heightFogY - 4, py)));
            for (int i = 0; i < 3; i++) col[i] = col[i] * (1 - f) + fogCol[i] * f;
        }
        if (h.waterT >= 0) { // water surface between the camera and the hit
            float wt = h.waterT, depthW = h.b == AIR ? 40 : Math.max(0, h.t - wt);
            float sx = o[0] + d[0] * wt, sz = o[2] + d[2] * wt;
            float wx = (vnoise(sx * 0.6f, sz * 0.6f, 41) - 0.5f) * 0.08f, wz = (vnoise(sx * 0.6f + 9, sz * 0.6f, 42) - 0.5f) * 0.08f;
            float[] nn = n3(wx, 1, wz); float cos = Math.abs(d[0] * nn[0] + d[1] * nn[1] + d[2] * nn[2]);
            float F = Math.min(0.75f, 0.02f + 0.98f * (float) Math.pow(1 - cos, 5));
            float[] r = {d[0] - 2 * nn[0] * (d[0] * nn[0] + d[1] * nn[1] + d[2] * nn[2]), 0, d[2] - 2 * nn[2] * (d[0] * nn[0] + d[1] * nn[1] + d[2] * nn[2])}; r[1] = Math.abs(d[1] - 2 * nn[1] * (d[0] * nn[0] + d[1] * nn[1] + d[2] * nn[2]));
            float[] rc = sky(r, true); for (int i = 0; i < 3; i++) rc[i] = Math.min(rc[i], 2.2f) * 0.8f; float absorb = (float) Math.exp(-depthW * waterAbs);
            float spec = (float) Math.pow(Math.max(0, r[0] * sun[0] + r[1] * sun[1] + r[2] * sun[2]), 400) * 2.5f * (1 - Math.min(1, Math.abs(wx) * 6));
            for (int i = 0; i < 3; i++) { float under = col[i] * absorb + waterCol[i] * (1 - absorb); col[i] = under * (1 - F) + rc[i] * F + spec * sunCol[i]; }
            float f = 1 - (float) Math.exp(-wt * fogDensity); for (int i = 0; i < 3; i++) col[i] = col[i] * (1 - f) + fogCol[i] * f;
            outDepth[0] = Math.min(outDepth[0], wt);
        }
        return col;
    }
    static float clamp(float v) { return Math.max(0, Math.min(0.9999f, v)); }

    // ------------------------------------------------------------------ render + post
    static BufferedImage render(int W0, int H0, int ss) {
        int w = W0 * ss, hgt = H0 * ss;
        float[] img = new float[w * hgt * 3], dep = new float[w * hgt];
        float cy = (float) Math.cos(Math.toRadians(yaw)), sy = (float) Math.sin(Math.toRadians(yaw)), cp = (float) Math.cos(Math.toRadians(pitch)), sp = (float) Math.sin(Math.toRadians(pitch));
        float[] fwd = {sy * cp, sp, cy * cp}, right = n3(-cy, 0, sy), up = {right[1] * fwd[2] - right[2] * fwd[1], right[2] * fwd[0] - right[0] * fwd[2], right[0] * fwd[1] - right[1] * fwd[0]};
        float tanF = (float) Math.tan(Math.toRadians(fov / 2)), aspect = w / (float) hgt;
        IntStream.range(0, hgt).parallel().forEach(j -> { float[] od = new float[1];
            for (int i = 0; i < w; i++) {
                float px = (2 * (i + 0.5f) / w - 1) * tanF * aspect, py = (1 - 2 * (j + 0.5f) / hgt) * tanF;
                float[] d = n3(fwd[0] + right[0] * px + up[0] * py, fwd[1] + right[1] * px + up[1] * py, fwd[2] + right[2] * px + up[2] * py);
                float[] c = shade(cam, d, 0, od); for (int q = 0; q < 3; q++) if (!(c[q] >= 0) || c[q] > 1e4f) c[q] = c[q] > 0 ? 1e4f : 0; int k = (j * w + i) * 3; img[k] = c[0]; img[k + 1] = c[1]; img[k + 2] = c[2]; dep[j * w + i] = od[0]; } });
        // point glows (torches, fire, crystals) and particles, projected to the screen
        for (float[] g : GLOWS) splat(img, dep, w, hgt, g, fwd, right, up, tanF, aspect, ss, true);
        for (float[] p : PARTICLES) splat(img, dep, w, hgt, p, fwd, right, up, tanF, aspect, ss, false);
        // depth of field (far blur)
        if (dof > 0) { float[] bl = blur(img, w, hgt, Math.max(1, (int) (dof * ss)), 3);
            for (int p = 0; p < w * hgt; p++) { float k = smooth(focus, focus * 2.6f, dep[p]) * 0.85f; for (int c = 0; c < 3; c++) img[p * 3 + c] = img[p * 3 + c] * (1 - k) + bl[p * 3 + c] * k; } }
        // bloom
        float[] br = new float[img.length]; for (int p = 0; p < img.length; p++) br[p] = Math.max(0, img[p] * exposure - 0.9f);
        float[] b1 = blur(br, w, hgt, Math.max(2, w / 90), 3), b2 = blur(br, w, hgt, Math.max(4, w / 30), 3);
        BufferedImage out = new BufferedImage(W0, H0, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < H0; j++) for (int i = 0; i < W0; i++) {
            float[] c = new float[3];
            for (int a = 0; a < ss; a++) for (int b = 0; b < ss; b++) { int p = ((j * ss + a) * w + i * ss + b) * 3; for (int k = 0; k < 3; k++) c[k] += (img[p + k] * exposure + (b1[p + k] * 0.6f + b2[p + k] * 0.8f) * bloom) / (ss * ss); }
            float vx = (i + 0.5f) / W0 - 0.5f, vy = (j + 0.5f) / H0 - 0.5f, vig = 1 - vignette * (vx * vx + vy * vy) * 2.2f;
            float lum = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
            int rgb = 0;
            for (int k = 0; k < 3; k++) { float x = (lum + (c[k] - lum) * saturation) * vig * tint[k]; x = aces(x); x = (float) Math.pow(Math.max(0, x), 1 / 2.2); x = 0.5f + (x - 0.5f) * contrast; rgb = rgb << 8 | Math.max(0, Math.min(255, Math.round(x * 255))); }
            out.setRGB(i, j, rgb);
        }
        return out;
    }
    static float aces(float x) { return Math.max(0, Math.min(1, (x * (2.51f * x + 0.03f)) / (x * (2.43f * x + 0.59f) + 0.14f))); }
    static void splat(float[] img, float[] dep, int w, int h, float[] g, float[] fwd, float[] right, float[] up, float tanF, float aspect, int ss, boolean glow) {
        float dx = g[0] - cam[0], dy = g[1] - cam[1], dz = g[2] - cam[2];
        float z = dx * fwd[0] + dy * fwd[1] + dz * fwd[2]; if (z < 0.5f) return;
        float sx = (dx * right[0] + dy * right[1] + dz * right[2]) / z / (tanF * aspect), sy = (dx * up[0] + dy * up[1] + dz * up[2]) / z / tanF;
        int cx = (int) ((sx + 1) / 2 * w), cy = (int) ((1 - sy) / 2 * h);
        float rad = g[6] * ss * (glow ? 70 : 60) / z; if (glow) rad = Math.max(rad, 3 * ss);
        int R = (int) (rad * (glow ? 3 : 1)) + 1;
        for (int y = cy - R; y <= cy + R; y++) for (int x = cx - R; x <= cx + R; x++) { if (x < 0 || y < 0 || x >= w || y >= h) continue;
            if (dep[y * w + x] < z - 1.5f) continue;
            float d = (float) Math.hypot(x - cx, y - cy) / Math.max(0.5f, rad);
            float a = glow ? (float) Math.exp(-d * d * 0.7f) * 1.2f + (float) Math.exp(-d * 1.5f) * 0.18f : (d < 1 ? 0.9f : 0);
            if (a <= 0.002f) continue; int p = (y * w + x) * 3;
            if (glow) for (int k = 0; k < 3; k++) img[p + k] += g[3 + k] * a; else for (int k = 0; k < 3; k++) img[p + k] = img[p + k] * (1 - a) + g[3 + k] * a; }
    }
    static float[] blur(float[] src, int w, int h, int r, int passes) {
        float[] a = src.clone(), b = new float[src.length];
        for (int p = 0; p < passes; p++) {
            for (int y = 0; y < h; y++) for (int c = 0; c < 3; c++) { float s = 0; int n = 0; for (int x = -r; x <= r; x++) { int xx = Math.max(0, Math.min(w - 1, x)); s += a[(y * w + xx) * 3 + c]; n++; }
                for (int x = 0; x < w; x++) { b[(y * w + x) * 3 + c] = s / n; int xo = Math.max(0, Math.min(w - 1, x - r)), xi = Math.max(0, Math.min(w - 1, x + r + 1)); s += a[(y * w + xi) * 3 + c] - a[(y * w + xo) * 3 + c]; } }
            for (int x = 0; x < w; x++) for (int c = 0; c < 3; c++) { float s = 0; int n = 0; for (int y = -r; y <= r; y++) { int yy = Math.max(0, Math.min(h - 1, y)); s += b[(yy * w + x) * 3 + c]; n++; }
                for (int y = 0; y < h; y++) { a[(y * w + x) * 3 + c] = s / n; int yo = Math.max(0, Math.min(h - 1, y - r)), yi = Math.max(0, Math.min(h - 1, y + r + 1)); s += b[(yi * w + x) * 3 + c] - b[(yo * w + x) * 3 + c]; } }
        }
        return a;
    }

    static void look(float x, float y, float z, float tx, float ty, float tz, float f) {
        cam = new float[]{x, y, z}; float dx = tx - x, dy = ty - y, dz = tz - z;
        yaw = (float) Math.toDegrees(Math.atan2(dx, dz)); pitch = (float) Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))); fov = f; focus = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static void main(String[] a) throws Exception {
        String scene = a[0]; int w = Integer.parseInt(a[2]), h = Integer.parseInt(a[3]), ss = a.length > 4 ? Integer.parseInt(a[4]) : 2;
        long t0 = System.currentTimeMillis();
        Scenes.build(scene);
        computeLight(!nether);
        ImageIO.write(render(w, h, ss), "png", new File(a[1]));
        System.out.println(scene + " " + w + "x" + h + " in " + (System.currentTimeMillis() - t0) / 1000.0 + "s");
    }
}
