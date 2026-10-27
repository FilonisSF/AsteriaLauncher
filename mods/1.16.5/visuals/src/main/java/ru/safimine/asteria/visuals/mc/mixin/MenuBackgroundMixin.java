package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import ru.safimine.asteria.visuals.mc.Gfx116;

/** the Asteria picture instead of the dirt background of menu screens */
@Mixin(Screen.class)
public abstract class MenuBackgroundMixin {
    @Inject(method = "renderBackgroundTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$bg(int vOffset, CallbackInfo ci) {
        if (!Menu.on()) return; MinecraftClient c = MinecraftClient.getInstance();
        try { Menu.onTitle = MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen; Menu.background(new Gfx116(new MatrixStack()), c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight()); ci.cancel(); } catch (Throwable ignored) {}
    }
}
