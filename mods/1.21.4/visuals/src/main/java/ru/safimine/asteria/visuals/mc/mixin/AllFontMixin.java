package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.safimine.asteria.visuals.mc.VisualsClient;

/** "Our font for all of Minecraft": chat, nicks, menus, item names use the font chosen in the theme */
@Mixin(TextRenderer.class)
public class AllFontMixin {
    @ModifyVariable(method = "getFontStorage", at = @At("HEAD"), argsOnly = true, require = 0)
    private Identifier asteria$font(Identifier id) { try { return VisualsClient.allFont(id); } catch (Throwable t) { return id; } }
}
