package ru.safimine.asteria.visuals.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.visuals.Fx;
import net.minecraft.client.font.TextVisitFactory;

/** "Hide nick": your name in any drawn text becomes an alias (chat, tab, name tags, scoreboard) */
@Mixin(TextVisitFactory.class)
public class NameProtectMixin {
    @ModifyVariable(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String asteria$f(String s) { return Fx.protect(s); }
    @ModifyVariable(method = "visitForwards(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String asteria$fw(String s) { return Fx.protect(s); }
    @ModifyVariable(method = "visitBackwards(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String asteria$bw(String s) { return Fx.protect(s); }
}
