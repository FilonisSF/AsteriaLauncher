package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Fx;

/** zoom: mouse wheel changes zoom strength, mouse is less sensitive while zoomed */
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void asteria$scroll(long window, double h, double v, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null && v != 0 && Fx.scroll(v)) ci.cancel();
    }
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"), require = 0)
    private Object asteria$sens(net.minecraft.client.option.SimpleOption<?> o) { Object v = o.getValue(); return v instanceof Double && o == net.minecraft.client.MinecraftClient.getInstance().options.getMouseSensitivity() ? (Double) v * Fx.sensitivity() : v; }
}
