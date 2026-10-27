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
    void push(float x, float y, float scale);
    void pop();
    /** clip to a rectangle (GUI coordinates, not affected by push) */
    void scissor(float x, float y, float w, float h);
    void noScissor();
}
