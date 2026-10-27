package ru.safimine.asteria.visuals;

/** 2D drawing surface in GUI coordinates. Each Minecraft version implements it on top of its own rendering API. */
public interface Gfx {
    /** convex polygon (triangle fan) with one colour per vertex; xy = x0,y0,x1,y1,... */
    void poly(float[] xy, int[] argb);
    /** triangle strip (v0 v1 v2, v1 v2 v3, ...) in one draw; the fallback draws it as quads */
    default void strip(float[] xy, int[] argb) {
        for (int i = 0; i + 3 < argb.length; i += 2)
            poly(new float[]{xy[i * 2], xy[i * 2 + 1], xy[i * 2 + 2], xy[i * 2 + 3], xy[i * 2 + 6], xy[i * 2 + 7], xy[i * 2 + 4], xy[i * 2 + 5]}, new int[]{argb[i], argb[i + 1], argb[i + 3], argb[i + 2]});
    }
    void text(String s, float x, float y, int argb, boolean shadow);
    int width(String s);
    /** text drawn above items (item counts) */
    void textTop(String s, float x, float y, int argb);
    /** a png from disk as a texture (re-uploaded when key changes); false if it could not be drawn */
    default boolean image(String key, java.io.File png, float x, float y, float w, float h) { return false; }
    /** a texture of our mod (asteria_visuals:<path>, tw x th pixels) stretched to x, y, w, h, smooth filtering */
    default void texture(String path, int tw, int th, float x, float y, float w, float h) {}
    void item(Object stack, int x, int y);
    /** status-effect icon; false if this version can't draw it */
    boolean effectIcon(Object type, int x, int y, int size);
    /** face of a player skin (skin texture id), size in GUI pixels */
    void head(Object skin, float x, float y, float size);
    /** a piece of a 64x64 skin in 1/8 texel units (texture seen as 512x512) at x, y, k GUI px per unit */
    default void skinPart(Object skin, int u, int v, int w, int h, float x, float y, float k) {}
    /** player face (with the hat layer) with rounded corners, r in GUI px */
    default void headRound(Object skin, float x, float y, float size, float r) {
        final int n = 8, S = 8 * n; float k = size / S; int R = Math.max(0, Math.min(S / 2, Math.round(r / k)));
        for (int layer = 0; layer < 2; layer++) { int u0 = (layer == 0 ? 8 : 40) * n, v0 = 8 * n;
            for (int row = 0; row < R; row++) { double dy = R - row - 0.5; int in = (int) Math.round(R - Math.sqrt(Math.max(0, R * (double) R - dy * dy)));
                skinPart(skin, u0 + in, v0 + row, S - 2 * in, 1, x + in * k, y + row * k, k);
                skinPart(skin, u0 + in, v0 + S - 1 - row, S - 2 * in, 1, x + in * k, y + (S - 1 - row) * k, k); }
            skinPart(skin, u0, v0 + R, S, S - 2 * R, x, y + R * k, k); }
    }
    /** a living entity's model fitted into a size x size box; false if this version can't */
    default boolean entity(Object e, float x, float y, float size) { return false; }
    void push(float x, float y, float scale);
    void pop();
    /** clip to a rectangle (GUI coordinates, not affected by push) */
    void scissor(float x, float y, float w, float h);
    void noScissor();
}
