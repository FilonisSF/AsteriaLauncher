package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import ru.safimine.asteria.visuals.mc.Gfx121;

/** the Asteria picture instead of the panorama behind menu screens (title screen included) */
@Mixin(Screen.class)
public abstract class MenuBackgroundMixin {
    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$bg(DrawContext ctx, float delta, CallbackInfo ci) {
        if (!Menu.on()) return;
        try { Menu.background(new Gfx121(ctx), ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight()); ci.cancel(); } catch (Throwable ignored) {}
    }
}
