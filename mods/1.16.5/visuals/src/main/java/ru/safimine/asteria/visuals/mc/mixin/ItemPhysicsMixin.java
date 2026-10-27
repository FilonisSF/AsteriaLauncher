package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** "Item physics": dropped items lie flat on the ground and tumble while flying */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemPhysicsMixin {
    @Shadow @Final private ItemRenderer itemRenderer;
    @Inject(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$physics(ItemEntity e, float yaw, float td, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!V.on("itemphysics")) return; ItemStack st = e.getStack(); if (st.isEmpty()) return;
        boolean block = st.getItem() instanceof BlockItem, air = !e.isOnGround(); float spin = air ? (e.age + td) * 18 : 0;
        int n = st.getCount() > 32 ? 3 : st.getCount() > 1 ? 2 : 1;
        m.push(); m.translate(0, block ? 0.12 : 0.02, 0); m.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion((e.getEntityId() * 47) % 360));
        if (!block) m.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90 + spin)); else if (air) m.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(spin));
        for (int i = 0; i < n; i++) { m.push(); if (i > 0) m.translate(0.06 * i, 0.04 * i, block ? 0.05 * i : -0.02 * i); itemRenderer.renderItem(st, ModelTransformation.Mode.GROUND, light, OverlayTexture.DEFAULT_UV, m, vcp); m.pop(); }
        m.pop(); ci.cancel();
    }
}
