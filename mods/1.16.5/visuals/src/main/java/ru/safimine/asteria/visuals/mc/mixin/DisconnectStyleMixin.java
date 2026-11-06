package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.mc.Gfx116;

/** the "Disconnected" screen gets an Asteria card behind its text and button (drawn right after the background, so they stay on top) */
@Mixin(Screen.class)
public class DisconnectStyleMixin {
    @Shadow public int width;
    @Shadow public int height;
    @Inject(method = "renderBackground(Lnet/minecraft/client/util/math/MatrixStack;I)V", at = @At("TAIL"), require = 0)
    private void asteria$card(net.minecraft.client.util.math.MatrixStack m, int vOffset, CallbackInfo ci) {
        if (!((Object) this instanceof DisconnectedScreen)) return;
        try { Hud.disconnectPanel(new Gfx116(m), width, height); } catch (Throwable ignored) {}
    }
}
