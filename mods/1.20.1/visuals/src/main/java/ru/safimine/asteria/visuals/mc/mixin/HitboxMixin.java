package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** F3+B with our "Hitboxes" on: our coloured boxes are drawn instead of the vanilla white ones */
@Mixin(EntityRenderDispatcher.class)
public class HitboxMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true, require = 0)
    private static void asteria$hitbox(CallbackInfo ci) { if (V.on("hitboxes")) ci.cancel(); }
}
