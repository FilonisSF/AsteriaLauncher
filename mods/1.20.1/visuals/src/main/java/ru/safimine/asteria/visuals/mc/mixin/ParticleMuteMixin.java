package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;

/** the game's own particles that one of our functions replaces (hits, totem) are not spawned: only ours are seen */
@Mixin(ParticleManager.class)
public class ParticleMuteMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$mute(ParticleEffect p, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> ci) {
        try { if (Fx.muteParticle(String.valueOf(net.minecraft.registry.Registries.PARTICLE_TYPE.getId(p.getType())))) ci.setReturnValue(null); } catch (Throwable ignored) {}
    }
}
