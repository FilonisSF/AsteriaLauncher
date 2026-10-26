package ru.safimine.asteria;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** only touched when PlaceholderAPI is installed */
final class Papi {
    private Papi() {}
    static String set(Player p, String s) { return PlaceholderAPI.setPlaceholders(p, s); }
}
