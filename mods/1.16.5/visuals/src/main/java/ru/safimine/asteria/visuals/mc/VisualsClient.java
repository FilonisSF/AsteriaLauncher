package ru.safimine.asteria.visuals.mc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
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
    public static KeyBinding MENU, ZOOM, FREELOOK, MARK, M_TOGGLE, M_NEXT, M_PREV, M_UP, M_DOWN;
    private static final Map<String, Boolean> HELD = new HashMap<>();
    private static int appliedHit = 0;

    @Override public void onInitializeClient() {
        V.init(MinecraftClient.getInstance().getSession().getUsername()); ru.safimine.asteria.visuals.Menu.tr = k -> net.minecraft.client.resource.language.I18n.translate(k);
        MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.menu", InputUtil.Type.KEYSYM, 344, "category.asteria"));
        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.zoom", InputUtil.Type.KEYSYM, 67, "category.asteria"));
        M_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.music_toggle", InputUtil.Type.KEYSYM, -1, "category.asteria"));
        M_NEXT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.music_next", InputUtil.Type.KEYSYM, -1, "category.asteria"));
        M_PREV = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.music_prev", InputUtil.Type.KEYSYM, -1, "category.asteria"));
        M_UP = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.music_up", InputUtil.Type.KEYSYM, -1, "category.asteria"));
        M_DOWN = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.music_down", InputUtil.Type.KEYSYM, -1, "category.asteria"));
        MARK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.mark", InputUtil.Type.KEYSYM, 66, "category.asteria"));
        FREELOOK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.asteria.freelook", InputUtil.Type.KEYSYM, 342, "category.asteria"));
        // the HUD is drawn by InGameHudMixin under the chat, Tab and scoreboard
        WorldRenderEvents.LAST.register(ctx -> { try { Fx.render(new R3_116(ctx), Game116.I); } catch (Throwable t) { Fx.err("world", t); } });
        ClientTickEvents.END_CLIENT_TICK.register(VisualsClient::tick);
    }

    private static void tick(MinecraftClient c) {
        try {
            V.tick();
            while (MENU.wasPressed()) if (c.currentScreen == null) c.openScreen(new AsteriaScreen());
            Fx.zoomKey(ZOOM.isPressed() && c.currentScreen == null);
            if (c.player != null && V.on("autosprint") && c.options.keyForward.isPressed() && !c.player.isSneaking() && c.player.getHungerManager().getFoodLevel() > 6 && !c.player.isUsingItem()) c.player.setSprinting(true);
            Fx.commandBinds(Game116.I, c.currentScreen != null);
            while (M_TOGGLE.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("toggle"); while (M_NEXT.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("next"); while (M_PREV.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("prev");
            while (M_UP.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("volup"); while (M_DOWN.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("voldown");
            while (MARK.wasPressed()) if (c.player != null && c.currentScreen == null) Fx.markKey(Game116.I, net.minecraft.client.gui.screen.Screen.hasShiftDown());
            if (c.player != null) Fx.freeLookKey(Game116.I, FREELOOK.isPressed() && c.currentScreen == null, c.player.yaw, c.player.pitch);
            if (c.player != null) Fx.tick(Game116.I);
            // per-module key binds (only while playing)
            long h = c.getWindow().getHandle();
            for (V.Mod m : V.MODS) {
                if (m.bind == 0) continue;
                boolean down = c.currentScreen == null && Game116.I.rawKey(m.bind);
                Boolean was = HELD.put(m.id, down);
                if (down && (was == null || !was)) { m.on = !m.on; V.changed(); Hud.bindToggled(m); }
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
    /** chat messages of ours (.метка ...) stay in the client */
    public static boolean chat(String msg) { return Fx.chatCommand(Game116.I, msg); }
    public static void hud(net.minecraft.client.util.math.MatrixStack m) { Gfx116 g = new Gfx116(m, true); try { Hud.render(g, Game116.I); } catch (Throwable t) { Fx.err("hud", t); } finally { try { g.flush(); } catch (Throwable ignored) {} } }
    /** our font for every text of Minecraft (theme "font for all text"): minecraft:default -> asteria_visuals:f<N>s<scale> */
    private static final Identifier[][] ALL = new Identifier[16][7];
    public static Identifier allFont(Identifier id) {
        if (!V.allFont || V.font <= 0 || id == null || !"minecraft".equals(id.getNamespace()) || !("default".equals(id.getPath()) || "uniform".equals(id.getPath()))) return id; // the main and the "uniform" font: menus, chat, mods
        int i = Math.min(15, V.font), k = Math.max(1, Math.min(6, (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor())));
        if (ALL[i][k] == null) ALL[i][k] = new Identifier("asteria_visuals", "f" + i + "s" + k); return ALL[i][k];
    }
    public static int category(Object stack) { return Game116.I.category(stack); }
}
