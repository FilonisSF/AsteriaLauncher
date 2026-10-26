package ru.safimine.asteria.visuals;

/** 2D drawing surface in GUI coordinates. Each Minecraft version implements it on top of its own rendering API. */
public interface Gfx {
    /** convex polygon (triangle fan) with one colour per vertex; xy = x0,y0,x1,y1,... */
    void poly(float[] xy, int[] argb);
    void text(String s, float x, float y, int argb, boolean shadow);
    int width(String s);
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
