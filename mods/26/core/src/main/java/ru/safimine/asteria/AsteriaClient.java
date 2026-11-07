package ru.safimine.asteria;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.sounds.SoundSource;
import ru.safimine.asteria.core.Asteria;

/** AsteriaLauncher mod for Minecraft 26.x (one jar for 26.1 - 26.3; the game ships with Mojang's own names, so nothing is remapped).
 *  No mixins: settings changed in the launcher are applied while the game runs, "join a friend" from the launcher, current
 *  server for Discord. Every step of applying settings is guarded on its own: a method renamed in a later 26.x skips only that step. */
public class AsteriaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Asteria.start(new Asteria.Platform() {
            @Override public void onClient(Runnable r) {
                Minecraft c = Minecraft.getInstance();
                if (c == null) throw new IllegalStateException("client not ready");
                c.execute(r);
            }
            @Override public Object registerTexture(String path, byte[] png) { throw new UnsupportedOperationException("no launcher skins on 26.x yet"); }
            @Override public void reloadOptions() { reload(); }
            @Override public void join(String host, int port) {
                Minecraft c = Minecraft.getInstance();
                if (c.level != null) step(() -> c.disconnectFromWorld(net.minecraft.network.chat.Component.empty()));
                String a = host + (port == 25565 ? "" : ":" + port);
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), c,
                        net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(a), new ServerData("Друг", a, ServerData.Type.OTHER), false, null);
            }
            @Override public String status() {
                Minecraft c = Minecraft.getInstance();
                if (c == null || c.level == null) return "menu";
                if (c.isSingleplayer()) return "sp";
                ServerData s = c.getCurrentServer();
                if (s != null && s.ip != null && !s.ip.isEmpty()) return "mp:" + s.ip;
                try {
                    java.net.SocketAddress sa = c.getConnection().getConnection().getRemoteAddress();
                    if (sa instanceof java.net.InetSocketAddress ia) return "mp:" + ia.getHostString() + (ia.getPort() == 25565 ? "" : ":" + ia.getPort());
                } catch (Throwable ignored) {}
                return "mp:";
            }
        });
    }

    private static void step(Runnable r) { try { r.run(); } catch (Throwable ignored) {} }

    /** re-reads options.txt written by the launcher and applies it without restarting the game */
    static void reload() {
        Minecraft c = Minecraft.getInstance();
        Options o = c.options;
        int[] before = new int[3]; boolean[] ao = new boolean[1];
        step(() -> { before[0] = o.renderDistance().get(); before[1] = o.biomeBlendRadius().get(); before[2] = o.mipmapLevels().get(); ao[0] = o.ambientOcclusion().get(); });
        o.load();
        step(() -> System.out.println("[Asteria] settings from the launcher applied: view " + o.renderDistance().get() + ", fov " + o.fov().get() + ", gui " + o.guiScale().get()));
        Window w = c.getWindow();
        step(() -> w.updateVsync(o.enableVsync().get()));
        step(() -> { if (w.isFullscreen() != o.fullscreen().get()) w.toggleFullScreen(); });
        step(KeyMapping::resetMapping);
        step(() -> { for (SoundSource s : SoundSource.values()) c.getSoundManager().refreshCategoryVolume(s); });
        step(c::resizeGui);
        step(() -> { if (before[2] != o.mipmapLevels().get()) { c.updateMaxMipLevel(o.mipmapLevels().get()); c.reloadResourcePacks(); } });
        step(() -> { if (c.levelRenderer != null && (before[0] != o.renderDistance().get() || before[1] != o.biomeBlendRadius().get() || ao[0] != o.ambientOcclusion().get())) c.levelRenderer.allChanged(); });
    }
}
