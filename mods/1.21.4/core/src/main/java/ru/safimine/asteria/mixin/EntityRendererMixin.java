package ru.safimine.asteria.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.Star;
import ru.safimine.asteria.core.Asteria;

/** the star also shows in the name tag above the head of launcher players */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void asteria$label(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state.displayName != null && entity instanceof PlayerEntity && Asteria.isLauncherPlayer(((PlayerEntity) entity).getGameProfile().getName()))
            state.displayName = Star.prefix(state.displayName);
    }
}
