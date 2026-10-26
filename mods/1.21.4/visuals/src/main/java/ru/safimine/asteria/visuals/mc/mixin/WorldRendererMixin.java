package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
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
    private void asteria$world(ObjectAllocator alloc, RenderTickCounter tc, boolean outline, Camera cam, GameRenderer gr, Matrix4f pos, Matrix4f proj, CallbackInfo ci) {
        VisualsClient.world(pos, cam, tc.getTickDelta(false));
    }
}
