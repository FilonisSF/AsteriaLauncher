package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.safimine.asteria.visuals.mc.Gfx116;
import java.util.function.BiConsumer;

/** title screen: no Realms button, and our logo instead of "Minecraft Java Edition" */
@Mixin(TitleScreen.class)
public abstract class TitleMenuMixin {
    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void asteria$noRealms(CallbackInfo ci) {
        if (!Menu.on()) return;
        try {
            Screen s = (Screen) (Object) this; String realms = I18n.translate("menu.online"); ClickableWidget rb = null;
            for (Element e : s.children()) if (e instanceof ClickableWidget && ((ClickableWidget) e).getMessage().getString().equals(realms)) rb = (ClickableWidget) e;
            if (rb == null) return;
            int ry = rb.y; rb.visible = false; rb.active = false;
            for (Element e : s.children()) if (e instanceof ClickableWidget && e != rb && ((ClickableWidget) e).y > ry) ((ClickableWidget) e).y = ((ClickableWidget) e).y - 24;
        } catch (Throwable ignored) {}
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;method_29343(IILjava/util/function/BiConsumer;)V"), require = 0)
    private void asteria$noLogo(TitleScreen s, int x, int y, BiConsumer<Integer, Integer> draw) { if (!Menu.on()) s.method_29343(x, y, draw); }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIFFIIII)V"), require = 0)
    private void asteria$noEdition(MatrixStack m, int x, int y, float u, float v, int w, int h, int tw, int th) { if (!Menu.on()) net.minecraft.client.gui.DrawableHelper.drawTexture(m, x, y, u, v, w, h, tw, th); }
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void asteria$logo(MatrixStack m, int mx, int my, float delta, CallbackInfo ci) {
        if (!Menu.on()) return;
        try { Menu.logo(new Gfx116(m), MinecraftClient.getInstance().getWindow().getScaledWidth(), 30); } catch (Throwable ignored) {}
    }
}
