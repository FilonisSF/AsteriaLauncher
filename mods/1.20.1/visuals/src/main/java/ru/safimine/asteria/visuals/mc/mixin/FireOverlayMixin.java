package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** "No fire on the screen" */
@Mixin(InGameOverlayRenderer.class)
public class FireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void asteria$fire(CallbackInfo ci) { if (V.on("nofire")) ci.cancel(); }
}
