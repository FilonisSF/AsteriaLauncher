package ru.safimine.asteria;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/** %asteria_icon% (star or empty) and %asteria_launcher% (true/false) for TAB, chat and nametag plugins. */
final class AsteriaExpansion extends PlaceholderExpansion {
    private final AsteriaBridge plugin;
    AsteriaExpansion(AsteriaBridge plugin) { this.plugin = plugin; }
    @Override public String getIdentifier() { return "asteria"; }
    @Override public String getAuthor() { return "SafiMine"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onPlaceholderRequest(Player p, String id) {
        if (p == null) return "";
        if (id.equals("icon")) return AsteriaBridge.isLauncher(p) ? plugin.prefix() : "";
        if (id.equals("launcher")) return String.valueOf(AsteriaBridge.isLauncher(p));
        return null;
    }
}
