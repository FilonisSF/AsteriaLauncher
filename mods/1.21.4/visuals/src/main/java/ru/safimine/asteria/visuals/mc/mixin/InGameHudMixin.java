package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import ru.safimine.asteria.visuals.mc.VisualsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** custom crosshair: the vanilla one is hidden (ours is drawn by the HUD) */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    /** "Hotbar" draws its own bar, the vanilla one is skipped */
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$hotbar(CallbackInfo ci) { if (V.on("hotbar")) ci.cancel(); }
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void asteria$crosshair(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc, CallbackInfo ci) { if (V.on("crosshair")) ci.cancel(); }
    @Inject(method = "render", at = @At("TAIL"))
    private void asteria$hud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc, CallbackInfo ci) { VisualsClient.hud(ctx); }
}
