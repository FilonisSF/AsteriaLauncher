package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import ru.safimine.asteria.visuals.mc.Gfx121;

/** Asteria buttons everywhere in Minecraft's menus (plain and cycling buttons; icon buttons keep their own look) */
@Mixin(PressableWidget.class)
public abstract class ButtonStyleMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$button(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!Menu.on()) return;
        ClickableWidget w = (ClickableWidget) (Object) this;
        try { Menu.button(new Gfx121(ctx), w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.isSelected(), w.active, w.getMessage().getString()); ci.cancel(); } catch (Throwable ignored) {}
    }
}
