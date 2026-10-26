package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.V;

/** clear sky (only for you) */
@Mixin(World.class)
public class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void asteria$rain(float delta, CallbackInfoReturnable<Float> cir) { if ((Object) this instanceof ClientWorld && V.on("worldtime") && V.bool("worldtime", "clear")) cir.setReturnValue(0f); }
    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void asteria$thunder(float delta, CallbackInfoReturnable<Float> cir) { if ((Object) this instanceof ClientWorld && V.on("worldtime") && V.bool("worldtime", "clear")) cir.setReturnValue(0f); }
}
