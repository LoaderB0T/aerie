package com.loaderb0t.aerie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** All of Aerie's particle effects. Server-side: everything goes out through ServerLevel.sendParticles. */
final class AerieParticles {
    private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.72f, 0.38f, 1.0f), 1.1f);
    private static final DustParticleOptions TEAL = new DustParticleOptions(new Vector3f(0.30f, 0.95f, 0.85f), 1.0f);
    private static final DustParticleOptions BORDER = new DustParticleOptions(new Vector3f(0.80f, 0.55f, 1.0f), 1.7f);
    private static final int[][] FOCI = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
    private static final double BORDER_RANGE = 6.0;

    private AerieParticles() {}

    /**
     * While attuning (called every 4 ticks): enchanting glyphs stream from each Focus into the Core, a violet
     * double helix winds up around it, and near the end the vortex spills upward. All of it grows with the charge.
     */
    static void charging(ServerLevel level, BlockPos core, float fraction) {
        Vec3 c = core.getCenter();
        RandomSource random = level.random;

        int glyphs = 1 + (int) (fraction * 4);
        for (int[] f : FOCI) {
            Vec3 toFocus = core.offset(f[0], 1, f[1]).getCenter().subtract(c);
            for (int i = 0; i < glyphs; i++) {
                // Enchant particles fly from (pos + velocity) to pos: aim them from the Focus into the Core.
                level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 0,
                        toFocus.x + jitter(random, 0.3), toFocus.y + jitter(random, 0.3), toFocus.z + jitter(random, 0.3), 1.0);
            }
        }

        double turn = level.getGameTime() * 0.3;
        double radius = 0.55 + fraction * 0.5;
        double height = 0.5 + fraction * 1.8;
        for (int strand = 0; strand < 2; strand++) {
            for (int k = 0; k < 4; k++) {
                double h = (k + random.nextDouble()) / 4 * height;
                double angle = turn + strand * Math.PI + h * 2.5;
                level.sendParticles(VIOLET, c.x + Math.cos(angle) * radius, c.y - 0.45 + h, c.z + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0);
            }
        }

        if (fraction > 0.75) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.6, c.z, 3, 0.15, 0.1, 0.15, 0.03);
        }
    }

    /** The moment attunement finishes: a flash, a burst of light and a shimmer settling over the shrine. */
    static void attuned(ServerLevel level, BlockPos core) {
        Vec3 c = core.getCenter();
        level.playSound(null, core, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1f);
        level.playSound(null, core, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 70, 0.4, 0.4, 0.4, 0.18);
        level.sendParticles(TEAL, c.x, c.y + 1, c.z, 60, 2.2, 1.2, 2.2, 0);
        for (int[] f : FOCI) {
            Vec3 p = core.offset(f[0], 1, f[1]).getCenter();
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.5, p.z, 8, 0.1, 0.4, 0.1, 0.03);
        }
    }

    /** Once per second while active: a quiet sign of life, so you can tell the shrine is working. */
    static void active(ServerLevel level, BlockPos core) {
        Vec3 c = core.getCenter();
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.6, c.z, 1, 0.12, 0.05, 0.12, 0.01);
        int[] f = FOCI[level.random.nextInt(FOCI.length)];
        Vec3 p = core.offset(f[0], 1, f[1]).getCenter();
        level.sendParticles(TEAL, p.x, p.y + 0.6, p.z, 2, 0.15, 0.15, 0.15, 0);
    }

    /**
     * A shimmering violet wall on each chunk edge within range that borders an uncovered chunk, denser the closer
     * the player flies to it. Only that player sees it.
     */
    static void border(ServerPlayer player, AerieNetworkData data) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        ChunkPos chunk = player.chunkPosition();
        double localX = player.getX() - chunk.getMinBlockX();
        double localZ = player.getZ() - chunk.getMinBlockZ();
        int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        double[] distances = {localX, 16 - localX, localZ, 16 - localZ};
        for (int i = 0; i < 4; i++) {
            int dx = sides[i][0], dz = sides[i][1];
            if (distances[i] > BORDER_RANGE || data.canFly(player.getUUID(), ChunkPos.asLong(chunk.x + dx, chunk.z + dz))) continue;
            double closeness = 1 - distances[i] / BORDER_RANGE;
            double plane = dx < 0 ? chunk.getMinBlockX() : dx > 0 ? chunk.getMaxBlockX() + 1
                    : dz < 0 ? chunk.getMinBlockZ() : chunk.getMaxBlockZ() + 1;
            double alongCenter = dx != 0 ? player.getZ() : player.getX();
            // A lattice snapped to the block grid, so the wall reads as a surface rather than random dust.
            for (int a = -5; a <= 5; a++) {
                for (int v = -2; v <= 4; v++) {
                    if (random.nextDouble() > 0.25 + 0.75 * closeness) continue;
                    double along = Math.floor(alongCenter) + a + 0.5 + jitter(random, 0.15);
                    double y = Math.floor(player.getY()) + v + 0.5 + jitter(random, 0.15);
                    double x = dx != 0 ? plane : along;
                    double z = dx != 0 ? along : plane;
                    level.sendParticles(player, BORDER, true, x, y, z, 1, 0, 0, 0, 0);
                }
            }
            for (int s = 0; s < 3; s++) {
                double along = alongCenter + jitter(random, 4);
                double y = player.getY() + 1 + jitter(random, 2.5);
                level.sendParticles(player, ParticleTypes.END_ROD, true, dx != 0 ? plane : along, y, dx != 0 ? along : plane,
                        1, 0, 0, 0, 0);
            }
        }
    }

    private static double jitter(RandomSource random, double amount) {
        return (random.nextDouble() * 2 - 1) * amount;
    }
}
