package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import ru.safimine.asteria.visuals.mc.Gfx120;

/** "AsteriaLauncher" with the AL mark instead of "Minecraft Java Edition" */
@Mixin(LogoDrawer.class)
public abstract class LogoMixin {
    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$logo(DrawContext ctx, int screenWidth, float alpha, int y, CallbackInfo ci) {
        if (!Menu.on()) return;
        try { Menu.logo(new Gfx120(ctx), screenWidth, y); ci.cancel(); } catch (Throwable ignored) {}
    }
}
