package ru.safimine.asteria.visuals.mc;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import org.joml.Matrix4f;
import ru.safimine.asteria.visuals.Fx;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.V;
import ru.safimine.asteria.visuals.mc.mixin.OverlayTextureAccessor;

import java.util.HashMap;
import java.util.Map;

/** Asteria Visuals (PREMIUM) for Minecraft 1.21.4. Needs no Fabric API: HUD, world, tick and key bindings are hooked by our own mixins. */
public class VisualsClient implements ClientModInitializer {
    public static final KeyBinding MENU = new KeyBinding("key.asteria.menu", InputUtil.Type.KEYSYM, 344, "category.asteria");
    public static final KeyBinding ZOOM = new KeyBinding("key.asteria.zoom", InputUtil.Type.KEYSYM, 67, "category.asteria");
    public static final KeyBinding[] KEYS = {MENU, ZOOM};
    private static final Map<String, Boolean> HELD = new HashMap<>();
    private static int appliedHit = 0;

    @Override public void onInitializeClient() { V.init(MinecraftClient.getInstance().getSession().getUsername()); }

    public static void hud(DrawContext ctx) { try { Hud.render(new Gfx121(ctx), Game121.I); } catch (Throwable ignored) {} }
    public static void world(Matrix4f pos, Camera cam, float pt) { try { Fx.render(new R3_121(pos, cam, pt), Game121.I); } catch (Throwable ignored) {} }

    public static void tick(MinecraftClient c) {
        try {
            V.tick();
            while (MENU.wasPressed()) if (c.currentScreen == null) c.setScreen(new AsteriaScreen());
            Fx.zoomKey(ZOOM.isPressed() && c.currentScreen == null);
            if (c.player != null) Fx.tick(Game121.I);
            long h = c.getWindow().getHandle();
            for (V.Mod m : V.MODS) {
                if (m.bind <= 0) continue;
                boolean down = c.currentScreen == null && InputUtil.isKeyPressed(h, m.bind);
                Boolean was = HELD.put(m.id, down);
                if (down && (was == null || !was)) { m.on = !m.on; V.changed(); Hud.notify(m.name, m.on ? "Включено" : "Выключено", m.on); }
            }
            int hit = V.on("hitcolor") ? V.color("hitcolor", "color") : 0;
            if (hit != appliedHit) { appliedHit = hit; recolorHit(c, hit); }
        } catch (Throwable ignored) {}
    }

    private static void recolorHit(MinecraftClient c, int argb) {
        NativeImageBackedTexture tex = ((OverlayTextureAccessor) c.gameRenderer.getOverlayTexture()).asteria$texture();
        NativeImage img = tex.getImage(); if (img == null) return;
        int r = argb == 0 ? 255 : (argb >> 16) & 255, g = argb == 0 ? 0 : (argb >> 8) & 255, b = argb == 0 ? 0 : argb & 255;
        int abgr = (0xB2 << 24) | (b << 16) | (g << 8) | r;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColorArgb(x, y, (0xB2 << 24) | (r << 16) | (g << 8) | b);
        tex.upload();
    }
}
