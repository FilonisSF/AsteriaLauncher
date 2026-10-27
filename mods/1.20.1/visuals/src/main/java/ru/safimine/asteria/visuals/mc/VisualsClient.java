package ru.safimine.asteria.visuals.mc;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import ru.safimine.asteria.visuals.Fx;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.V;
import ru.safimine.asteria.visuals.mc.mixin.OverlayTextureAccessor;

import java.util.HashMap;
import java.util.Map;

/** Asteria Visuals (PREMIUM) for Minecraft 1.20.x. Needs no Fabric API: HUD, world, tick and key bindings are hooked by our own mixins. */
public class VisualsClient implements ClientModInitializer {
    public static final KeyBinding MENU = new KeyBinding("key.asteria.menu", InputUtil.Type.KEYSYM, 344, "category.asteria");
    public static final KeyBinding ZOOM = new KeyBinding("key.asteria.zoom", InputUtil.Type.KEYSYM, 67, "category.asteria");
    public static final KeyBinding FREELOOK = new KeyBinding("key.asteria.freelook", InputUtil.Type.KEYSYM, 342, "category.asteria");
    public static final KeyBinding MARK = new KeyBinding("key.asteria.mark", InputUtil.Type.KEYSYM, 66, "category.asteria");
    public static final KeyBinding[] KEYS = {MENU, ZOOM, FREELOOK, MARK};
    private static final Map<String, Boolean> HELD = new HashMap<>();
    private static int appliedHit = 0;

    @Override public void onInitializeClient() { V.init(MinecraftClient.getInstance().getSession().getUsername()); ru.safimine.asteria.visuals.Menu.tr = k -> net.minecraft.client.resource.language.I18n.translate(k); }

    public static void hud(DrawContext ctx) { try { Hud.render(new Gfx120(ctx), Game120.I); } catch (Throwable ignored) {} }
    public static void world(MatrixStack ms, Camera cam, float pt) { try { Fx.render(new R3_120(ms, cam, pt), Game120.I); } catch (Throwable ignored) {} }

    public static void tick(MinecraftClient c) {
        try {
            V.tick();
            while (MENU.wasPressed()) if (c.currentScreen == null) c.setScreen(new AsteriaScreen());
            Fx.zoomKey(ZOOM.isPressed() && c.currentScreen == null);
            while (MARK.wasPressed()) if (c.player != null && c.currentScreen == null) Fx.markKey(Game120.I, net.minecraft.client.gui.screen.Screen.hasShiftDown());
            if (c.player != null) Fx.freeLookKey(Game120.I, FREELOOK.isPressed() && c.currentScreen == null, c.player.getYaw(), c.player.getPitch());
            if (c.player != null) Fx.tick(Game120.I);
            long h = c.getWindow().getHandle();
            for (V.Mod m : V.MODS) {
                if (m.bind == 0) continue;
                boolean down = c.currentScreen == null && Game120.I.rawKey(m.bind);
                Boolean was = HELD.put(m.id, down);
                if (down && (was == null || !was)) { m.on = !m.on; V.changed(); Hud.bindToggled(m); }
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
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColor(x, y, abgr);
        tex.upload();
    }
}
