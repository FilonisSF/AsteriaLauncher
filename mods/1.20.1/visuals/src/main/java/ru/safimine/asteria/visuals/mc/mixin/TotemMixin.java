package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;
import net.minecraft.client.render.GameRenderer;

/** "Totem animation": our burst when a totem saves you, optionally without the big vanilla totem */
@Mixin(GameRenderer.class)
public class TotemMixin {
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$totem(CallbackInfo ci) { if (Fx.totem()) ci.cancel(); }
}
