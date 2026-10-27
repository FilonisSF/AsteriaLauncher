package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** number of boss bars on screen (the island moves below them) */
@Mixin(BossBarHud.class)
public interface BossBarAccessor {
    @Accessor("bossBars") Map<?, ?> asteria$bars();
}
