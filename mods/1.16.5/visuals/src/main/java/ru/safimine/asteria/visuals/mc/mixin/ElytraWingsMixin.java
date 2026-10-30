package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** "Wings instead of elytra": your own vanilla elytra are not drawn (our wings are) */
@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraWingsMixin {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$wings(MatrixStack m, VertexConsumerProvider v, int light, LivingEntity e, float a, float b, float c2, float d, float f, float g, CallbackInfo ci) {
        if (V.on("elytrawings") && e == MinecraftClient.getInstance().player) ci.cancel();
    }
}
