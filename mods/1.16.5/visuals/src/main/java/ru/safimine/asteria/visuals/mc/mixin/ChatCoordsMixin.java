package ru.safimine.asteria.visuals.mc.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.safimine.asteria.visuals.Fx;

/** coordinates in any chat line get a "⌖ метка" button after them: one click puts a marker there (ScreenClickMixin) */
@Mixin(ChatHud.class)
public class ChatCoordsMixin {
    private static Text asteria$withMark(Text t) {
        try { String xyz = Fx.chatCoords(t.getString()); if (xyz == null) return t;
            return t.shallowCopy().append(new LiteralText("  ⌖ метка").setStyle(Style.EMPTY.withColor(Formatting.AQUA).withFormatting(Formatting.UNDERLINE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "asteria:mark " + xyz)))); } catch (Throwable e) { return t; }
    }
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text asteria$plain(Text t) { return asteria$withMark(t); }
}
