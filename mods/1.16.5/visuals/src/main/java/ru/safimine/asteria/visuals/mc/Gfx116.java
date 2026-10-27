package ru.safimine.asteria.visuals.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ru.safimine.asteria.visuals.Gfx;
import ru.safimine.asteria.visuals.V;

/**
 * 2D drawing for 1.16.5: MatrixStack + Tessellator (POSITION_COLOR triangle fans). Transforms live only in the MatrixStack:
 * the fixed-function GL matrix would apply them a second time to everything. Items are the one exception (they ignore the
 * MatrixStack), so the stack's matrix is copied onto the GL matrix just for them.
 */
public final class Gfx116 implements Gfx {
    private final MatrixStack m;
    private final MinecraftClient c = MinecraftClient.getInstance();
    private final TextRenderer tr = c.textRenderer;
    public Gfx116(MatrixStack m) { this.m = m; }
    private static final java.util.Map<String, Identifier> TEX = new java.util.HashMap<>();
    private Identifier tex(String path) { return TEX.computeIfAbsent(path, p -> { Identifier id = new Identifier("asteria_visuals", p); try { c.getTextureManager().getTexture(id).setFilter(true, false); } catch (Throwable ignored) {} return id; }); }
    @Override public void texture(String path, int tw, int th, float x, float y, float w, float h) {
        Identifier id = tex(path); c.getTextureManager().bindTexture(id); c.getTextureManager().getTexture(id).setFilter(true, false);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.color4f(1, 1, 1, 1);
        m.push(); m.translate(x, y, 0); m.scale(w / tw, h / th, 1); DrawableHelper.drawTexture(m, 0, 0, 0f, 0f, tw, th, tw, th); m.pop();
    }

    @Override public void poly(float[] xy, int[] col) {
        Matrix4f mat = m.peek().getModel();
        RenderSystem.disableTexture(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.shadeModel(7425); RenderSystem.disableCull();
        RenderSystem.disableAlphaTest(); // the GUI alpha test (> 0.1) would drop faint fills, hovers and soft shadows
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(6, VertexFormats.POSITION_COLOR); // GL_TRIANGLE_FAN
        for (int i = 0; i < col.length; i++) { int c = col[i]; b.vertex(mat, xy[i * 2], xy[i * 2 + 1], 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next(); }
        Tessellator.getInstance().draw();
        RenderSystem.shadeModel(7424); RenderSystem.enableTexture(); RenderSystem.enableCull(); RenderSystem.enableAlphaTest();
    }
    @Override public void strip(float[] xy, int[] col) {
        Matrix4f mat = m.peek().getModel();
        RenderSystem.disableTexture(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.shadeModel(7425); RenderSystem.disableCull(); RenderSystem.disableAlphaTest();
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(5, VertexFormats.POSITION_COLOR); // GL_TRIANGLE_STRIP
        for (int i = 0; i < col.length; i++) { int c = col[i]; b.vertex(mat, xy[i * 2], xy[i * 2 + 1], 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next(); }
        Tessellator.getInstance().draw();
        RenderSystem.shadeModel(7424); RenderSystem.enableTexture(); RenderSystem.enableCull(); RenderSystem.enableAlphaTest();
    }
    @Override public void text(String s, float x, float y, int argb, boolean shadow) {
        if (((argb >>> 24) & 255) < 5) return;
        RenderSystem.enableBlend();
        if (shadow) tr.drawWithShadow(m, t(s), x, y, argb); else tr.draw(m, t(s), x, y, argb);
    }
    private static Identifier imgId; private static String imgKey;
    /** loads (or reuses) the png as a texture under a fixed id */
    private Identifier tex(String key, java.io.File png) {
        if (key.equals(imgKey) && imgId != null) return imgId;
        try (java.io.InputStream in = new java.io.FileInputStream(png)) {
            net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(in);
            Identifier id = new Identifier("asteria_visuals", "dyn/cover");
            if (imgId != null) c.getTextureManager().destroyTexture(imgId);
            c.getTextureManager().registerTexture(id, new net.minecraft.client.texture.NativeImageBackedTexture(img));
            imgId = id; imgKey = key; return id;
        } catch (Exception e) { imgKey = key; imgId = null; return null; }
    }
    @Override public boolean image(String key, java.io.File png, float x, float y, float w, float h) {
        Identifier id = tex(key, png); if (id == null) return false;
        c.getTextureManager().bindTexture(id); RenderSystem.enableBlend(); RenderSystem.color4f(1, 1, 1, 1);
        m.push(); m.translate(x, y, 0); m.scale(w / 64f, h / 64f, 1); DrawableHelper.drawTexture(m, 0, 0, 0f, 0f, 64, 64, 64, 64); m.pop();
        return true;
    }
    @Override public void textTop(String s, float x, float y, int argb) { m.push(); m.translate(0, 0, 300); text(s, x, y, argb, true); m.pop(); }
    @Override public int width(String s) { return tr.getWidth(t(s)); }
    /** our smooth TTF font (assets/asteria_visuals/font/ui.json) unless the theme switches it off */
    private static final Identifier[][] UI = new Identifier[16][7];
    /** GUI scale 1..6: the font variant rendered for exactly this many pixels per GUI pixel */
    private static int gs() { return Math.max(1, Math.min(6, (int) Math.round(net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor()))); }
    private static Text t(String s) { if (V.font <= 0) return new LiteralText(s); int i = Math.min(15, V.font), k = gs(); if (UI[i][k] == null) UI[i][k] = new Identifier("asteria_visuals", "f" + i + "s" + k); return new LiteralText(s).setStyle(Style.EMPTY.withFont(UI[i][k])); }
    @Override public void item(Object stack, int x, int y) {
        RenderSystem.pushMatrix(); RenderSystem.multMatrix(m.peek().getModel());
        c.getItemRenderer().renderInGuiWithOverrides((ItemStack) stack, x, y);
        RenderSystem.popMatrix();
    }
    @Override public boolean effectIcon(Object type, int x, int y, int size) {
        Sprite sp = c.getStatusEffectSpriteManager().getSprite((StatusEffect) type);
        c.getTextureManager().bindTexture(sp.getAtlas().getId());
        RenderSystem.enableBlend(); RenderSystem.color4f(1, 1, 1, 1);
        DrawableHelper.drawSprite(m, x, y, 0, size, size, sp);
        return true;
    }
    @Override public void head(Object skin, float x, float y, float size) {
        c.getTextureManager().bindTexture((Identifier) skin);
        RenderSystem.enableBlend(); RenderSystem.color4f(1, 1, 1, 1);
        m.push(); m.translate(x, y, 0); m.scale(size / 8f, size / 8f, 1);
        DrawableHelper.drawTexture(m, 0, 0, 8, 8, 8, 8, 8, 8, 64, 64);   // face
        DrawableHelper.drawTexture(m, 0, 0, 8, 8, 40, 8, 8, 8, 64, 64);  // hat layer
        m.pop();
    }
    @Override public void skinPart(Object skin, int u, int v, int w, int h, float x, float y, float k) {
        c.getTextureManager().bindTexture((Identifier) skin); RenderSystem.enableBlend(); RenderSystem.color4f(1, 1, 1, 1);
        m.push(); m.translate(x, y, 0); m.scale(k, k, 1); DrawableHelper.drawTexture(m, 0, 0, w, h, (float) u, (float) v, w, h, 512, 512); m.pop();
    }
    @Override public boolean entity(Object e, float x, float y, float size) {
        if (!(e instanceof net.minecraft.entity.LivingEntity)) return false; net.minecraft.entity.LivingEntity le = (net.minecraft.entity.LivingEntity) e;
        int s = Math.max(2, (int) (size * 0.85f / Math.max(le.getHeight(), le.getWidth())));
        RenderSystem.pushMatrix(); RenderSystem.multMatrix(m.peek().getModel());
        try { net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity((int) (x + size / 2), (int) (y + size - 2), s, 0, 0, le); } finally { RenderSystem.popMatrix(); }
        return true;
    }
    @Override public void push(float x, float y, float s) {
        m.push(); m.translate(x, y, 0); m.scale(s, s, 1f);
    }
    @Override public void pop() { m.pop(); }
    @Override public void scissor(float x, float y, float w, float h) {
        Window win = c.getWindow(); double k = win.getScaleFactor();
        RenderSystem.enableScissor((int) (x * k), (int) (win.getFramebufferHeight() - (y + h) * k), Math.max(0, (int) (w * k)), Math.max(0, (int) (h * k)));
    }
    @Override public void noScissor() { RenderSystem.disableScissor(); }
}
