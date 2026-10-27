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

/** title screen: no Realms button (the other buttons move up into its place) */
@Mixin(TitleScreen.class)
public abstract class TitleMenuMixin {
    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void asteria$noRealms(CallbackInfo ci) {
        if (!Menu.on()) return;
        try {
            Screen s = (Screen) (Object) this; String realms = I18n.translate("menu.online"); ClickableWidget rb = null;
            for (Element e : s.children()) if (e instanceof ClickableWidget && ((ClickableWidget) e).getMessage().getString().equals(realms)) rb = (ClickableWidget) e;
            if (rb == null) return;
            int ry = rb.getY(); rb.visible = false; rb.active = false;
            for (Element e : s.children()) if (e instanceof ClickableWidget && e != rb && ((ClickableWidget) e).getY() > ry) ((ClickableWidget) e).setY(((ClickableWidget) e).getY() - 24);
        } catch (Throwable ignored) {}
    }
}
