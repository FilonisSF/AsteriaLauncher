package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.safimine.asteria.visuals.V;

/** full brightness without touching your saved brightness setting */
@Mixin(LightmapTextureManager.class)
public class LightmapMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"), require = 0)
    private Object asteria$gamma(SimpleOption<?> o) {
        Object v = o.getValue();
        return V.on("fullbright") && o == MinecraftClient.getInstance().options.getGamma() ? (Object) 16.0 : v;
    }
}
