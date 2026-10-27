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
    private void asteria$crosshair(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc, CallbackInfo ci) { if (V.on("crosshair") && V.mode("crosshair", "style") != 7) ci.cancel(); } // style 7 = vanilla

    /** our HUD goes under the scoreboard, chat and Tab (drawn before them); at the end of the frame only if that spot was not reached */
    private static boolean hudDrawn;
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), require = 0)
    private void asteria$hudStart(CallbackInfo ci) { hudDrawn = false; }
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), require = 0)
    private void asteria$hudEarly(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc, CallbackInfo ci) { if (!hudDrawn) { hudDrawn = true; VisualsClient.hud(ctx); } }
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"), require = 0)
    private void asteria$hudEnd(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc, CallbackInfo ci) { if (!hudDrawn) { hudDrawn = true; VisualsClient.hud(ctx); } }
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$noScoreboard(CallbackInfo ci) { if (ru.safimine.asteria.visuals.V.on("noscoreboard")) ci.cancel(); }
}
