package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import ru.safimine.asteria.visuals.mc.Gfx116;

/** Asteria buttons everywhere in Minecraft's menus (plain buttons; sliders, text fields and icon buttons stay vanilla) */
@Mixin(ClickableWidget.class)
public abstract class ButtonStyleMixin {
    @Inject(method = "renderButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$button(MatrixStack m, int mx, int my, float delta, CallbackInfo ci) {
        Object self = this; if (!(self instanceof ButtonWidget)) return;
        ClickableWidget w = (ClickableWidget) self; if (!Menu.styled(w.getWidth())) return;
        try { Menu.button(new Gfx116(m), w.x, w.y, w.getWidth(), w.getHeight(), w.isHovered(), w.active, w.getMessage().getString()); ci.cancel(); } catch (Throwable ignored) {}
    }
}
