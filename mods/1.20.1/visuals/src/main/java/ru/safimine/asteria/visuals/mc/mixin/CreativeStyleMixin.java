package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.mc.Gfx120;

/** "Свой хотбар и инвентарь" in creative: the Asteria panel with soft slot tiles and our own scroll bar (tabs, search and items stay vanilla) */
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeStyleMixin extends HandledScreen<CreativeInventoryScreen.CreativeScreenHandler> {
    @Shadow private float scrollPosition;
    private CreativeStyleMixin(CreativeInventoryScreen.CreativeScreenHandler h, PlayerInventory i, Text t) { super(h, i, t); }
    @Inject(method = "drawBackground", at = @At("RETURN"), require = 0)
    private void asteria$style(DrawContext ctx, float delta, int mx, int my, CallbackInfo ci) {
        try { if (!Hud.styledInventory()) return; ctx.draw();
            java.util.List<int[]> slots = new java.util.ArrayList<>();
            for (Slot s : this.handler.slots) if (s.x >= 0 && s.y >= 0 && s.x < this.backgroundWidth && s.y < this.backgroundHeight) slots.add(new int[]{s.x, s.y});
            Hud.creativeScreen(new Gfx120(ctx), this.x, this.y, this.backgroundWidth, this.backgroundHeight, slots, this.scrollPosition); } catch (Throwable ignored) {}
    }
}
