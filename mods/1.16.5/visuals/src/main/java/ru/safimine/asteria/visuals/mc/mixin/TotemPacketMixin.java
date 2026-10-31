package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;

/** "the enemy lost a totem": status 35 is handled by the network handler itself and never reaches the entity, so it is caught here (on the game thread) */
@Mixin(ClientPlayNetworkHandler.class)
public class TotemPacketMixin {
    @Inject(method = "onEntityStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread", shift = At.Shift.AFTER), require = 0)
    private void asteria$totemPacket(EntityStatusS2CPacket packet, CallbackInfo ci) {
        try { if (packet.getStatus() != 35) return; MinecraftClient mc = MinecraftClient.getInstance(); if (mc.world == null) return;
            Entity e = packet.getEntity(mc.world); if (e == mc.player) Fx.selfTotem(); else if (e instanceof PlayerEntity) Fx.otherTotem(e.getName().getString()); } catch (Throwable ignored) {}
    }
}
