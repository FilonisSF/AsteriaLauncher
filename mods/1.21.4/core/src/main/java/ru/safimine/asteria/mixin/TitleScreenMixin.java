package ru.safimine.asteria.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Asteria crystal background in the main menu instead of the rotating panorama (slow "breathing" zoom). */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique private static final Identifier ASTERIA_BG = Identifier.of("asteria", "textures/gui/menu_bg.png");

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$background(DrawContext c, float delta, CallbackInfo ci) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
            AbstractTexture t = mc.getTextureManager().getTexture(ASTERIA_BG); if (t != null) t.setFilter(true, false);
            double a = (System.currentTimeMillis() % 60000L) / 60000.0 * Math.PI * 2;
            float s = Math.max(w / 1280f, h / 720f) * (1.07f + 0.035f * (float) Math.sin(a));
            c.getMatrices().push();
            c.getMatrices().translate(w / 2f + (float) Math.cos(a) * w * 0.012f, h / 2f + (float) Math.sin(a * 2) * h * 0.008f, 0);
            c.getMatrices().scale(s, s, 1); c.getMatrices().translate(-640, -360, 0);
            c.drawTexture(RenderLayer::getGuiTextured, ASTERIA_BG, 0, 0, 0f, 0f, 1280, 720, 1280, 720);
            c.getMatrices().pop();
            ci.cancel();
        } catch (Throwable ignored) {}
    }
}
