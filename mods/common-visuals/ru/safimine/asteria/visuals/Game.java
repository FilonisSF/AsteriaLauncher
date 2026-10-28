package ru.safimine.asteria.visuals;

import java.util.List;

/** What the visuals need to know about the running game. Each Minecraft version implements it. Entities are passed as Object. */
public interface Game {
    int width();
    int height();
    boolean hudHidden();
    boolean screenOpen();
    /** 0 forward, 1 left, 2 back, 3 right, 4 jump, 5 sneak, 6 attack (LMB), 7 use (RMB) */
    boolean key(int i);
    String keyName(int i);
    /** is a keyboard key (GLFW code) or mouse button (-1..-8) down */
    boolean rawKey(int code);
    String keyLabel(int code);
    String username();
    int ping();
    double x();
    double y();
    double z();
    float yaw();
    String server();
    List<Item> armor();
    List<Effect> effects();
    /** attack cooldown progress 0..1 (1 = ready) */
    float cooldown();

    // ---- entities (for world visuals and the target HUD)
    Object self();
    Object crosshairEntity();
    boolean living(Object e);
    boolean alive(Object e);
    /** interpolated position (feet) */
    double[] pos(Object e, float partial);
    float height(Object e);
    float width(Object e);
    String name(Object e);
    float hp(Object e);
    float maxHp(Object e);
    /** skin texture id for a player, null for mobs */
    Object skin(Object e);
    boolean onGround();
    double motionY();
    boolean thirdPerson();
    float bodyYaw(float partial);

    /** plays one of our sounds (asteria_visuals:<id>, see sounds.json) */
    default void sound(String id, float volume, float pitch) {}
    /** 36 slots: 0-8 hotbar, 9-35 inventory; empty slots are null */
    default Object[] inventory() { return new Object[0]; }
    default Object offhand() { return null; }
    default int selectedSlot() { return 0; }
    default int count(Object stack) { return 0; }
    /** registry id, e.g. minecraft:totem_of_undying */
    default String itemId(Object stack) { return ""; }
    /** item cooldown left 0..1 (ender pearl, chorus, shield...) */
    default float itemCooldown(Object stack) { return 0; }

    /** outline box (minX,minY,minZ,maxX,maxY,maxZ) of the block under the crosshair, or null */
    default double[] targetBlock() { return null; }
    default boolean sprinting() { return false; }
    /** the player list (Tab) is held open: our HUD steps aside */
    default boolean tabOpen() { return false; }
    /** a mob's spawn egg (its icon in the target HUD), null for players / no egg */
    default Object entityIcon(Object e) { return null; }
    /** armor an entity wears, helmet first, with enchantments */
    default List<Item> armorOf(Object e) { return java.util.Collections.emptyList(); }
    /** what an entity holds in its main hand (null if nothing) */
    default Item heldBy(Object e) { return null; }
    /** other players in the world (not you) */
    default java.util.List<Object> players() { return java.util.Collections.emptyList(); }
    /** the account name of a player entity (no prefixes / team colours) */
    default String playerName(Object e) { return ""; }
    /** where an entity looks (degrees) */
    default float yawOf(Object e) { return 0; }
    /** head pitch (down = +), body yaw, head yaw and the sneaking pose of any player */
    default float pitchOf(Object e) { return 0; }
    default float bodyYawOf(Object e) { return yawOf(e); }
    default float headYawOf(Object e) { return yawOf(e); }
    default boolean sneakingOf(Object e) { return false; }
    /** the same, smoothed between ticks */
    default float headYawOf(Object e, float pt) { return headYawOf(e); }
    default float bodyYawOf(Object e, float pt) { return bodyYawOf(e); }
    default float pitchOf(Object e, float pt) { return pitchOf(e); }
    default boolean onGroundOf(Object e) { return true; }
    /** F3+B: entity hitboxes are shown */
    default boolean hitboxDebug() { return false; }
    /** chat is open (the HUD stays visible there, unlike under menus) */
    default boolean chatOpen() { return false; }
    /** a totem of undying stack, for the totem counter icon */
    default Object totemStack() { return null; }
    /** how many boss bars are shown at the top */
    default int bossBars() { return 0; }

    /** what an entity holds in its off hand (null if nothing) */
    default Item offBy(Object e) { return null; }
    /** sends a chat message as the player */
    default void chat(String text) {}
    /** current server: {name, address, players online} or null in single player */
    default String[] serverInfo() { return null; }
    /** the server's icon saved as a png (null if it has none) */
    default java.io.File serverIcon() { return null; }
    /** is the entity a player */
    default boolean isPlayer(Object e) { return skin(e) != null; }
    /** game perspective: 0 first person, 1 third person back, 2 front */
    default int perspective() { return thirdPerson() ? 1 : 0; }
    default void setPerspective(int p) {}
    /** the block the player looks at up to dist blocks away: {x, y, z} of its top centre, or null */
    default double[] lookAt(double dist) { return null; }
    /** light at a point, 0..1 (block light or sky light, night included) */
    default float light(double x, double y, double z) { return 1; }
    /** food level 0..20 (-1 unknown) */
    default int food() { return -1; }
    /** the world (dimension) id, e.g. minecraft:the_nether */
    default String dimension() { return ""; }
    /** runs a command ("/spawn" or "spawn") */
    default void command(String cmd) {}
    /** living entities around (not you), for hitboxes */
    default java.util.List<Object> living(double range) { return java.util.Collections.emptyList(); }
    /** what the player aims with: {kind (0 none, 1 bow, 2 crossbow, 3 pearl/snowball/egg, 4 trident, 5 potion), power 0..1}, eye x y z, yaw, pitch */
    default double[] aim() { return null; }
    /** is there a solid block at this point */
    default boolean solid(double x, double y, double z) { return false; }
    /** category of an item: 0 weapon, 1 tool, 2 block, 3 food, 4 other */
    default int category(Object stack) { return 4; }
    /** opens / closes the Asteria menu screen */
    void openMenu();
    void closeMenu();

    final class Item {
        public final Object stack; public final int damage, max; public final String name;
        /** enchantments as shown in the tooltip, e.g. "Защита IV" */
        public final List<String> ench;
        public Item(Object stack, int damage, int max, String name) { this(stack, damage, max, name, java.util.Collections.emptyList()); }
        public Item(Object stack, int damage, int max, String name, List<String> ench) { this.stack = stack; this.damage = damage; this.max = max; this.name = name; this.ench = ench; }
    }
    final class Effect {
        public final Object type; public final String name; public final int amp, ticks, color;
        public Effect(Object type, String name, int amp, int ticks, int color) { this.type = type; this.name = name; this.amp = amp; this.ticks = ticks; this.color = color; }
    }
}
