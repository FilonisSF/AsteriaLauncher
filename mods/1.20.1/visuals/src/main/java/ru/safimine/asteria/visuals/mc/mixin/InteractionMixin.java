package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;
import ru.safimine.asteria.visuals.mc.Bridge;

/** remembers who you hit (target HUD / marker) and spawns hit particles */
@Mixin(ClientPlayerInteractionManager.class)
public class InteractionMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void asteria$attack(PlayerEntity player, Entity target, CallbackInfo ci) { Fx.onAttack(Bridge.game(), target); }
}
