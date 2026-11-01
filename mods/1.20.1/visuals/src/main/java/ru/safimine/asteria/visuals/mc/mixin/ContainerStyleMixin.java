package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.mc.Gfx120;

/** "Свой хотбар и инвентарь": chests, ender chests, barrels, shulker boxes, hoppers and dispensers in the Asteria style (only slots on them, nothing else to cover) */
@Mixin({GenericContainerScreen.class, ShulkerBoxScreen.class, HopperScreen.class, Generic3x3ContainerScreen.class})
public abstract class ContainerStyleMixin extends HandledScreen<ScreenHandler> {
    private ContainerStyleMixin(ScreenHandler h, PlayerInventory i, Text t) { super(h, i, t); }
    @Inject(method = "drawBackground", at = @At("RETURN"), require = 0)
    private void asteria$style(DrawContext ctx, float delta, int mx, int my, CallbackInfo ci) {
        try { if (!Hud.styledInventory()) return; ctx.draw(); // the vanilla picture is queued: put it in the frame first, or it covered our panel
            java.util.List<int[]> slots = new java.util.ArrayList<>();
            for (Slot s : this.handler.slots) slots.add(new int[]{s.x, s.y});
            Hud.containerScreen(new Gfx120(ctx), this.x, this.y, this.backgroundWidth, this.backgroundHeight, slots); } catch (Throwable ignored) {}
    }
}
