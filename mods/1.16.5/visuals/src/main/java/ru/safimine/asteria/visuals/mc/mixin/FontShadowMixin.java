package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.font.Glyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** our font for all text: the shadow of the smooth letters sits half as far (a whole font pixel made them look heavy and doubled) */
@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public class FontShadowMixin {
    @Redirect(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getShadowOffset()F"), require = 0)
    private float asteria$shadow(Glyph g) { return VisualsClient.ttfShadow(g, g.getShadowOffset()); }
}
