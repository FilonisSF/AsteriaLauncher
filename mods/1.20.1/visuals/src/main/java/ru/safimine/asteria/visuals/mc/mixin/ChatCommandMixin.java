package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;

/** ".метка X Y Z Название" in chat adds a marker instead of being sent */
@Mixin(ClientPlayNetworkHandler.class)
public class ChatCommandMixin {
    @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$chat(String msg, CallbackInfo ci) { try { if (ru.safimine.asteria.visuals.mc.VisualsClient.chat(msg)) ci.cancel(); } catch (Throwable ignored) {} }
}
