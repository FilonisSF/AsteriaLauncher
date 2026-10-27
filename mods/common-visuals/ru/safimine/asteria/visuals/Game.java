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
    /** other players in the world (not you) */
    default java.util.List<Object> players() { return java.util.Collections.emptyList(); }
    /** the account name of a player entity (no prefixes / team colours) */
    default String playerName(Object e) { return ""; }
    /** where an entity looks (degrees) */
    default float yawOf(Object e) { return 0; }
    /** chat is open (the HUD stays visible there, unlike under menus) */
    default boolean chatOpen() { return false; }
    /** a totem of undying stack, for the totem counter icon */
    default Object totemStack() { return null; }
    /** how many boss bars are shown at the top */
    default int bossBars() { return 0; }

    /** opens / closes the Asteria menu screen */
    void openMenu();
    void closeMenu();

    final class Item {
        public final Object stack; public final int damage, max; public final String name;
        public Item(Object stack, int damage, int max, String name) { this.stack = stack; this.damage = damage; this.max = max; this.name = name; }
    }
    final class Effect {
        public final Object type; public final String name; public final int amp, ticks, color;
        public Effect(Object type, String name, int amp, int ticks, int color) { this.type = type; this.name = name; this.amp = amp; this.ticks = ticks; this.color = color; }
    }
}
