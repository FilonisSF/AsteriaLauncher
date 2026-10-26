package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** world visuals are drawn after everything else in the world */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void asteria$world(MatrixStack ms, float pt, long nano, boolean outline, Camera cam, GameRenderer gr, LightmapTextureManager lm, Matrix4f proj, CallbackInfo ci) {
        VisualsClient.world(ms, cam, pt);
    }
}
