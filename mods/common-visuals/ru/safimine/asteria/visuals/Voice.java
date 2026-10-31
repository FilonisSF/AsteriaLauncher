package ru.safimine.asteria.visuals;

/**
 * Sounds of the alerts and of switched functions. "Предупреждения → Озвучка" picks the girl's voice from the sound pack
 * (functions on / off, totems) or one of the plain signals; warnings that have no spoken phrase get a soft bell in voice mode.
 */
public final class Voice {
    private Voice() {}
    private static final String[] SFX = {null, "bell", "blip", "pop", "glass", "magic_pok", null};
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
            Fx.sound("bell", 0.65f * v); return; }
        Fx.sound(SFX[k], 0.65f * v);
    }
}
