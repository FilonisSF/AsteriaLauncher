package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.V;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** our font for all text: only letters, digits and punctuation come from it. Resource-pack icons (donate ranks), spacers and symbols are taken
 *  from the game's font as the server made them (they overlapped or showed as wrong glyphs). */
@Mixin(FontStorage.class)
public class FontSpaceMixin {
    @Shadow @Final private Identifier id;
    private static final Identifier ASTERIA_DEFAULT = new Identifier("minecraft", "default");
    private FontStorage asteria$vanilla(int cp) {
        if (V.ourGlyph(cp) || !VisualsClient.smoothFont(id)) return null;
        FontStorage f = ((TextRendererAccessor) MinecraftClient.getInstance().textRenderer).asteria$fonts().apply(ASTERIA_DEFAULT); return f == (Object) this ? null : f;
    }
    @Inject(method = "getGlyph(IZ)Lnet/minecraft/client/font/Glyph;", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$glyph(int cp, boolean validate, CallbackInfoReturnable<Glyph> cir) {
        if (cp == 32 && VisualsClient.smoothFont(id)) { return; }
        FontStorage f = asteria$vanilla(cp); if (f != null) cir.setReturnValue(f.getGlyph(cp, validate));
    }
    @Inject(method = "getGlyphRenderer(I)Lnet/minecraft/client/font/GlyphRenderer;", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$render(int cp, CallbackInfoReturnable<net.minecraft.client.font.GlyphRenderer> cir) { FontStorage f = asteria$vanilla(cp); if (f != null) cir.setReturnValue(f.getGlyphRenderer(cp)); }
}
