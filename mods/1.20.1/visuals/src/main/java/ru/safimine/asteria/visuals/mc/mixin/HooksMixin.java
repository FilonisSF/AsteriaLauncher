package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** client tick (instead of Fabric API lifecycle events) */
@Mixin(MinecraftClient.class)
public class HooksMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void asteria$tick(CallbackInfo ci) { VisualsClient.tick((MinecraftClient) (Object) this); }
}
