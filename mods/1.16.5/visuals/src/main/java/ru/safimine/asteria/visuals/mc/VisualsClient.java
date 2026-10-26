package ru.safimine.asteria.visuals.mc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import ru.safimine.asteria.visuals.Fx;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.V;
import ru.safimine.asteria.visuals.mc.mixin.OverlayTextureAccessor;

import java.util.HashMap;
import java.util.Map;

/** Asteria Visuals (PREMIUM) for Minecraft 1.16.5: wiring of the shared visuals code into the game. */
public class VisualsClient implements ClientModInitializer {
    public static KeyBinding MENU, ZOOM;
    private static final Map<String, Boolean> HELD = new HashMap<>();
    private static int appliedHit = 0;

    @Override public void onInitializeClient() {
        V.init(MinecraftClient.getInstance().getSession().getUsername());
        MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.menu", InputUtil.Type.KEYSYM, 344, "category.asteria"));
        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.zoom", InputUtil.Type.KEYSYM, 67, "category.asteria"));
        HudRenderCallback.EVENT.register((m, delta) -> { try { Hud.render(new Gfx116(m), Game116.I); } catch (Throwable ignored) {} });
        WorldRenderEvents.LAST.register(ctx -> { try { Fx.render(new R3_116(ctx), Game116.I); } catch (Throwable ignored) {} });
        ClientTickEvents.END_CLIENT_TICK.register(VisualsClient::tick);
    }

    private static void tick(MinecraftClient c) {
        try {
            V.tick();
            while (MENU.wasPressed()) if (c.currentScreen == null) c.openScreen(new AsteriaScreen());
            Fx.zoomKey(ZOOM.isPressed() && c.currentScreen == null);
            if (c.player != null) Fx.tick(Game116.I);
            // per-module key binds (only while playing)
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

    /** the red "got hurt" tint of entities lives in a 16x16 overlay texture; we repaint its upper half */
    private static void recolorHit(MinecraftClient c, int argb) {
        NativeImageBackedTexture tex = ((OverlayTextureAccessor) c.gameRenderer.getOverlayTexture()).asteria$texture();
        NativeImage img = tex.getImage(); if (img == null) return;
        int r = argb == 0 ? 255 : (argb >> 16) & 255, g = argb == 0 ? 0 : (argb >> 8) & 255, b = argb == 0 ? 0 : argb & 255;
        int abgr = (0xB2 << 24) | (b << 16) | (g << 8) | r;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setPixelColor(x, y, abgr);
        tex.upload();
    }
}
