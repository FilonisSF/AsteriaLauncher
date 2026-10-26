package ru.safimine.asteria;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** the Asteria star glyph (own bitmap font "asteria:icons", char U+E000) */
public final class Star {
    private static final Identifier FONT = Identifier.of("asteria", "icons");
    private Star() {}
    /** our star glyph; it is recognised by identity, never by text (resource packs put donate icons into U+E000.. too) */
    private static final MutableText STAR = Text.literal("").setStyle(Style.EMPTY.withFont(FONT));
    public static Text prefix(Text name) {
        java.util.List<Text> sib = name.getSiblings();
        if (!sib.isEmpty() && sib.get(0) == STAR) return name; // already ours
        // the star, the space and the name are SIBLINGS of an unstyled root: a child would inherit the star's font
        // and the nick / donate prefix (resource-pack glyphs) would turn into boxes
        MutableText root = Text.literal("");
        return root.append(STAR).append(Text.literal(" ")).append(name);
    }
}
