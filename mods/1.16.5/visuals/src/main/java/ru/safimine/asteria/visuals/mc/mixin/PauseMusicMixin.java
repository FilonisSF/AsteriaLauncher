package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;

/** ESC menu: a music card with the track, its time, play / pause / skip and volume */
@Mixin(Screen.class)
public class PauseMusicMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void asteria$music(net.minecraft.client.util.math.MatrixStack m, int mx, int my, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof GameMenuScreen)) return;
        try { MinecraftClient c = MinecraftClient.getInstance(); boolean down = GLFW.glfwGetMouseButton(c.getWindow().getHandle(), 0) == GLFW.GLFW_PRESS;
            ru.safimine.asteria.visuals.mc.Gfx116 g = new ru.safimine.asteria.visuals.mc.Gfx116(m, true); try { float top = -1; for (Object o : ((net.minecraft.client.gui.screen.Screen) (Object) this).children()) if (o instanceof net.minecraft.client.gui.widget.ClickableWidget) { net.minecraft.client.gui.widget.ClickableWidget w = (net.minecraft.client.gui.widget.ClickableWidget) o; if (w.visible) top = Math.max(top, w.y + w.getHeight()); }
                Menu.pauseMusic(g, c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight(), mx, my, down, top); } finally { g.flush(); } } catch (Throwable ignored) {}
    }
}
