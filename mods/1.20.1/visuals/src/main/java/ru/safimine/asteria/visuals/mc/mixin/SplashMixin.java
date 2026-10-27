package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;

/** no yellow splash text next to the AsteriaLauncher logo */
@Mixin(SplashTextRenderer.class)
public abstract class SplashMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$noSplash(CallbackInfo ci) { if (Menu.on()) ci.cancel(); }
}
