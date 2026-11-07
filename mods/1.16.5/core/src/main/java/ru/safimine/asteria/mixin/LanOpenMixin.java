package ru.safimine.asteria.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** a world opened to friends ("Open to LAN", reached through the launcher's relay) lets in accounts without a licence too:
 *  the game would check every joiner with Mojang and turn away the offline accounts most friends play with */
@Mixin(IntegratedServer.class)
public class LanOpenMixin {
    @Inject(method = "openToLan", at = @At("HEAD"), require = 0)
    private void asteria$offlineFriends(CallbackInfoReturnable<Boolean> cir) { ((MinecraftServer) (Object) this).setOnlineMode(false); }
}
