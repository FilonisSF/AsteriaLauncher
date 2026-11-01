package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** the dark grey titles of the styled screens ("Сундук", "Инвентарь") turn light, or they were lost on the dark Asteria panel */
@Mixin(HandledScreen.class)
public class ScreenLabelMixin {
    @ModifyConstant(method = "drawForeground", constant = @Constant(intValue = 4210752), require = 0)
    private int asteria$label(int c) { return VisualsClient.styledScreen(this) ? ru.safimine.asteria.visuals.Hud.LABEL : c; }
}
