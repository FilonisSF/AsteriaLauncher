package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;

/** main menu: the own picture from the launcher, drawn over the background of the base mod, right before the vanilla shading on top */
@Mixin(TitleScreen.class)
public abstract class TitleBgMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V", ordinal = 0), require = 0)
    private void asteria$titleBg(net.minecraft.client.gui.DrawContext ctx, int mx, int my, float d, CallbackInfo ci) {
        if (!Menu.on() || !Menu.ownBg()) return;
        try { Menu.onTitle = true; ru.safimine.asteria.visuals.mc.Gfx120 g = new ru.safimine.asteria.visuals.mc.Gfx120(ctx, true); try { Menu.background(g, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight()); } finally { g.flush(); } } catch (Throwable ignored) {}
    }
}
