package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;

/** our hit sounds replace Minecraft's attack sounds (strong, sweep, crit, knockback, weak, nodamage) */
@Mixin(SoundManager.class)
public class SoundMuteMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$mute(SoundInstance s, CallbackInfo ci) { try { if (Fx.muteVanilla(String.valueOf(s.getId()))) ci.cancel(); } catch (Throwable ignored) {} }
}
