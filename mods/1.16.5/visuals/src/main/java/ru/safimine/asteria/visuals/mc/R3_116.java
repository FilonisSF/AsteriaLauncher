package ru.safimine.asteria.visuals.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import ru.safimine.asteria.visuals.Fx;

/** 3D drawing for 1.16.5 in camera-relative coordinates (context matrix already contains the camera rotation). */
final class R3_116 implements Fx.R3 {
    private final WorldRenderContext ctx; private final Matrix4f mat; private final Vec3d cam; private final Camera camera;
    private final BufferBuilder b = Tessellator.getInstance().getBuffer();
    R3_116(WorldRenderContext ctx) { this.ctx = ctx; this.mat = ctx.matrixStack().peek().getModel(); this.camera = ctx.camera(); this.cam = camera.getPos(); }

    @Override public void begin(int mode, boolean depth) {
        RenderSystem.disableTexture(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        RenderSystem.shadeModel(7425); RenderSystem.disableAlphaTest(); RenderSystem.depthMask(false); RenderSystem.disableLighting();
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.5f);
        b.begin(mode == TRIS ? 4 : mode == QUADS ? 7 : mode == STRIP ? 5 : 1, VertexFormats.POSITION_COLOR);
    }
    @Override public void v(double x, double y, double z, int c) {
        b.vertex(mat, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next();
    }
    @Override public void beginTex(String texture, boolean depth) {
        RenderSystem.enableBlend(); RenderSystem.disableCull(); RenderSystem.shadeModel(7425); RenderSystem.disableAlphaTest(); RenderSystem.depthMask(false); RenderSystem.disableLighting();
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        MinecraftClient.getInstance().getTextureManager().bindTexture(new Identifier("asteria_visuals", "textures/fx/" + texture + ".png"));
        RenderSystem.enableTexture(); RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        b.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
    }
    @Override public void vt(double x, double y, double z, float u, float v, int c) {
        b.vertex(mat, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).texture(u, v).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next();
    }
    @Override public void end() {
        Tessellator.getInstance().draw(); RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.enableCull(); RenderSystem.enableTexture(); RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424); RenderSystem.lineWidth(1f);
    }
    @Override public double[] cam() { return new double[]{cam.x, cam.y, cam.z}; }
    @Override public float camYaw() { return camera.getYaw(); }
    @Override public float camPitch() { return camera.getPitch(); }
    @Override public float partial() { return ctx.tickDelta(); }
}
