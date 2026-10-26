package ru.safimine.asteria.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Asteria crystal background in the main menu instead of the rotating panorama (slow "breathing" zoom). */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique private static final Identifier ASTERIA_BG = new Identifier("asteria", "textures/gui/menu_bg.png");

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;render(FF)V"), require = 0)
    private void asteria$background(RotatingCubeMapRenderer panorama, float delta, float alpha) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
            mc.getTextureManager().bindTexture(ASTERIA_BG);
            AbstractTexture t = mc.getTextureManager().getTexture(ASTERIA_BG); if (t != null) t.setFilter(true, false);
            double a = (System.currentTimeMillis() % 60000L) / 60000.0 * Math.PI * 2;
            float s = Math.max(w / 1280f, h / 720f) * (1.07f + 0.035f * (float) Math.sin(a));
            MatrixStack m = new MatrixStack();
            m.translate(w / 2f + (float) Math.cos(a) * w * 0.012f, h / 2f + (float) Math.sin(a * 2) * h * 0.008f, 0);
            m.scale(s, s, 1); m.translate(-640, -360, 0);
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.color4f(1, 1, 1, alpha);
            DrawableHelper.drawTexture(m, 0, 0, 0f, 0f, 1280, 720, 1280, 720);
            RenderSystem.color4f(1, 1, 1, 1);
        } catch (Throwable e) { panorama.render(delta, alpha); }
    }
}
