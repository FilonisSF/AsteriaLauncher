package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.mc.VisualsClient;

import java.util.Arrays;

/** adds the Asteria keys (menu, zoom) to Options → Controls, before options.txt is read so saved binds apply */
@Mixin(GameOptions.class)
public class KeysMixin {
    @Mutable @Final @Shadow public KeyBinding[] allKeys;

    @Inject(method = "load", at = @At("HEAD"))
    private void asteria$keys(CallbackInfo ci) {
        for (KeyBinding k : VisualsClient.KEYS) {
            if (Arrays.asList(allKeys).contains(k)) continue;
            KeyBindingAccessor.asteria$order().putIfAbsent(k.getCategory(), 100);
            allKeys = Arrays.copyOf(allKeys, allKeys.length + 1);
            allKeys[allKeys.length - 1] = k;
        }
    }
}
