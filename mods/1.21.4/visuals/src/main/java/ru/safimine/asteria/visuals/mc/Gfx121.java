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
import ru.safimine.asteria.visuals.Gfx;

/** 2D drawing for 1.21.2–1.21.4: DrawContext for text/items/textures, Tessellator (POSITION_COLOR fans) for shapes. */
final class Gfx121 implements Gfx {
    private final DrawContext ctx;
    private final MatrixStack m;
    private final MinecraftClient c = MinecraftClient.getInstance();
    private final TextRenderer tr = c.textRenderer;
    Gfx121(DrawContext ctx) { this.ctx = ctx; this.m = ctx.getMatrices(); }

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
    @Override public void text(String s, float x, float y, int argb, boolean shadow) {
        if (((argb >>> 24) & 255) < 5) return;
        m.push(); m.translate(x, y, 0);
        ctx.drawText(tr, s, 0, 0, argb, shadow);
        m.pop();
    }
    @Override public int width(String s) { return tr.getWidth(s); }
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
    @Override public void push(float x, float y, float s) { m.push(); m.translate(x, y, 0); m.scale(s, s, 1f); }
    @Override public void pop() { m.pop(); }
    @Override public void scissor(float x, float y, float w, float h) {
        ctx.draw();
        Window win = c.getWindow(); double k = win.getScaleFactor();
        RenderSystem.enableScissor((int) (x * k), (int) (win.getFramebufferHeight() - (y + h) * k), Math.max(0, (int) (w * k)), Math.max(0, (int) (h * k)));
    }
    @Override public void noScissor() { ctx.draw(); RenderSystem.disableScissor(); }
}
