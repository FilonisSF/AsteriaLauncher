package ru.safimine.asteria.visuals.mc;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
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
    public static final KeyBinding FREELOOK = new KeyBinding("key.asteria.freelook", InputUtil.Type.KEYSYM, 342, "category.asteria");
    public static final KeyBinding MARK = new KeyBinding("key.asteria.mark", InputUtil.Type.KEYSYM, 66, "category.asteria");
    public static final KeyBinding M_TOGGLE = new KeyBinding("key.asteria.music_toggle", InputUtil.Type.KEYSYM, -1, "category.asteria"), M_NEXT = new KeyBinding("key.asteria.music_next", InputUtil.Type.KEYSYM, -1, "category.asteria"),
        M_PREV = new KeyBinding("key.asteria.music_prev", InputUtil.Type.KEYSYM, -1, "category.asteria"), M_UP = new KeyBinding("key.asteria.music_up", InputUtil.Type.KEYSYM, -1, "category.asteria"), M_DOWN = new KeyBinding("key.asteria.music_down", InputUtil.Type.KEYSYM, -1, "category.asteria");
    public static final KeyBinding[] KEYS = {MENU, ZOOM, FREELOOK, MARK, M_TOGGLE, M_NEXT, M_PREV, M_UP, M_DOWN};
    private static final Map<String, Boolean> HELD = new HashMap<>();
    private static int appliedHit = 0;

    @Override public void onInitializeClient() { V.init(MinecraftClient.getInstance().getSession().getUsername()); ru.safimine.asteria.visuals.Menu.tr = k -> net.minecraft.client.resource.language.I18n.translate(k); }

    public static void hud(DrawContext ctx) { Gfx121 g = new Gfx121(ctx, true); try { Hud.render(g, Game121.I); } catch (Throwable t) { Fx.err("hud", t); } finally { try { g.flush(); } catch (Throwable ignored) {} } }
    public static void world(Matrix4f pos, Camera cam, float pt) { try { Fx.render(new R3_121(pos, cam, pt), Game121.I); } catch (Throwable t) { Fx.err("world", t); } }

    private static final boolean TEST_SWING = System.getProperty("asteria.test.swing") != null;
    public static void tick(MinecraftClient c) {
        try {
            V.tick();
            if (TEST_SHOT != null && c.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen && ++shotTick == 100) testShot(c); // test runs only (-Dasteria.test.shot=<png>)
            while (MENU.wasPressed()) if (c.currentScreen == null) c.setScreen(new AsteriaScreen());
            Fx.zoomKey(ZOOM.isPressed() && c.currentScreen == null);
            if (c.player != null && V.on("autosprint") && c.options.forwardKey.isPressed() && !c.player.isSneaking() && c.player.getHungerManager().getFoodLevel() > 6 && !c.player.isUsingItem()) c.player.setSprinting(true);
            Fx.commandBinds(Game121.I, c.currentScreen != null);
            if (c.player != null) Fx.autoClick(Game121.I, c.currentScreen != null);
            while (M_TOGGLE.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("toggle"); while (M_NEXT.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("next"); while (M_PREV.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("prev");
            while (M_UP.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("volup"); while (M_DOWN.wasPressed()) ru.safimine.asteria.visuals.Media.cmd("voldown");
            while (MARK.wasPressed()) if (c.player != null && c.currentScreen == null) Fx.markKey(Game121.I, net.minecraft.client.gui.screen.Screen.hasShiftDown());
            if (c.player != null) Fx.freeLookKey(Game121.I, FREELOOK.isPressed() && c.currentScreen == null, c.player.getYaw(), c.player.getPitch());
            if (c.player != null) Fx.tick(Game121.I);
            if (TEST_SWING && c.player != null) { if (c.player.getMainHandStack().isEmpty()) c.player.getInventory().setStack(c.player.getInventory().selectedSlot, new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "diamond_sword")))); if (c.player.age % 40 == 0) c.player.swingHand(net.minecraft.util.Hand.MAIN_HAND); } // test runs only (-Dasteria.test.swing)
            long h = c.getWindow().getHandle();
            for (V.Mod m : V.MODS) {
                if (m.bind == 0) continue;
                boolean down = c.currentScreen == null && Game121.I.rawKey(m.bind);
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
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColorArgb(x, y, (0xB2 << 24) | (r << 16) | (g << 8) | b);
        tex.upload();
    }
    /** chat messages of ours (.метка ...) stay in the client */
    public static boolean chat(String msg) { return Fx.chatCommand(Game121.I, msg); }
    /** our font for every text of Minecraft (theme "font for all text"): minecraft:default -> asteria_visuals:f<N>s<scale> */
    private static final Identifier[][] ALL = new Identifier[32][7];
    public static Identifier allFont(Identifier id) {
        if (!V.allFont || V.font <= 0 || id == null || !"minecraft".equals(id.getNamespace()) || !("default".equals(id.getPath()) || "uniform".equals(id.getPath()))) return id; // the main and the "uniform" font: menus, chat, mods
        int i = Math.min(31, V.font), k = Math.max(1, Math.min(6, (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor())));
        if (ALL[i][k] == null) ALL[i][k] = Identifier.of("asteria_visuals", "f" + i + "s" + k); return ALL[i][k];
    }
    public static int category(Object stack) { return Game121.I.category(stack); }
    /** the container screens drawn in the Asteria style (InventoryStyleMixin, ContainerStyleMixin): their titles are made light */
    public static boolean styledScreen(Object s) { return ru.safimine.asteria.visuals.Hud.styledInventory() && (s instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen || s instanceof net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen
        || s instanceof net.minecraft.client.gui.screen.ingame.HopperScreen || s instanceof net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen || s instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen); }
    /** a screen title in a finer copy of our font (asteria_visuals:f<N>h<k>, oversample k / 2): it is drawn x times bigger than the chat */
    public static net.minecraft.text.Text titleText(net.minecraft.text.Text t, int x) {
        if (!V.allFont || V.font <= 0) return t; int i = Math.min(31, V.font), k = Math.max(2, Math.min(12, (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor() * 2 * x)));
        Identifier id = Identifier.of("asteria_visuals", "f" + i + "h" + k); return net.minecraft.text.Text.empty().append(t).setStyle(net.minecraft.text.Style.EMPTY.withFont(id));
    }
    private static final ClassValue<Boolean> TTF = new ClassValue<Boolean>() { @Override protected Boolean computeValue(Class<?> c) { return c.getEnclosingClass() == net.minecraft.client.font.TrueTypeFont.class; } };
    /** the shadow of our smooth (TrueType) letters: half a font pixel instead of a whole one */
    public static float ttfShadow(Object glyph, float o) { return V.allFont && V.font > 0 && glyph != null && TTF.get(glyph.getClass()) ? o * 0.5f : o; }
    /** a glyph atlas of our fonts (asteria_visuals:f<N>s<k>/<page>, f<N>h<k>/<page>): drawn with smooth filtering */
    public static boolean smoothFont(Identifier id) { String p = id.getPath(); return "asteria_visuals".equals(id.getNamespace()) && p.length() > 1 && p.charAt(0) == 'f' && Character.isDigit(p.charAt(1)); }
    private static final String TEST_SHOT = System.getProperty("asteria.test.shot"); private static int shotTick;
    /** test runs only: the game's picture (main framebuffer) to a PNG, to look at fonts and the HUD without a person at the screen */
    private static void testShot(MinecraftClient c) {
        try { net.minecraft.client.gl.Framebuffer fb = c.getFramebuffer(); int w = fb.textureWidth, h = fb.textureHeight; java.nio.ByteBuffer b = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, fb.getColorAttachment()); org.lwjgl.opengl.GL11.glGetTexImage(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, b);
            java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { int i = ((h - 1 - y) * w + x) * 4; im.setRGB(x, y, (b.get(i) & 255) << 16 | (b.get(i + 1) & 255) << 8 | (b.get(i + 2) & 255)); }
            javax.imageio.ImageIO.write(im, "png", new java.io.File(TEST_SHOT)); System.out.println("[Asteria test] shot " + TEST_SHOT); } catch (Throwable t) { t.printStackTrace(); }
    }
}
