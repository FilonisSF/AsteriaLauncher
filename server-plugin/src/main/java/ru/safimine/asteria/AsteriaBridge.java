package ru.safimine.asteria;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SafiMine <-> AsteriaLauncher.
 *  1. Star in front of the nick in the tab list for everyone who plays from the launcher (visible to ALL players).
 *  2. Pushes player stats (coins, sapphires, kills...) to the backend for the launcher's Profile tab.
 *  3. Pushes server event timers (airdrops, black auction... from PlaceholderAPI) for the launcher's «Ивенты» block.
 */
public class AsteriaBridge extends JavaPlugin {
    static volatile Set<String> launcher = Collections.emptySet();
    private boolean papi;

    @Override public void onEnable() {
        saveDefaultConfig();
        papi = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (papi) new AsteriaExpansion(this).register();
        // who plays from the launcher: refreshed every 10 s (async), applied on the main thread
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            Set<String> s = fetchOnline();
            if (s == null) return;
            launcher = s;
            Bukkit.getScheduler().runTask(this, this::applyTab);
        }, 40L, 200L);
        long ticks = Math.max(10, getConfig().getInt("interval-seconds", 60)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, this::pushStats, 100L, ticks);
        long evTicks = Math.max(5, getConfig().getInt("events-interval-seconds", 15)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, this::pushEvents, 120L, evTicks);
    }

    static boolean isLauncher(Player p) { return launcher.contains(p.getName().toLowerCase()); }
    String prefix() { return ChatColor.translateAlternateColorCodes('&', getConfig().getString("tab-icon.prefix", "&b✦ &r")); }

    private void applyTab() {
        if (!getConfig().getBoolean("tab-icon.enabled", true) || !getConfig().getBoolean("tab-icon.set-player-list-name", true)) return;
        String pre = prefix();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String cur = p.getPlayerListName();
            boolean has = cur != null && cur.startsWith(pre);
            if (isLauncher(p) && !has) p.setPlayerListName(pre + (cur == null ? p.getName() : cur));
            else if (!isLauncher(p) && has) p.setPlayerListName(cur.substring(pre.length()));
        }
    }

    private Set<String> fetchOnline() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(getConfig().getString("url") + "/online").openConnection();
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            if (c.getResponseCode() != 200) return null;
            String body;
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
                byte[] b = new byte[4096]; int r; while ((r = in.read(b)) > 0) o.write(b, 0, r);
                body = new String(o.toByteArray(), StandardCharsets.UTF_8);
            }
            Set<String> s = new HashSet<>();
            for (String n : body.replaceAll("[\\[\\]\" ]", "").split(",")) if (!n.isEmpty()) s.add(n.toLowerCase());
            return s;
        } catch (Exception e) { return null; }
    }

    private void pushStats() {
        ConfigurationSection f = getConfig().getConfigurationSection("fields");
        if (f == null) return;
        StringBuilder sb = new StringBuilder("{\"players\":[");
        boolean first = true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!first) sb.append(','); first = false;
            sb.append("{\"name\":").append(q(p.getName())).append(",\"stats\":{");
            boolean k = true;
            for (String key : f.getKeys(false)) {
                if (!k) sb.append(','); k = false;
                String raw = f.getString(key, "");
                sb.append(q(key)).append(':').append(q(papi ? Papi.set(p, raw) : raw));
            }
            sb.append("}}");
        }
        final String body = sb.append("]}").toString();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(getConfig().getString("url") + "/server/stats").openConnection();
                c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(5000); c.setReadTimeout(5000);
                c.setRequestProperty("content-type", "application/json"); c.setRequestProperty("x-key", getConfig().getString("secret", ""));
                try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes(StandardCharsets.UTF_8)); }
                c.getResponseCode();
            } catch (Exception e) { getLogger().warning("Asteria backend unreachable: " + e.getMessage()); }
        });
    }
    /** events: { id: { name: "%placeholder%", timer: "%placeholder%" } } -> POST /server/events */
    private void pushEvents() {
        ConfigurationSection ev = getConfig().getConfigurationSection("events");
        if (ev == null || !papi) return;
        Player any = null;
        for (Player p : Bukkit.getOnlinePlayers()) { any = p; break; } // some expansions need a player; timers are the same for everyone
        StringBuilder sb = new StringBuilder("{\"time\":").append(System.currentTimeMillis()).append(",\"events\":{");
        boolean first = true;
        for (String id : ev.getKeys(false)) {
            ConfigurationSection e = ev.getConfigurationSection(id);
            if (e == null) continue;
            if (!first) sb.append(','); first = false;
            sb.append(q(id)).append(":{");
            boolean k = true;
            for (String key : e.getKeys(false)) {
                String raw = e.getString(key, "");
                String val;
                try { val = Papi.set(any, raw); } catch (Exception x) { val = ""; }
                if (val.equals(raw) && raw.contains("%")) val = ""; // placeholder not available
                if (!k) sb.append(','); k = false;
                sb.append(q(key)).append(':').append(q(val));
            }
            sb.append('}');
        }
        final String body = sb.append("}}").toString();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> post("/server/events", body));
    }

    private void post(String path, String body) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(getConfig().getString("url") + path).openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(5000); c.setReadTimeout(5000);
            c.setRequestProperty("content-type", "application/json"); c.setRequestProperty("x-key", getConfig().getString("secret", ""));
            try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes(StandardCharsets.UTF_8)); }
            c.getResponseCode();
        } catch (Exception e) { getLogger().warning("Asteria backend unreachable: " + e.getMessage()); }
    }

    private static String q(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""; }
}
