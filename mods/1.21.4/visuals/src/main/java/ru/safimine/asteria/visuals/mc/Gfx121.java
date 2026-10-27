package ru.safimine.asteria.visuals.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ru.safimine.asteria.visuals.Gfx;
import ru.safimine.asteria.visuals.V;

/** 2D drawing for 1.21.2–1.21.4: DrawContext for text/items/textures, Tessellator (POSITION_COLOR fans) for shapes. */
public final class Gfx121 implements Gfx {
    private final DrawContext ctx;
    private final MatrixStack m;
    private final MinecraftClient c = MinecraftClient.getInstance();
    private final TextRenderer tr = c.textRenderer;
    public Gfx121(DrawContext ctx) { this.ctx = ctx; this.m = ctx.getMatrices(); }
    private static final java.util.Map<String, Identifier> TEX = new java.util.HashMap<>();
    private Identifier tex(String path) { return TEX.computeIfAbsent(path, p -> { Identifier id = Identifier.of("asteria_visuals", p); try { c.getTextureManager().getTexture(id).setFilter(true, false); } catch (Throwable ignored) {} return id; }); }
    @Override public void texture(String path, int tw, int th, float x, float y, float w, float h) {
        Identifier id = tex(path);
        m.push(); m.translate(x, y, 0); m.scale(w / tw, h / th, 1); ctx.drawTexture(RenderLayer::getGuiTextured, id, 0, 0, 0f, 0f, tw, th, tw, th); m.pop();
    }

    @Override public void poly(float[] xy, int[] col) {
        ctx.draw();
        Matrix4f mat = m.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.field_53876); // POSITION_COLOR
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < col.length; i++) { int c = col[i]; b.vertex(mat, xy[i * 2], xy[i * 2 + 1], 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255); }
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.enableCull();
    }
    @Override public void strip(float[] xy, int[] col) {
        ctx.draw();
        Matrix4f mat = m.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.field_53876); // POSITION_COLOR
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < col.length; i++) { int c = col[i]; b.vertex(mat, xy[i * 2], xy[i * 2 + 1], 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255); }
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.enableCull();
    }
    @Override public void text(String s, float x, float y, int argb, boolean shadow) {
        if (((argb >>> 24) & 255) < 5) return;
        m.push(); m.translate(x, y, 0);
        ctx.drawText(tr, t(s), 0, 0, argb, shadow);
        m.pop();
    }
    private static Identifier imgId; private static String imgKey;
    /** loads (or reuses) the png as a texture under a fixed id */
    private Identifier tex(String key, java.io.File png) {
        if (key.equals(imgKey) && imgId != null) return imgId;
        try (java.io.InputStream in = new java.io.FileInputStream(png)) {
            net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(in);
            Identifier id = Identifier.of("asteria_visuals", "dyn/cover");
            if (imgId != null) c.getTextureManager().destroyTexture(imgId);
            c.getTextureManager().registerTexture(id, new net.minecraft.client.texture.NativeImageBackedTexture(img));
            imgId = id; imgKey = key; return id;
        } catch (Exception e) { imgKey = key; imgId = null; return null; }
    }
    @Override public boolean image(String key, java.io.File png, float x, float y, float w, float h) {
        Identifier id = tex(key, png); if (id == null) return false;
        m.push(); m.translate(x, y, 0); m.scale(w / 64f, h / 64f, 1); ctx.drawTexture(RenderLayer::getGuiTextured, id, 0, 0, 0f, 0f, 64, 64, 64, 64); m.pop();
        return true;
    }
    @Override public void textTop(String s, float x, float y, int argb) { m.push(); m.translate(0, 0, 300); text(s, x, y, argb, true); m.pop(); }
    @Override public int width(String s) { return tr.getWidth(t(s)); }
    /** our smooth TTF font (assets/asteria_visuals/font/ui.json) unless the theme switches it off */
    private static final Identifier[][] UI = new Identifier[16][7];
    /** GUI scale 1..6: the font variant rendered for exactly this many pixels per GUI pixel */
    private static int gs() { return Math.max(1, Math.min(6, (int) Math.round(net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor()))); }
    private static Text t(String s) { if (V.font <= 0) return Text.literal(s); int i = Math.min(15, V.font), k = gs(); if (UI[i][k] == null) UI[i][k] = Identifier.of("asteria_visuals", "f" + i + "s" + k); return Text.literal(s).setStyle(Style.EMPTY.withFont(UI[i][k])); }
    @Override public void item(Object stack, int x, int y) { ctx.drawItem((ItemStack) stack, x, y); }
    @SuppressWarnings("unchecked")
    @Override public boolean effectIcon(Object type, int x, int y, int size) {
        Sprite sp = c.getStatusEffectSpriteManager().getSprite((RegistryEntry<StatusEffect>) type);
        ctx.drawSpriteStretched(RenderLayer::getGuiTextured, sp, x, y, size, size);
        return true;
    }
    @Override public void head(Object skin, float x, float y, float size) {
        m.push(); m.translate(x, y, 0); m.scale(size / 8f, size / 8f, 1);
        ctx.drawTexture(RenderLayer::getGuiTextured, (Identifier) skin, 0, 0, 8f, 8f, 8, 8, 64, 64);
        ctx.drawTexture(RenderLayer::getGuiTextured, (Identifier) skin, 0, 0, 40f, 8f, 8, 8, 64, 64);
        m.pop();
    }
    @Override public void skinPart(Object skin, int u, int v, int w, int h, float x, float y, float k) {
        m.push(); m.translate(x, y, 0); m.scale(k, k, 1); ctx.drawTexture(RenderLayer::getGuiTextured, (Identifier) skin, 0, 0, (float) u, (float) v, w, h, 512, 512); m.pop();
    }
    @Override public boolean entity(Object e, float x, float y, float size) {
        if (!(e instanceof net.minecraft.entity.LivingEntity)) return false; net.minecraft.entity.LivingEntity le = (net.minecraft.entity.LivingEntity) e;
        float sc = le.getScale(), s = size * 0.85f / Math.max(le.getHeight(), le.getWidth());
        float by = le.bodyYaw, yw = le.getYaw(), pi = le.getPitch(), hy = le.headYaw, phy = le.prevHeadYaw;
        le.bodyYaw = 180; le.setYaw(180); le.setPitch(0); le.headYaw = 180; le.prevHeadYaw = 180; // faces the viewer
        try { net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, x + size / 2, y + size / 2, s, new org.joml.Vector3f(0, le.getHeight() / 2f + 0.0625f * sc, 0), new org.joml.Quaternionf().rotateZ((float) Math.PI), null, le); }
        finally { le.bodyYaw = by; le.setYaw(yw); le.setPitch(pi); le.headYaw = hy; le.prevHeadYaw = phy; }
        return true;
    }
    @Override public void push(float x, float y, float s) { m.push(); m.translate(x, y, 0); m.scale(s, s, 1f); }
    @Override public void pop() { m.pop(); }
    @Override public void scissor(float x, float y, float w, float h) {
        ctx.draw();
        Window win = c.getWindow(); double k = win.getScaleFactor();
        RenderSystem.enableScissor((int) (x * k), (int) (win.getFramebufferHeight() - (y + h) * k), Math.max(0, (int) (w * k)), Math.max(0, (int) (h * k)));
    }
    @Override public void noScissor() { ctx.draw(); RenderSystem.disableScissor(); }
}
