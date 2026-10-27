package ru.safimine.asteria.visuals.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.safimine.asteria.visuals.Fx;

/** 3D drawing for 1.20.x in camera-relative coordinates (the world matrix stack already contains the camera rotation). */
final class R3_120 implements Fx.R3 {
    private final Matrix4f mat; private final Vec3d cam; private final Camera camera; private final float pt;
    private final BufferBuilder b = Tessellator.getInstance().getBuffer();
    R3_120(MatrixStack ms, Camera camera, float pt) { this.mat = ms.peek().getPositionMatrix(); this.camera = camera; this.cam = camera.getPos(); this.pt = pt; }

    @Override public void begin(int mode, boolean depth) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.depthMask(false);
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.5f);
        VertexFormat.DrawMode dm = mode == TRIS ? VertexFormat.DrawMode.TRIANGLES : mode == QUADS ? VertexFormat.DrawMode.QUADS : mode == STRIP ? VertexFormat.DrawMode.TRIANGLE_STRIP : VertexFormat.DrawMode.DEBUG_LINES;
        b.begin(dm, VertexFormats.POSITION_COLOR);
    }
    @Override public void v(double x, double y, double z, int c) {
        b.vertex(mat, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next();
    }
    @Override public void beginTex(String texture, boolean depth) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, new Identifier("asteria_visuals", "textures/fx/" + texture + ".png"));
        RenderSystem.enableBlend(); RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull(); RenderSystem.depthMask(false);
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }
    @Override public void vt(double x, double y, double z, float u, float v, int c) {
        b.vertex(mat, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).texture(u, v).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).next();
    }
    @Override public void end() {
        Tessellator.getInstance().draw(); RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.enableCull(); RenderSystem.lineWidth(1f);
    }
    @Override public void beginSkin(Object texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram); RenderSystem.setShaderTexture(0, (Identifier) texture);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.depthMask(true); RenderSystem.enableDepthTest();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }
    private static final java.util.Map<String, Object[]> FT = new java.util.HashMap<>();
    @Override public Object fileTex(String key, java.io.File png) {
        String slot = key.contains(":") ? key.substring(0, key.indexOf(':')) : key; Object[] e = FT.get(slot);
        if (e != null && key.equals(e[0])) return e[1];
        MinecraftClient mc = MinecraftClient.getInstance(); Identifier id = new Identifier("asteria_visuals", "dyn/w_" + slot.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
        try (java.io.InputStream in = new java.io.FileInputStream(png)) {
            net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(in);
            if (e != null && e[1] != null) mc.getTextureManager().destroyTexture((Identifier) e[1]);
            mc.getTextureManager().registerTexture(id, new net.minecraft.client.texture.NativeImageBackedTexture(img)); FT.put(slot, new Object[]{key, id}); return id;
        } catch (Exception ex) { FT.put(slot, new Object[]{key, null}); return null; }
    }
    @Override public void beginSolid() { begin(QUADS, true); RenderSystem.depthMask(true); }
    @Override public Object asset(String path) { return new Identifier("asteria_visuals", "textures/fx/" + path + ".png"); }
    @Override public double[] cam() { return new double[]{cam.x, cam.y, cam.z}; }
    @Override public float camYaw() { return camera.getYaw(); }
    @Override public float camPitch() { return camera.getPitch(); }
    @Override public float partial() { return pt; }
}
