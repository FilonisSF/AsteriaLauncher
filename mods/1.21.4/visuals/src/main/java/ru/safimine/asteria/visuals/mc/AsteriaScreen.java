package ru.safimine.asteria.visuals.mc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.safimine.asteria.visuals.Gui;

/** the Asteria menu: everything is drawn by the shared {@link Gui}; this class only forwards input */
public class AsteriaScreen extends Screen {
    public AsteriaScreen() { super(Text.literal("Asteria")); Gui.opened(); }
    @Override public void render(DrawContext ctx, int mx, int my, float delta) { Gfx121 g = new Gfx121(ctx, true); try { Gui.render(g, Game121.I, mx, my); } finally { g.flush(); } }
    @Override public boolean mouseClicked(double x, double y, int b) { Gui.click((float) x, (float) y, b); return true; }
    @Override public boolean mouseReleased(double x, double y, int b) { Gui.release(); return true; }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy) { Gui.drag((float) x, (float) y); return true; }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { Gui.scroll(v); return true; }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (Gui.key(key)) return true;
        if (key == 256 || VisualsClient.MENU.matchesKey(key, scan)) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }
    @Override public boolean charTyped(char ch, int mods) { Gui.ch(ch); return true; }
    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Gui.closed(); }
}
