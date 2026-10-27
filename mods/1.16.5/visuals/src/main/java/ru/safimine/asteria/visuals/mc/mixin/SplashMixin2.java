package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;

/** our loading screen over Minecraft's (AL mark, rings, progress), fading out when loading is done */
@Mixin(SplashScreen.class)
public class SplashMixin2 {
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void asteria$splash(net.minecraft.client.util.math.MatrixStack m, int mx, int my, float delta, CallbackInfo ci) {
        try { if (!ru.safimine.asteria.visuals.V.menu) return; MinecraftClient c = MinecraftClient.getInstance(); float a = reloadCompleteTime < 0 ? 1 : Math.max(0, 1 - (System.currentTimeMillis() - reloadCompleteTime) / 900f);
            Menu.splash(new ru.safimine.asteria.visuals.mc.Gfx116(m), c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight(), progress, a); } catch (Throwable ignored) {}
    }
}
