package ru.safimine.asteria.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.safimine.asteria.Star;
import ru.safimine.asteria.core.Asteria;

/** the star also shows in the name tag above the head of launcher players */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), argsOnly = true)
    private Text asteria$label(Text text, Entity entity, Text original, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        return entity instanceof PlayerEntity && Asteria.isLauncherPlayer(((PlayerEntity) entity).getGameProfile().getName()) ? Star.prefix(text) : text;
    }
}
