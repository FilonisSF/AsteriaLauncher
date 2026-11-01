package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** our TrueType fonts are sampled smoothly (bilinear) from glyphs made at twice the screen resolution: even edges at any HUD or GUI scale,
 *  instead of the hard pixel steps the game uses for its pixel font. Done right after the game binds the glyph page (its lambda keeps
 *  the filter it was made with, so the field alone would not do) */
@Mixin(targets = "net.minecraft.client.render.RenderPhase$Texture")
public class SmoothFontMixin {
    @Inject(method = "method_23563(Lnet/minecraft/util/Identifier;Lnet/minecraft/util/TriState;Z)V", at = @At("RETURN"), require = 0)
    private static void asteria$smooth(Identifier id, net.minecraft.util.TriState blur, boolean mipmap, CallbackInfo ci) {
        try { if (VisualsClient.smoothFont(id)) MinecraftClient.getInstance().getTextureManager().getTexture(id).setFilter(true, mipmap); } catch (Throwable ignored) {}
    }
}
