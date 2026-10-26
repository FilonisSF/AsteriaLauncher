package ru.safimine.asteria.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.Star;
import ru.safimine.asteria.core.Asteria;

/** Asteria star in front of the nick of every AsteriaLauncher player in the tab list (like Lunar / Pulse). */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void asteria$icon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text name = cir.getReturnValue();
        if (name != null && Asteria.isLauncherPlayer(entry.getProfile().getName())) cir.setReturnValue(Star.prefix(name));
    }
}
