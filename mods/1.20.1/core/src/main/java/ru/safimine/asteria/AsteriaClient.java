package ru.safimine.asteria;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import ru.safimine.asteria.core.Asteria;

import java.io.ByteArrayInputStream;

/** AsteriaLauncher mod for Minecraft 1.20.1 (free): launcher star in the tab list and above heads, own skin/cape, skins/capes of
 *  other launcher players, settings changed in the launcher applied while the game runs, current server for Discord. */
public class AsteriaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Asteria.start(new Asteria.Platform() {
            @Override public void onClient(Runnable r) {
                MinecraftClient c = MinecraftClient.getInstance();
                if (c == null) throw new IllegalStateException("client not ready");
                c.execute(r);
            }
            @Override public Object registerTexture(String path, byte[] png) throws Exception {
                Identifier id = new Identifier("asteria", path);
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(png))));
                return id;
            }
            @Override public void reloadOptions() { reload(); }
            @Override public String status() {
                MinecraftClient c = MinecraftClient.getInstance();
                if (c == null || c.world == null) return "menu";
                if (c.isInSingleplayer()) return "sp";
                ServerInfo s = c.getCurrentServerEntry();
                if (s != null && s.address != null && !s.address.isEmpty()) return "mp:" + s.address;
                try { // joined with --server (launcher quick join): no server entry, take the address of the connection
                    java.net.SocketAddress sa = c.getNetworkHandler().getConnection().getAddress();
                    if (sa instanceof java.net.InetSocketAddress) { java.net.InetSocketAddress ia = (java.net.InetSocketAddress) sa; return "mp:" + ia.getHostString() + (ia.getPort() == 25565 ? "" : ":" + ia.getPort()); }
                } catch (Throwable ignored) {}
                return "mp:";
            }
        });
    }

    /** re-reads options.txt written by the launcher and applies it without restarting the game */
    static void reload() {
        MinecraftClient c = MinecraftClient.getInstance();
        GameOptions o = c.options;
        int view = o.getViewDistance().getValue(), blend = o.getBiomeBlendRadius().getValue(), mip = o.getMipmapLevels().getValue();
        Object gfx = o.getGraphicsMode().getValue(); boolean ao = o.getAo().getValue();
        o.load();
        c.getWindow().setFramerateLimit(o.getMaxFps().getValue());
        c.getWindow().setVsync(o.getEnableVsync().getValue());
        if (c.getWindow().isFullscreen() != o.getFullscreen().getValue()) c.getWindow().toggleFullscreen();
        KeyBinding.updateKeysByCode();
        for (SoundCategory cat : SoundCategory.values()) c.getSoundManager().updateSoundVolume(cat, o.getSoundVolume(cat));
        c.onResolutionChanged();
        if (mip != o.getMipmapLevels().getValue()) { c.setMipmapLevels(o.getMipmapLevels().getValue()); c.reloadResourcesConcurrently(); }
        if (c.worldRenderer != null && (view != o.getViewDistance().getValue() || blend != o.getBiomeBlendRadius().getValue() || gfx != o.getGraphicsMode().getValue() || ao != o.getAo().getValue())) c.worldRenderer.reload();
    }
}
