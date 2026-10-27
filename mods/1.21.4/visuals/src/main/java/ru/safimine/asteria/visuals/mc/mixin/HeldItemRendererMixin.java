package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.math.RotationAxis;
import ru.safimine.asteria.visuals.Fx;
import ru.safimine.asteria.visuals.V;

/** hand position and size (first person) */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER, ordinal = 0))
    private void asteria$swingPreset(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swing, ItemStack item, float equip, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (hand != Hand.MAIN_HAND) return; float[] p = Fx.swingPose(swing); if (p == null) return;
        float side = player.getMainArm() == Arm.RIGHT ? 1 : -1;
        m.translate(side * p[0], p[1], p[2]); m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p[3])); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * p[4])); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * p[5]));
    }
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER, ordinal = 0))
    private void asteria$viewmodel(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swing, ItemStack item, float equip, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!V.on("viewmodel")) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1 : -1, s = (float) V.num("viewmodel", "scale");
        m.translate(side * V.num("viewmodel", "x") * 0.5, V.num("viewmodel", "y") * 0.5, -V.num("viewmodel", "z") * 0.6);
        m.scale(s, s, s);
    }
}
