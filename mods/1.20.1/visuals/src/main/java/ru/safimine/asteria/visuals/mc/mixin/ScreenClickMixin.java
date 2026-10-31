package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** a click on "⌖ метка" in the chat: the marker is placed at once (without our mod the text would just go into the chat box) */
@Mixin(Screen.class)
public class ScreenClickMixin {
    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$mark(Style style, CallbackInfoReturnable<Boolean> cir) {
        try { ClickEvent e = style == null ? null : style.getClickEvent(); if (e == null || e.getAction() != ClickEvent.Action.SUGGEST_COMMAND || !e.getValue().startsWith("asteria:mark ")) return;
            VisualsClient.chat(".mark " + e.getValue().substring(13) + " Из чата"); cir.setReturnValue(true); } catch (Throwable ignored) {}
    }
}
