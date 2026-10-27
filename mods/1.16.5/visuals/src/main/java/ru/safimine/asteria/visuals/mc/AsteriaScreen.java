package ru.safimine.asteria.visuals.mc;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.client.gui.screen.Screen;
import ru.safimine.asteria.visuals.Gui;

/** the Asteria menu: everything is drawn by the shared {@link Gui}; this class only forwards input */
public class AsteriaScreen extends Screen {
    public AsteriaScreen() { super(new LiteralText("Asteria")); Gui.opened(); }
    @Override public void render(MatrixStack m, int mx, int my, float delta) { Gfx116 g = new Gfx116(m, true); try { Gui.render(g, Game116.I, mx, my); } finally { g.flush(); } }
    @Override public boolean mouseClicked(double x, double y, int b) { Gui.click((float) x, (float) y, b); return true; }
    @Override public boolean mouseReleased(double x, double y, int b) { Gui.release(); return true; }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy) { Gui.drag((float) x, (float) y); return true; }
    @Override public boolean mouseScrolled(double x, double y, double amount) { Gui.scroll(amount); return true; }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (Gui.key(key)) return true;
        if (key == 256 || VisualsClient.MENU.matchesKey(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
    @Override public boolean charTyped(char ch, int mods) { Gui.ch(ch); return true; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { Gui.closed(); }
}
