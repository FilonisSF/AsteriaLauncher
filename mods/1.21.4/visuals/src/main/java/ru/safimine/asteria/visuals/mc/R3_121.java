package ru.safimine.asteria.visuals.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.safimine.asteria.visuals.Fx;

/** 3D drawing for 1.21.2–1.21.4 in camera-relative coordinates (the position matrix contains the camera rotation). */
final class R3_121 implements Fx.R3 {
    private final Matrix4f mat; private final Vec3d cam; private final Camera camera; private final float pt;
    private BufferBuilder b;
    R3_121(Matrix4f pos, Camera camera, float pt) { this.mat = pos; this.camera = camera; this.cam = camera.getPos(); this.pt = pt; }

    @Override public void begin(int mode, boolean depth) {
        RenderSystem.setShader(ShaderProgramKeys.field_53876); // POSITION_COLOR
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.depthMask(false);
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        VertexFormat.DrawMode dm = mode == TRIS ? VertexFormat.DrawMode.TRIANGLES : mode == QUADS ? VertexFormat.DrawMode.QUADS : mode == STRIP ? VertexFormat.DrawMode.TRIANGLE_STRIP : VertexFormat.DrawMode.DEBUG_LINES;
        b = Tessellator.getInstance().begin(dm, VertexFormats.POSITION_COLOR);
    }
    @Override public void v(double x, double y, double z, int c) {
        b.vertex(mat, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255);
    }
    @Override public void end() {
        try { BufferRenderer.drawWithGlobalProgram(b.end()); } catch (IllegalStateException empty) { /* nothing was added */ }
        RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
    }
    @Override public double[] cam() { return new double[]{cam.x, cam.y, cam.z}; }
    @Override public float camYaw() { return camera.getYaw(); }
    @Override public float camPitch() { return camera.getPitch(); }
    @Override public float partial() { return pt; }
}
