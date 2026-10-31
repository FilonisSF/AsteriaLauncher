package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** the key a binding is set to: the auto-click presses it like a real click */
@Mixin(KeyBinding.class)
public interface BoundKeyAccessor {
    @Accessor("boundKey") InputUtil.Key asteria$bound();
}
