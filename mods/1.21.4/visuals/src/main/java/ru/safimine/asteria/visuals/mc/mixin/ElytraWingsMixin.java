package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** "Wings instead of elytra": your own vanilla elytra are not drawn (our wings are) */
@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraWingsMixin {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$wings(MatrixStack m, VertexConsumerProvider v, int light, EntityRenderState st, float a, float b, CallbackInfo ci) {
        if (!V.on("elytrawings")) return; MinecraftClient c = MinecraftClient.getInstance();
        if (st instanceof PlayerEntityRenderState && c.player != null && ((PlayerEntityRenderState) st).id == c.player.getId()) ci.cancel();
    }
}
