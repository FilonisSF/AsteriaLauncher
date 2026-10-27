package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** "Block highlight" draws its own outline (Fx), so the vanilla black one is skipped */
@Mixin(WorldRenderer.class)
public class BlockOutlineMixin {
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$outline(CallbackInfo ci) { if (V.on("blockhighlight")) ci.cancel(); }
}
