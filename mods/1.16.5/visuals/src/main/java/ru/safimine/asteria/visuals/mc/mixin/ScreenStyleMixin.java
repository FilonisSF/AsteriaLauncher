package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import net.minecraft.client.gui.screen.ingame.LoomScreen;
import net.minecraft.client.gui.screen.ingame.CartographyTableScreen;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.safimine.asteria.visuals.Hud;
import ru.safimine.asteria.visuals.mc.VisualsClient;
import ru.safimine.asteria.visuals.mc.Gfx116;

/** "Свой хотбар и инвентарь" on every other screen with an inventory (crafting table, furnaces, brewing, anvil, enchanting, grindstone,
 *  stonecutter, loom, cartography, beacon, horse): our panel goes right after the vanilla background, so the progress arrows, flames,
 *  buttons and the rest the game draws next stay on top of it */
@Mixin({CraftingScreen.class, AbstractFurnaceScreen.class, BrewingStandScreen.class, ForgingScreen.class, EnchantmentScreen.class, GrindstoneScreen.class, StonecutterScreen.class, LoomScreen.class, CartographyTableScreen.class, BeaconScreen.class, HorseScreen.class})
public abstract class ScreenStyleMixin extends HandledScreen<ScreenHandler> {
    private ScreenStyleMixin(ScreenHandler h, PlayerInventory i, Text t) { super(h, i, t); }
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V", shift = At.Shift.AFTER, ordinal = 0), require = 0)
    private void asteria$style(net.minecraft.client.util.math.MatrixStack m, float delta, int mx, int my, CallbackInfo ci) {
        try { if (!Hud.styledInventory()) return; VisualsClient.markStyled(this);
            java.util.List<int[]> slots = new java.util.ArrayList<>();
            for (Slot s : this.handler.slots) if (s.x >= 0 && s.y >= 0 && s.x < this.backgroundWidth && s.y < this.backgroundHeight) slots.add(new int[]{s.x, s.y});
            Hud.containerScreen(new Gfx116(m), this.x, this.y, this.backgroundWidth, this.backgroundHeight, slots); } catch (Throwable ignored) {}
    }
}
