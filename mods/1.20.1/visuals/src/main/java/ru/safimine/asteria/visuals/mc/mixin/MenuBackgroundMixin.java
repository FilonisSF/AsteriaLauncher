package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import ru.safimine.asteria.visuals.mc.Gfx120;

/** the Asteria picture instead of the dirt background of menu screens */
@Mixin(Screen.class)
public abstract class MenuBackgroundMixin {
    @Inject(method = "renderBackgroundTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$bg(DrawContext ctx, CallbackInfo ci) {
        if (!Menu.on()) return;
        try { Menu.onTitle = MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen; Menu.background(new Gfx120(ctx), ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight()); ci.cancel(); } catch (Throwable ignored) {}
    }
}
