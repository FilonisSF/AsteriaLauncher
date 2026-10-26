package ru.safimine.asteria.mixin;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.safimine.asteria.core.Asteria;

/** Skin, cape (also on the elytra) and arm model of AsteriaLauncher players; the own ones come straight from the launcher's files. */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    private static AssetInfo.TextureAsset asteria$asset(Object id) { return new AssetInfo.TextureAssetInfo((Identifier) id, (Identifier) id); }

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void asteria$skin(CallbackInfoReturnable<SkinTextures> cir) {
        String name = ((PlayerListEntry) (Object) this).getProfile().name();
        Object skin = Asteria.skin(name), cape = Asteria.cape(name);
        if (skin == null && cape == null) return;
        SkinTextures o = cir.getReturnValue();
        // record parts (body, cape, elytra, model, secure) are accessed by their runtime names
        AssetInfo.TextureAsset body = skin != null ? asteria$asset(skin) : o.comp_1626();
        PlayerSkinType model = skin != null ? (Asteria.slim(name) ? PlayerSkinType.SLIM : PlayerSkinType.WIDE) : o.comp_1629();
        AssetInfo.TextureAsset c = cape != null ? asteria$asset(cape) : o.comp_1627();
        AssetInfo.TextureAsset e = cape != null ? asteria$asset(cape) : o.comp_1628();
        cir.setReturnValue(new SkinTextures(body, c, e, model, o.comp_1630()));
    }
}
