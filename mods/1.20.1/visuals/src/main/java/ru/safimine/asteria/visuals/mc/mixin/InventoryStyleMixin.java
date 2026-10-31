package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.mc.Gfx120;

/** "Свой хотбар и инвентарь": the inventory screen in the Asteria style, drawn over the vanilla picture (the player and the items stay) */
@Mixin(InventoryScreen.class)
public abstract class InventoryStyleMixin extends HandledScreen<PlayerScreenHandler> {
    private InventoryStyleMixin(PlayerScreenHandler h, PlayerInventory i, Text t) { super(h, i, t); }
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture", shift = At.Shift.AFTER, ordinal = 0), require = 0)
    private void asteria$style(DrawContext ctx, float delta, int mx, int my, CallbackInfo ci) {
        try { if (!Hud.styledInventory()) return; java.util.List<int[]> slots = new java.util.ArrayList<>();
            for (Slot s : this.handler.slots) slots.add(new int[]{s.x, s.y});
            Hud.inventoryScreen(new Gfx120(ctx), this.x, this.y, this.backgroundWidth, this.backgroundHeight, slots); } catch (Throwable ignored) {}
    }
}
