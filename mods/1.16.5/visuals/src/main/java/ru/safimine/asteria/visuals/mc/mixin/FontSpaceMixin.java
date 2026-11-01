package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** 1.16 gives every font the pixel font's 4-unit space: with our smooth fonts the words stood far apart. Their own width instead */
@Mixin(FontStorage.class)
public class FontSpaceMixin {
    @Shadow @Final private Identifier id;
    private static final Glyph ASTERIA_SPACE = () -> 2.5f;
    @Inject(method = "getGlyph", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$space(int cp, CallbackInfoReturnable<Glyph> cir) { if (cp == 32 && VisualsClient.smoothFont(id)) cir.setReturnValue(ASTERIA_SPACE); }
}
