package ru.safimine.asteria;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** the Asteria star glyph (own bitmap font "asteria:icons", char U+E000) */
public final class Star {
    private static final Identifier FONT = Identifier.of("asteria", "icons");
    private Star() {}
    public static Text prefix(Text name) {
        if (name.getString().startsWith("")) return name;
        MutableText star = Text.literal("").setStyle(Style.EMPTY.withFont(FONT));
        return star.append(Text.literal(" ").setStyle(Style.EMPTY)).append(name);
    }
}
