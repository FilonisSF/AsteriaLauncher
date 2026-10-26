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
