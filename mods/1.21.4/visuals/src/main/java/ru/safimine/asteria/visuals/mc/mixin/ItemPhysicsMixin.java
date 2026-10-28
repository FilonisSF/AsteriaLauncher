package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.V;

/** "Item physics" for 1.21.2+: dropped items lie flat on the ground and tumble while flying (the render state does not know "on ground", so it is kept here) */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemPhysicsMixin {
    @Shadow @Final private Random random;
    /** render state -> {on ground, entity id, block item, age + partial} */
    private static final java.util.Map<EntityRenderState, float[]> INFO = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V", at = @At("TAIL"), require = 0)
    private void asteria$info(Entity en, EntityRenderState s, float td, CallbackInfo ci) {
        if (!(en instanceof ItemEntity)) return; ItemEntity e = (ItemEntity) en;
        INFO.put(s, new float[]{e.isOnGround() ? 1 : 0, e.getId(), e.getStack().getItem() instanceof BlockItem ? 1 : 0, e.getItemAge() + td});
    }
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$physics(EntityRenderState st, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!V.on("itemphysics") || !(st instanceof ItemEntityRenderState)) return; float[] f = INFO.get(st); if (f == null) return;
        ItemEntityRenderState s = (ItemEntityRenderState) st; if (s.itemRenderState.isEmpty()) return;
        boolean block = f[2] > 0, air = f[0] == 0; float spin = air ? f[3] * 18 : 0;
        m.push(); m.translate(0, block ? 0.12 : 0.02, 0); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((int) f[1] * 47) % 360));
        if (!block) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90 + spin)); else if (air) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin));
        random.setSeed(s.seed); ItemEntityRenderer.renderStack(m, vcp, light, s, random);
        m.pop(); ci.cancel();
    }
}
