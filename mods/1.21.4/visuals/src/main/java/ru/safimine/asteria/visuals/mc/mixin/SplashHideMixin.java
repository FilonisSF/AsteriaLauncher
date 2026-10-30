package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Minecraft's own loading screen under ours: dark instead of red, without the Mojang logo */
@Mixin(SplashOverlay.class)
public abstract class SplashHideMixin {
    /** the brand colour is also used to clear the whole window: dark there too (a red frame flashed otherwise) */
    @Shadow @Final @Mutable private static java.util.function.IntSupplier BRAND_ARGB;
    @Inject(method = "<clinit>", at = @At("TAIL"), require = 0)
    private static void asteria$darkBrand(CallbackInfo ci) { BRAND_ARGB = () -> 0xFF06121A; }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashOverlay;withAlpha(II)I"), require = 0)
    private int asteria$dark(int color, int alpha) { return 0x0006121A; } // no dark veil: our screen is drawn instead
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"), require = 0)
    private void asteria$noLogo(DrawContext ctx, java.util.function.Function<?, ?> layer, net.minecraft.util.Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {}
}
