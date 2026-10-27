package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;
import net.minecraft.client.render.Camera;

/** "Third person camera": distance from the player */
@Mixin(Camera.class)
public class CameraMixin {
    @ModifyVariable(method = "clipToSpace", at = @At("HEAD"), argsOnly = true, require = 0)
    private double asteria$dist(double d) { return (double) Fx.camDist(d); }
}
