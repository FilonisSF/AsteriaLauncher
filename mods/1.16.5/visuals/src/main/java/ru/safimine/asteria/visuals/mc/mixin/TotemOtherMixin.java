package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;

/** status 35 = a totem of undying was used: for another player that is the "enemy lost a totem" alert */
@Mixin(LivingEntity.class)
public abstract class TotemOtherMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void asteria$otherTotem(byte status, CallbackInfo ci) {
        if (status != 35) return; Object self = this;
        try { if (self instanceof PlayerEntity && self != MinecraftClient.getInstance().player) Fx.otherTotem(((PlayerEntity) self).getName().getString()); } catch (Throwable ignored) {}
    }
}
