package ru.safimine.asteria.mixin;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.core.Asteria;

/** Skin, cape (also on the elytra) and arm model of AsteriaLauncher players; the own ones come straight from the launcher's files. */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    private String asteria$name() { return ((PlayerListEntry) (Object) this).getProfile().getName(); }

    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void asteria$skin(CallbackInfoReturnable<Identifier> cir) { Object id = Asteria.skin(asteria$name()); if (id != null) cir.setReturnValue((Identifier) id); }
    @Inject(method = "hasSkinTexture", at = @At("HEAD"), cancellable = true)
    private void asteria$hasSkin(CallbackInfoReturnable<Boolean> cir) { if (Asteria.skin(asteria$name()) != null) cir.setReturnValue(true); }
    @Inject(method = "getCapeTexture", at = @At("HEAD"), cancellable = true)
    private void asteria$cape(CallbackInfoReturnable<Identifier> cir) { Object id = Asteria.cape(asteria$name()); if (id != null) cir.setReturnValue((Identifier) id); }
    @Inject(method = "getElytraTexture", at = @At("HEAD"), cancellable = true)
    private void asteria$elytra(CallbackInfoReturnable<Identifier> cir) { Object id = Asteria.cape(asteria$name()); if (id != null) cir.setReturnValue((Identifier) id); }
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void asteria$model(CallbackInfoReturnable<String> cir) { if (Asteria.skin(asteria$name()) != null) cir.setReturnValue(Asteria.slim(asteria$name()) ? "slim" : "default"); }
}
