package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.V;

/** your own time of day (only for you) */
@Mixin(ClientWorld.Properties.class)
public class WorldPropertiesMixin {
    private static final long[] TIMES = {1000, 6000, 12600, 15000, 18000};
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void asteria$time(CallbackInfoReturnable<Long> cir) { if (V.on("worldtime")) cir.setReturnValue(TIMES[V.mode("worldtime", "time")]); }
}
