package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Menu;

/** lists in our menus (servers, worlds) sit on a light see-through shade instead of the dark tile */
@Mixin(EntryListWidget.class)
public class MenuListBgMixin {
    @Inject(method = "drawMenuListBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void asteria$listBg(DrawContext ctx, CallbackInfo ci) {
        if (!Menu.on() || MinecraftClient.getInstance().world != null) return;
        EntryListWidget<?> w = (EntryListWidget<?>) (Object) this; ctx.fill(w.getX(), w.getY(), w.getRight(), w.getBottom(), 0x55050A12); ci.cancel();
    }
}
