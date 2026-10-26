package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.V;

/** custom fog colour and distance */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private static void asteria$color(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        if (!V.on("fog")) return;
        int c = V.color("fog", "color");
        cir.setReturnValue(new Vector4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, cir.getReturnValue().w));
    }
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void asteria$distance(Camera camera, BackgroundRenderer.FogType type, Vector4f color, float viewDistance, boolean thick, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        if (!V.on("fog") || type != BackgroundRenderer.FogType.FOG_TERRAIN || thick) return;
        Fog f = cir.getReturnValue(); // (start, end, shape, r, g, b, a) by runtime names
        cir.setReturnValue(new Fog(viewDistance * (float) V.num("fog", "start") * 0.9f, viewDistance, f.comp_3011(), f.comp_3012(), f.comp_3013(), f.comp_3014(), f.comp_3015()));
    }
}
