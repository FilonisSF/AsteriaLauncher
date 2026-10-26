package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.safimine.asteria.visuals.V;

/** full brightness without touching your saved brightness setting */
@Mixin(LightmapTextureManager.class)
public class LightmapMixin {
    @Redirect(method = "update", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;gamma:D", opcode = 180), require = 0)
    private double asteria$gamma(GameOptions o) { return V.on("fullbright") ? 16.0 : o.gamma; }
}
