package ru.safimine.asteria.mixin;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.core.Asteria;

/** Skin, cape (also on the elytra) and arm model of AsteriaLauncher players; the own ones come straight from the launcher's files. */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void asteria$skin(CallbackInfoReturnable<SkinTextures> cir) {
        String name = ((PlayerListEntry) (Object) this).getProfile().getName();
        Object skin = Asteria.skin(name), cape = Asteria.cape(name);
        if (skin == null && cape == null) return;
        SkinTextures o = cir.getReturnValue();
        // record parts (texture, textureUrl, cape, elytra, model, secure) are accessed by their runtime names
        Identifier tex = skin != null ? (Identifier) skin : o.comp_1626();
        SkinTextures.Model model = skin != null ? (Asteria.slim(name) ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE) : o.comp_1629();
        Identifier c = cape != null ? (Identifier) cape : o.comp_1627();
        Identifier e = cape != null ? (Identifier) cape : o.comp_1628();
        cir.setReturnValue(new SkinTextures(tex, o.comp_1911(), c, e, model, o.comp_1630()));
    }
}
