package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.mc.Gfx116;

/** "Свой хотбар и инвентарь": the inventory screen in the Asteria style, drawn over the vanilla picture, under the player (the player and the items stay) */
@Mixin(InventoryScreen.class)
public abstract class InventoryStyleMixin extends HandledScreen<PlayerScreenHandler> {
    private InventoryStyleMixin(PlayerScreenHandler h, PlayerInventory i, Text t) { super(h, i, t); }
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V"), require = 0)
    private void asteria$style(MatrixStack m, float delta, int mx, int my, CallbackInfo ci) {
        try { if (!Hud.styledInventory()) return;
            java.util.List<int[]> slots = new java.util.ArrayList<>();
            for (Slot s : this.handler.slots) slots.add(new int[]{s.x, s.y});
            Hud.inventoryScreen(new Gfx116(m), this.x, this.y, this.backgroundWidth, this.backgroundHeight, slots); } catch (Throwable ignored) {}
    }
    /** the "Создание" title light on the dark panel */
    @ModifyConstant(method = "drawForeground", constant = @Constant(intValue = 4210752), require = 0)
    private int asteria$label(int c) { return Hud.styledInventory() ? Hud.LABEL : c; }
}
