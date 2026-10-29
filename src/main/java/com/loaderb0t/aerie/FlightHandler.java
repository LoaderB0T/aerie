package com.loaderb0t.aerie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Grants and revokes Aerie flight. Aerie only ever adds or removes its own CREATIVE_FLIGHT modifier;
 * it stops a player flying only when nothing else (creative mode, other mods) still allows flight.
 */
public final class FlightHandler {
    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(AerieMod.MOD_ID, "flight");

    /**
     * World game time until which a player may not get Aerie flight. Game time is saved and shared by all
     * dimensions; entries survive relogs (so logging out can't skip a lockout) and are cleared when the server stops.
     */
    private static final Map<UUID, Long> lockoutUntil = new HashMap<>();

    private FlightHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % AerieConfig.CHECK_INTERVAL_TICKS.get() == 0) {
            check(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        lockoutUntil.clear(); // singleplayer keeps the JVM: don't carry lockouts into the next world
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        boolean byMonster = source.getEntity() instanceof Enemy || source.getDirectEntity() instanceof Enemy;
        if (event.getEntity() instanceof ServerPlayer player && byMonster) cutOff(player);
        else if (event.getEntity() instanceof Enemy && source.getEntity() instanceof ServerPlayer player) cutOff(player);
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) onLogin(player);
    }

    /**
     * abilities.flying is saved but the flight modifier is not, so a player who logged out mid-air would drop on
     * their first tick. Re-check at once; if Aerie no longer lets them fly, let them float down instead.
     */
    static void onLogin(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;
        boolean wasFlying = player.getAbilities().flying;
        check(player);
        if (wasFlying && !player.mayFly()) {
            stopFlyingIfNoOtherSource(player);
            int grace = AerieConfig.LEAVE_GRACE_SECONDS.get();
            if (grace > 0) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, grace * 20));
        }
    }

    static void check(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) return;
        var data = AerieNetworkData.get(player.serverLevel());
        boolean eligible = !lockedOut(player) && data.canFly(player.getUUID(), player.chunkPosition().toLong());
        boolean has = flight.hasModifier(MODIFIER_ID);
        if (eligible && !has) {
            flight.addTransientModifier(new AttributeModifier(MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
        } else if (!eligible && has) {
            boolean wasFlying = player.getAbilities().flying;
            flight.removeModifier(MODIFIER_ID);
            stopFlyingIfNoOtherSource(player);
            int grace = AerieConfig.LEAVE_GRACE_SECONDS.get();
            if (wasFlying && !player.mayFly() && grace > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, grace * 20));
            }
        }
        if (eligible && player.getAbilities().flying && AerieConfig.BORDER_PARTICLES.get()) borderParticles(player, data);
    }

    private static boolean lockedOut(ServerPlayer player) {
        Long until = lockoutUntil.get(player.getUUID());
        if (until == null) return false;
        if (until > player.serverLevel().getGameTime()) return true;
        lockoutUntil.remove(player.getUUID());
        return false;
    }

    /** Combat: drop Aerie flight instantly, no grace, and block it for the lockout time. */
    static void cutOff(ServerPlayer player) {
        lockoutUntil.put(player.getUUID(), player.serverLevel().getGameTime() + AerieConfig.COMBAT_LOCKOUT_SECONDS.get() * 20L);
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && flight.hasModifier(MODIFIER_ID)) {
            flight.removeModifier(MODIFIER_ID);
            stopFlyingIfNoOtherSource(player);
        }
    }

    static void stopFlyingIfNoOtherSource(ServerPlayer player) {
        if (player.getAbilities().flying && !player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    /** Particles on each chunk edge within 4 blocks that borders an uncovered chunk; only this player sees them. */
    private static void borderParticles(ServerPlayer player, AerieNetworkData data) {
        ChunkPos chunk = player.chunkPosition();
        double localX = player.getX() - chunk.getMinBlockX();
        double localZ = player.getZ() - chunk.getMinBlockZ();
        int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        double[] distances = {localX, 16 - localX, localZ, 16 - localZ};
        var random = player.getRandom();
        for (int i = 0; i < 4; i++) {
            int dx = sides[i][0], dz = sides[i][1];
            if (distances[i] > 4 || data.canFly(player.getUUID(), ChunkPos.asLong(chunk.x + dx, chunk.z + dz))) continue;
            for (int n = 0; n < 6; n++) {
                double along = (random.nextDouble() * 2 - 1) * 4;
                double y = player.getY() + 1 + (random.nextDouble() * 2 - 1) * 3;
                double x = dx != 0 ? (dx < 0 ? chunk.getMinBlockX() : chunk.getMaxBlockX() + 1) : player.getX() + along;
                double z = dz != 0 ? (dz < 0 ? chunk.getMinBlockZ() : chunk.getMaxBlockZ() + 1) : player.getZ() + along;
                player.serverLevel().sendParticles(player, ParticleTypes.END_ROD, true, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
}
