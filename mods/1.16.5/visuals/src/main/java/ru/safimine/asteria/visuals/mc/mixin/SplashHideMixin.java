package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Minecraft's own loading screen under ours: dark instead of red, without the Mojang logo */
@Mixin(SplashScreen.class)
public abstract class SplashHideMixin {
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/SplashScreen;BRAND_RGB:I", opcode = 178 /* GETSTATIC */), require = 0)
    private int asteria$darkRgb() { return 0x06121A; }
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/SplashScreen;BRAND_ARGB:I", opcode = 178 /* GETSTATIC */), require = 0)
    private int asteria$darkArgb() { return 0xFF000000 | 0x06121A; }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashScreen;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIFFIIII)V"), require = 0)
    private void asteria$noLogo(MatrixStack m, int x, int y, int w, int h, float u, float v, int rw, int rh, int tw, int th) {}
    /** no dark veil (the fills under the logo): our screen is drawn instead */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashScreen;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"), require = 0)
    private void asteria$noFill(MatrixStack m, int x1, int y1, int x2, int y2, int color) {}
}
