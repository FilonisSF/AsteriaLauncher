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
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIFFIIII)V", ordinal = 0), require = 0)
    private void asteria$titleBg(net.minecraft.client.util.math.MatrixStack m, int mx, int my, float d, CallbackInfo ci) {
        if (!Menu.on() || !Menu.ownBg()) return;
        try { Menu.onTitle = true; ru.safimine.asteria.visuals.mc.Gfx116 g = new ru.safimine.asteria.visuals.mc.Gfx116(m, true); try { Menu.background(g, net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth(), net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight()); } finally { g.flush(); } } catch (Throwable ignored) {}
    }
}
