package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;

/** our loading screen (background, logo in the chosen font, our bar) drawn where Minecraft draws its progress bar; only pictures: shapes and fonts are not loaded yet */
@Mixin(SplashOverlay.class)
public abstract class SplashMixin2 {
    @Shadow private long reloadCompleteTime;
    @Shadow private float progress;
    @Shadow private void renderProgressBar(net.minecraft.client.gui.DrawContext m, int x1, int y1, int x2, int y2, float opacity) {}
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashOverlay;renderProgressBar(Lnet/minecraft/client/gui/DrawContext;IIIIF)V"), require = 0)
    private void asteria$splash(SplashOverlay self, net.minecraft.client.gui.DrawContext m, int x1, int y1, int x2, int y2, float opacity) {
        try { MinecraftClient c = MinecraftClient.getInstance(); Menu.splash(new ru.safimine.asteria.visuals.mc.Gfx120(m), c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight(), 1, progress, reloadCompleteTime < 0); }
        catch (Throwable t) { renderProgressBar(m, x1, y1, x2, y2, opacity); } // something went wrong: at least the vanilla bar
    }
    /** after loading Minecraft fades into the menu without calling the bar: our screen (no bar) fades with it instead of a dark veil */
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void asteria$fadeOut(net.minecraft.client.gui.DrawContext m, int mx, int my, float delta, CallbackInfo ci) {
        if (reloadCompleteTime < 0) return; float f = (System.currentTimeMillis() - reloadCompleteTime) / 1000f; if (f < 1 || f >= 2) return;
        try { MinecraftClient c = MinecraftClient.getInstance(); Menu.splash(new ru.safimine.asteria.visuals.mc.Gfx120(m), c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight(), 2 - f, 1, false); } catch (Throwable ignored) {}
    }
}
