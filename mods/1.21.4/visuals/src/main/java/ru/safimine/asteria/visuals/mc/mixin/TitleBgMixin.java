package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import ru.safimine.asteria.visuals.mc.Gfx121;

/** main menu: the own picture from the launcher (runs before the base mod, which draws our art otherwise) */
@Mixin(value = TitleScreen.class, priority = 900)
public abstract class TitleBgMixin {
    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$titleBg(DrawContext ctx, float delta, CallbackInfo ci) {
        if (!Menu.on() || !Menu.ownBg()) return;
        try { Menu.onTitle = true; Gfx121 g = new Gfx121(ctx, true); try { Menu.background(g, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight()); } finally { g.flush(); } ci.cancel(); } catch (Throwable ignored) {}
    }
}
