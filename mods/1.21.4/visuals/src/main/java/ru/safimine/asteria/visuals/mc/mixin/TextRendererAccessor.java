package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** the game's own font lookup (FontManager), past our "font for all text" swap */
@Mixin(TextRenderer.class)
public interface TextRendererAccessor {
    @Accessor("fontStorageAccessor") java.util.function.Function<Identifier, FontStorage> asteria$fonts();
}
