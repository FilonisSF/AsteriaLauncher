package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;
import net.minecraft.client.world.ClientWorld;

/** "Sky colour": tints the sky (1.21.2+ returns the colour as an int) */
@Mixin(ClientWorld.class)
public class SkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void asteria$sky(CallbackInfoReturnable<Integer> cir) { int c = cir.getReturnValueI(); int n = Fx.sky(c & 0xFFFFFF); if (n != (c & 0xFFFFFF)) cir.setReturnValue((c & 0xFF000000) | n); }
}
