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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.math.RotationAxis;
import ru.safimine.asteria.visuals.Fx;
import ru.safimine.asteria.visuals.V;

/** hand position and size (first person) */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    /** position: moves the whole hand (before Minecraft places it) */
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER, ordinal = 0))
    private void asteria$move(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swing, ItemStack item, float equip, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (hand == Hand.MAIN_HAND) { float[] p = Fx.swingPose(swing); if (p != null) { float side = player.getMainArm() == Arm.RIGHT ? 1 : -1; m.translate(side * p[0], p[1], p[2]); } }
        if (!V.on("viewmodel")) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1 : -1; float[] hd = Fx.hand(hand == Hand.MAIN_HAND ? 0 : 1);
        m.translate(side * hd[0] * 0.5, hd[1] * 0.5, -hd[2] * 0.6);
    }
    /** rotation, swing turns and size: around the item in the hand, so turning feels natural */
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"), require = 0)
    private void asteria$turn(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swing, ItemStack item, float equip, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (hand == Hand.MAIN_HAND) { float[] p = Fx.swingPose(swing); if (p != null) { float side = player.getMainArm() == Arm.RIGHT ? 1 : -1;
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p[3])); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * p[4])); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * p[5])); } }
        float mini = Fx.miniScale(ru.safimine.asteria.visuals.mc.VisualsClient.category(item)); if (mini != 1) m.scale(mini, mini, mini);
        if (!V.on("viewmodel")) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1 : -1; float[] hd = Fx.hand(hand == Hand.MAIN_HAND ? 0 : 1); float s = hd[6];
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(hd[3])); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * hd[4])); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * hd[5]));
        m.scale(s, s, s);
    }
    /** our swing styles replace the vanilla swing movement (otherwise it hides them) */
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private float asteria$vanillaSwing(float swing) { return Fx.vanillaSwing(swing); }
}
