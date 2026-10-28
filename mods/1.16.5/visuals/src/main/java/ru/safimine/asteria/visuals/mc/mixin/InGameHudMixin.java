package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
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
    private void asteria$crosshair(MatrixStack m, CallbackInfo ci) { if (V.on("crosshair") && V.mode("crosshair", "style") != 7) { ci.cancel(); return; } // style 7 = vanilla
        xhRot = V.on("crosshair") && V.num("crosshair", "rotate") != 0; if (xhRot) { net.minecraft.client.MinecraftClient c = net.minecraft.client.MinecraftClient.getInstance(); float cx = c.getWindow().getScaledWidth() / 2f, cy = c.getWindow().getScaledHeight() / 2f;
            m.push(); m.translate(cx, cy, 0); m.multiply(net.minecraft.util.math.Vec3f.POSITIVE_Z.getDegreesQuaternion((float) V.num("crosshair", "rotate"))); m.translate(-cx, -cy, 0); } }
    private boolean xhRot;
    @Inject(method = "renderCrosshair", at = @At("RETURN"), require = 0) // every return: vanilla leaves early in third person
    private void asteria$crosshairEnd(MatrixStack m, CallbackInfo ci) { if (xhRot) { xhRot = false; m.pop(); } }

    /** our HUD goes under the scoreboard, chat and Tab (drawn before them); at the end of the frame only if that spot was not reached */
    private static boolean hudDrawn;
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), require = 0)
    private void asteria$hudStart(CallbackInfo ci) { hudDrawn = false; }
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getScoreboard()Lnet/minecraft/scoreboard/Scoreboard;", ordinal = 0), require = 0)
    private void asteria$hudEarly(net.minecraft.client.util.math.MatrixStack m, float delta, CallbackInfo ci) { if (!hudDrawn) { hudDrawn = true; ru.safimine.asteria.visuals.mc.VisualsClient.hud(m); } }
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("TAIL"), require = 0)
    private void asteria$hudEnd(net.minecraft.client.util.math.MatrixStack m, float delta, CallbackInfo ci) { if (!hudDrawn) { hudDrawn = true; ru.safimine.asteria.visuals.mc.VisualsClient.hud(m); } }
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$noScoreboard(CallbackInfo ci) { if (ru.safimine.asteria.visuals.V.on("noscoreboard")) ci.cancel(); }
}
