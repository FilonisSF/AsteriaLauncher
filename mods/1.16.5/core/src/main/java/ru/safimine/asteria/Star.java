package ru.safimine.asteria;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** the Asteria star glyph (own bitmap font "asteria:icons", char U+E000) */
public final class Star {
    private static final Identifier FONT = new Identifier("asteria", "icons");
    private Star() {}
    public static Text prefix(Text name) {
        if (name.getString().startsWith("")) return name;
        MutableText star = new LiteralText("").setStyle(Style.EMPTY.withFont(FONT));
        return star.append(new LiteralText(" ").setStyle(Style.EMPTY)).append(name);
    }
}
