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
    /** buttons other mods add after our init (screen events) are untangled too */
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void asteria$untangleLate(net.minecraft.client.gui.DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) { if (Menu.on()) try { asteria$untangle((Screen) (Object) this); } catch (Throwable ignored) {} }
    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void asteria$noRealms(CallbackInfo ci) {
        if (!Menu.on()) return;
        try {
            Screen s = (Screen) (Object) this; String realms = I18n.translate("menu.online"); ClickableWidget rb = null;
            for (Element e : s.children()) if (e instanceof ClickableWidget && ((ClickableWidget) e).getMessage().getString().equals(realms)) rb = (ClickableWidget) e;
            if (rb == null) return;
            int ry = rb.getY(); rb.visible = false; rb.active = false;
            for (Element e : s.children()) if (e instanceof ClickableWidget && e != rb && ((ClickableWidget) e).getY() > ry) ((ClickableWidget) e).setY(((ClickableWidget) e).getY() - 24);
            asteria$untangle(s);
        } catch (Throwable ignored) {}
    }
    /** buttons other mods put on the title screen (account switchers and such) stay where they were while ours move up a row:
     *  a small one that lies on another button moves outwards, away from the middle, until it is free */
    private static void asteria$untangle(Screen s) {
        java.util.List<ClickableWidget> all = new java.util.ArrayList<>(); for (Element e : s.children()) if (e instanceof ClickableWidget && ((ClickableWidget) e).visible) all.add((ClickableWidget) e);
        for (int i = all.size() - 1; i >= 0; i--) { ClickableWidget w = all.get(i), o = asteria$hit(w, all); if (w.getWidth() > 24 || o == null) continue; // the one added last moves
            w.setY(o.getY()); int dir = w.getX() + w.getWidth() / 2 < s.width / 2 ? -24 : 24; // into the row of the button it lay on, then sideways
            for (int n = 0; n < 8 && asteria$hit(w, all) != null; n++) w.setX(w.getX() + dir); }
    }
    private static ClickableWidget asteria$hit(ClickableWidget w, java.util.List<ClickableWidget> all) {
        for (ClickableWidget o : all) if (o != w && w.getX() < o.getX() + o.getWidth() && o.getX() < w.getX() + w.getWidth() && w.getY() < o.getY() + 20 && o.getY() < w.getY() + 20) return o; return null;
    }
}
