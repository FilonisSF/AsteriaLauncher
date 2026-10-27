package ru.safimine.asteria.visuals.mc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Items;
import ru.safimine.asteria.visuals.Game;
import ru.safimine.asteria.visuals.mc.mixin.BossBarAccessor;

import java.util.ArrayList;
import java.util.List;

/** Game state for 1.20.x. */
final class Game120 implements Game {
    static final Game120 I = new Game120();
    private final MinecraftClient c = MinecraftClient.getInstance();

    private KeyBinding kb(int i) {
        GameOptions o = c.options;
        switch (i) { case 0: return o.forwardKey; case 1: return o.leftKey; case 2: return o.backKey; case 3: return o.rightKey; case 4: return o.jumpKey; case 5: return o.sneakKey; case 6: return o.attackKey; default: return o.useKey; }
    }
    @Override public int width() { return c.getWindow().getScaledWidth(); }
    @Override public int height() { return c.getWindow().getScaledHeight(); }
    @Override public boolean hudHidden() { return c.options.hudHidden; }
    @Override public boolean screenOpen() { return c.currentScreen != null; }
    @Override public boolean key(int i) { return kb(i).isPressed(); }
    @Override public String keyName(int i) { String s = kb(i).getBoundKeyLocalizedText().getString(); return s.length() > 4 ? s.substring(0, 4) : s.toUpperCase(); }
    @Override public boolean rawKey(int code) { long h = c.getWindow().getHandle(); return code < 0 ? GLFW.glfwGetMouseButton(h, -code - 1) == GLFW.GLFW_PRESS : InputUtil.isKeyPressed(h, code); }
    @Override public String keyLabel(int code) { try { String s = InputUtil.fromKeyCode(code, 0).getLocalizedText().getString(); return s.length() > 10 ? s.substring(0, 10) : s; } catch (Throwable t) { return "#" + code; } }
    @Override public String username() { return c.getSession().getUsername(); }
    @Override public int ping() {
        ClientPlayNetworkHandler n = c.getNetworkHandler(); if (n == null || c.player == null) return -1;
        PlayerListEntry e = n.getPlayerListEntry(c.player.getUuid()); return e == null ? -1 : e.getLatency();
    }
    @Override public double x() { return c.player == null ? 0 : c.player.getX(); }
    @Override public double y() { return c.player == null ? 0 : c.player.getY(); }
    @Override public double z() { return c.player == null ? 0 : c.player.getZ(); }
    @Override public float yaw() { return c.player == null ? 0 : c.player.getYaw(); }
    @Override public String server() { return c.getCurrentServerEntry() == null ? null : c.getCurrentServerEntry().address; }
    @Override public List<Item> armor() {
        List<Item> out = new ArrayList<>(); ClientPlayerEntity p = c.player; if (p == null) return out;
        for (int i = 3; i >= 0; i--) { ItemStack s = p.getInventory().armor.get(i); if (!s.isEmpty()) out.add(new Item(s, s.getDamage(), s.isDamageable() ? s.getMaxDamage() : 0, s.getName().getString())); }
        return out;
    }
    @Override public List<Effect> effects() {
        List<Effect> out = new ArrayList<>(); if (c.player == null) return out;
        for (StatusEffectInstance e : c.player.getStatusEffects()) out.add(new Effect(e.getEffectType(), e.getEffectType().getName().getString(), e.getAmplifier(), e.getDuration(), e.getEffectType().getColor()));
        return out;
    }
    @Override public float cooldown() { return c.player == null ? 1 : c.player.getAttackCooldownProgress(0); }
    @Override public Object self() { return c.player; }
    @Override public Object crosshairEntity() { return c.targetedEntity; }
    @Override public boolean living(Object e) { return e instanceof LivingEntity; }
    @Override public boolean alive(Object e) { return e instanceof Entity && ((Entity) e).isAlive() && !((Entity) e).isRemoved(); }
    @Override public double[] pos(Object o, float pt) { Entity e = (Entity) o; return new double[]{MathHelper.lerp(pt, e.lastRenderX, e.getX()), MathHelper.lerp(pt, e.lastRenderY, e.getY()), MathHelper.lerp(pt, e.lastRenderZ, e.getZ())}; }
    @Override public float height(Object e) { return ((Entity) e).getHeight(); }
    @Override public float width(Object e) { return ((Entity) e).getWidth(); }
    @Override public String name(Object e) { return ((Entity) e).getDisplayName().getString(); }
    @Override public float hp(Object e) { return e instanceof LivingEntity ? ((LivingEntity) e).getHealth() : 0; }
    @Override public float maxHp(Object e) { return e instanceof LivingEntity ? ((LivingEntity) e).getMaxHealth() : 20; }
    @Override public Object skin(Object e) { return e instanceof AbstractClientPlayerEntity ? ((AbstractClientPlayerEntity) e).getSkinTexture() : null; }
    @Override public boolean onGround() { return c.player != null && c.player.isOnGround(); }
    @Override public double motionY() { return c.player == null ? 0 : c.player.getVelocity().y; }
    @Override public boolean thirdPerson() { return !c.options.getPerspective().isFirstPerson(); }
    @Override public float bodyYaw(float pt) { return c.player == null ? 0 : MathHelper.lerp(pt, c.player.prevBodyYaw, c.player.bodyYaw); }
    @Override public void sound(String id, float vol, float pitch) { c.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(new Identifier("asteria_visuals", id)), pitch, vol)); }
    @Override public Object[] inventory() { Object[] o = new Object[36]; if (c.player == null) return o; for (int i = 0; i < 36; i++) { ItemStack s = c.player.getInventory().main.get(i); o[i] = s.isEmpty() ? null : s; } return o; }
    @Override public Object offhand() { if (c.player == null) return null; ItemStack s = c.player.getOffHandStack(); return s.isEmpty() ? null : s; }
    @Override public int selectedSlot() { return c.player == null ? 0 : c.player.getInventory().selectedSlot; }
    @Override public int count(Object s) { return s == null ? 0 : ((ItemStack) s).getCount(); }
    @Override public String itemId(Object s) { return s == null ? "" : Registries.ITEM.getId(((ItemStack) s).getItem()).toString(); }
    @Override public float itemCooldown(Object s) { return s == null || c.player == null ? 0 : c.player.getItemCooldownManager().getCooldownProgress(((ItemStack) s).getItem(), 0); }
    @Override public boolean chatOpen() { return c.currentScreen instanceof ChatScreen; }
    @Override public Object totemStack() { return new ItemStack(Items.TOTEM_OF_UNDYING); }
    @Override public int bossBars() { try { return ((BossBarAccessor) c.inGameHud.getBossBarHud()).asteria$bars().size(); } catch (Throwable t) { return 0; } }
    @Override public java.util.List<Object> players() { java.util.List<Object> o = new ArrayList<>(); if (c.world == null) return o; for (Object p : c.world.getPlayers()) if (p != c.player) o.add(p); return o; }
    @Override public String playerName(Object e) { return e instanceof net.minecraft.entity.player.PlayerEntity ? ((net.minecraft.entity.player.PlayerEntity) e).getGameProfile().getName() : ""; }
    @Override public float yawOf(Object e) { return e instanceof Entity ? ((Entity) e).getYaw() : 0; }
    @Override public boolean sprinting() { return c.player != null && c.player.isSprinting(); }
    @Override public double[] targetBlock() {
        if (c.world == null || !(c.crosshairTarget instanceof BlockHitResult) || c.crosshairTarget.getType() != HitResult.Type.BLOCK) return null;
        BlockPos p = ((BlockHitResult) c.crosshairTarget).getBlockPos(); VoxelShape sh = c.world.getBlockState(p).getOutlineShape(c.world, p);
        if (sh.isEmpty()) return null; Box b = sh.getBoundingBox();
        return new double[]{p.getX() + b.minX, p.getY() + b.minY, p.getZ() + b.minZ, p.getX() + b.maxX, p.getY() + b.maxY, p.getZ() + b.maxZ};
    }
    @Override public void openMenu() { c.setScreen(new AsteriaScreen()); }
    @Override public void closeMenu() { c.setScreen(null); }
}
