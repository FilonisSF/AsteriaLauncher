package ru.safimine.asteria.visuals.mc.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** custom fog colour and distance */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void asteria$color(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (!V.on("fog")) return;
        int c = V.color("fog", "color");
        red = ((c >> 16) & 255) / 255f; green = ((c >> 8) & 255) / 255f; blue = (c & 255) / 255f;
        RenderSystem.clearColor(red, green, blue, 0f);
    }
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void asteria$distance(Camera camera, BackgroundRenderer.FogType type, float viewDistance, boolean thick, float tickDelta, CallbackInfo ci) {
        if (!V.on("fog") || type != BackgroundRenderer.FogType.FOG_TERRAIN || thick) return;
        RenderSystem.setShaderFogStart(viewDistance * (float) V.num("fog", "start") * 0.9f);
        RenderSystem.setShaderFogEnd(viewDistance);
    }
}
