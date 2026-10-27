package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;

/** free look: while the key is held the mouse turns the camera, not the player */
@Mixin(Entity.class)
public class FreeLookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$look(double dx, double dy, CallbackInfo ci) {
        if (Fx.freeLook && (Object) this == MinecraftClient.getInstance().player) { Fx.freeLookTurn(dx, dy); ci.cancel(); }
    }
}
