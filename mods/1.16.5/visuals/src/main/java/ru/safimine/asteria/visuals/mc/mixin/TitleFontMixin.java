package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** our font for all text: the big screen titles (drawn 4x, the subtitle 2x) get a finer copy of the font, or their letters were blocky */
@Mixin(InGameHud.class)
public class TitleFontMixin {
    @Shadow private Text title;
    @Shadow private Text subtitle;
    private Text asteria$title, asteria$subtitle;
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), require = 0)
    private void asteria$titleFont(net.minecraft.client.util.math.MatrixStack m, float td, CallbackInfo ci) {
        try { if (title != null && title != asteria$title) title = asteria$title = VisualsClient.titleText(title, 4);
            if (subtitle != null && subtitle != asteria$subtitle) subtitle = asteria$subtitle = VisualsClient.titleText(subtitle, 2); } catch (Throwable ignored) {}
    }
}
