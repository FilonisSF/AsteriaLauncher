package ru.safimine.asteria.visuals;

/**
 * Sounds of the alerts and of switched functions. "Предупреждения → Озвучка" picks the voice (functions on / off, totems,
 * and a spoken phrase for every warning) or one of the plain signals.
 */
public final class Voice {
    private Voice() {}
    private static final String[] SFX = {null, "bell", "blip", "pop", "glass", "magic_pok", null};
    /** the spoken phrases of the warnings (a female voice) */
    private static final java.util.Map<String, String> VOICED = new java.util.HashMap<>();
    static { VOICED.put("hp", "wv_hp"); VOICED.put("armor", "wv_armor"); VOICED.put("food", "wv_food"); VOICED.put("effect", "wv_effect");
        VOICED.put("enemy_armor", "wv_enemy_armor"); VOICED.put("shield", "wv_shield"); VOICED.put("friend", "wv_friend"); }
    private static float vol() { return (float) V.num("alerts", "volume"); }
    private static boolean voiced() { return V.mode("alerts", "sfx") == 0; }
    /** a module was switched: the girl says "on" / "off", or the click of the sound pack */
    public static void toggled(V.Mod m) {
        if (V.on("alerts") && voiced() && V.bool("alerts", "modvoice")) { Fx.sound(m.on ? "wv_enable" : "wv_disable", 0.6f * vol() / 0.85f); return; }
        if (V.on("sounds")) Fx.sound(m.on ? "wv_enable" : "wv_disable", 0.6f);
    }
    /** an alert: a spoken phrase where the voice has one, otherwise the chosen signal */
    public static void alert(String key) {
        int k = V.mode("alerts", "sfx"); if (k >= SFX.length - 1) return; float v = vol();
        if (k == 0) {
            if (key.equals("enemy_totem")) { Fx.sound("wv_tother" + (1 + (int) (Math.random() * 4)), v); return; } // "the enemy has lost a totem"
            if (key.equals("totem")) { Fx.sound("wv_totem" + (1 + (int) (Math.random() * 2)), v); return; }
            String said = VOICED.get(key); Fx.sound(said != null ? said : "bell", said != null ? v : 0.65f * v); return; } // every warning has its own spoken phrase now
        Fx.sound(SFX[k], 0.65f * v);
    }
}
