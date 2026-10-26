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
import ru.safimine.asteria.visuals.Gfx;

/** 2D drawing for 1.16.5: MatrixStack + Tessellator (POSITION_COLOR triangle fans); items follow the RenderSystem matrix. */
final class Gfx116 implements Gfx {
    private final MatrixStack m;
    private final MinecraftClient c = MinecraftClient.getInstance();
    private final TextRenderer tr = c.textRenderer;
    Gfx116(MatrixStack m) { this.m = m; }

    @Override public void poly(float[] xy, int[] col) {
        Matrix4f mat = m.peek().getModel();
        RenderSystem.disableTexture(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.shadeModel(7425); RenderSystem.disableCull();
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(6, VertexFormats.POSITION_COLOR); // GL_TRIANGLE_FAN
        for (int i = 0; i < col.length; i++) { int c = col[i]; b.vertex(mat, xy[i * 2], xy[i * 2 + 1], 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next(); }
        Tessellator.getInstance().draw();
        RenderSystem.shadeModel(7424); RenderSystem.enableTexture(); RenderSystem.enableCull();
    }
    @Override public void text(String s, float x, float y, int argb, boolean shadow) {
        if (((argb >>> 24) & 255) < 5) return;
        RenderSystem.enableBlend();
        if (shadow) tr.drawWithShadow(m, s, x, y, argb); else tr.draw(m, s, x, y, argb);
    }
    @Override public int width(String s) { return tr.getWidth(s); }
    @Override public void item(Object stack, int x, int y) { c.getItemRenderer().renderInGuiWithOverrides((ItemStack) stack, x, y); }
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
    @Override public void push(float x, float y, float s) {
        m.push(); m.translate(x, y, 0); m.scale(s, s, 1f);
        RenderSystem.pushMatrix(); RenderSystem.translatef(x, y, 0); RenderSystem.scalef(s, s, 1f);
    }
    @Override public void pop() { m.pop(); RenderSystem.popMatrix(); }
    @Override public void scissor(float x, float y, float w, float h) {
        Window win = c.getWindow(); double k = win.getScaleFactor();
        RenderSystem.enableScissor((int) (x * k), (int) (win.getFramebufferHeight() - (y + h) * k), Math.max(0, (int) (w * k)), Math.max(0, (int) (h * k)));
    }
    @Override public void noScissor() { RenderSystem.disableScissor(); }
}
