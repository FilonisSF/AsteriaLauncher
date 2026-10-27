package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

/** "Sky colour": tints the sky */
@Mixin(ClientWorld.class)
public class SkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void asteria$sky(CallbackInfoReturnable<Vec3d> cir) {
        Vec3d v = cir.getReturnValue(); int rgb = ((int) (v.x * 255) << 16) | ((int) (v.y * 255) << 8) | (int) (v.z * 255), n = Fx.sky(rgb);
        if (n != rgb) cir.setReturnValue(new Vec3d(((n >> 16) & 255) / 255.0, ((n >> 8) & 255) / 255.0, (n & 255) / 255.0));
    }
}
