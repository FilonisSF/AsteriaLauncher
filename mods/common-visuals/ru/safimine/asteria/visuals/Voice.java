package ru.safimine.asteria.visuals;

import java.io.File;

/**
 * The announcer: short spoken phrases made by the launcher with a Windows voice the player picked (config/asteria-voice/<id>.wav).
 * ids: on_<module>, off_<module>, alert_hp, alert_armor, alert_food, alert_totem, alert_effect, alert_enemy_totem, alert_enemy_armor.
 * Played with Java sound (not the game's sound system), one phrase at a time; false when there is no such phrase.
 */
public final class Voice {
    private Voice() {}
    private static volatile Thread playing; private static volatile javax.sound.sampled.Clip clip;
    public static boolean has(String id) { return V.on("voice") && file(id).exists(); }
    private static File file(String id) { return new File(new File(V.dir(), "asteria-voice"), id + ".wav"); }
    public static boolean say(String id) {
        if (!V.on("voice")) return false; File f = file(id); if (!f.exists()) return false;
        final float vol = (float) V.num("voice", "volume");
        Thread t = new Thread(() -> {
            try { javax.sound.sampled.Clip old = clip; if (old != null) { old.stop(); old.close(); } // the newest phrase wins
                javax.sound.sampled.AudioInputStream in = javax.sound.sampled.AudioSystem.getAudioInputStream(f); javax.sound.sampled.Clip c = javax.sound.sampled.AudioSystem.getClip(); c.open(in); clip = c;
                try { javax.sound.sampled.FloatControl g = (javax.sound.sampled.FloatControl) c.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN); g.setValue(Math.max(g.getMinimum(), (float) (20 * Math.log10(Math.max(0.01, vol))))); } catch (Exception ignored) {}
                c.start(); Thread.sleep(Math.min(8000, c.getMicrosecondLength() / 1000 + 100)); c.close(); if (clip == c) clip = null;
            } catch (Throwable ignored) {}
        }, "asteria-voice"); t.setDaemon(true); playing = t; t.start(); return true;
    }
    /** a module was switched: its name is said ("Траектория включена"), or the short voice of the old sound pack */
    public static void toggled(V.Mod m) {
        if (V.on("voice") && V.bool("voice", "modules") && say((m.on ? "on_" : "off_") + m.id)) return;
        if (V.on("sounds") || V.on("voice")) Fx.sound(m.on ? "wv_enable" : "wv_disable", 0.6f);
    }
    /** an alert: its phrase, or a soft signal (never the "module off" voice) */
    public static void alert(String key) {
        if (V.on("voice") && V.bool("voice", "alerts") && say("alert_" + key)) return;
        if (key.equals("enemy_totem")) { Fx.sound("wv_tother" + (1 + (int) (Math.random() * 4)), 0.8f); return; } // "the enemy has lost a totem" in the voice of the sound pack
        Fx.sound("bell", 0.55f);
    }
}
