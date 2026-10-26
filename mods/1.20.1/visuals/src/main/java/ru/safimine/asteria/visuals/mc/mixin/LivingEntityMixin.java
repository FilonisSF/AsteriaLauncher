package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.V;

/** slower, smoother swing animation of your own hand (only what you see) */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void asteria$swing(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == MinecraftClient.getInstance().player && V.on("swing")) cir.setReturnValue((int) V.num("swing", "speed"));
    }
}
