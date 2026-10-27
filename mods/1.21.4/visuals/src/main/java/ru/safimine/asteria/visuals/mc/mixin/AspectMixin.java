package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

/** "Stretched screen": scales the projection horizontally */
@Mixin(GameRenderer.class)
public class AspectMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
    private void asteria$aspect(CallbackInfoReturnable<Matrix4f> cir) {
        float k = Fx.stretch(MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight()); if (k == 1) return;
        cir.setReturnValue(new Matrix4f().scaling(k, 1, 1).mul(cir.getReturnValue()));
    }
}
