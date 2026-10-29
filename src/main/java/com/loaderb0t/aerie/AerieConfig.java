package com.loaderb0t.aerie;

import net.neoforged.neoforge.common.ModConfigSpec;

/** SERVER config: per world, synced to clients, editable in singleplayer through the mod's config screen. */
public final class AerieConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue CHARGE_TIME_SECONDS = BUILDER
            .comment("Seconds a complete shrine needs to attune before flight works.")
            .defineInRange("chargeTimeSeconds", 300, 0, 86400);
    public static final ModConfigSpec.IntValue COMBAT_LOCKOUT_SECONDS = BUILDER
            .comment("Seconds flight stays blocked after hitting or being hit by a monster.")
            .defineInRange("combatLockoutSeconds", 30, 0, 3600);
    public static final ModConfigSpec.IntValue LEAVE_GRACE_SECONDS = BUILDER
            .comment("Seconds of Slow Falling when flight ends for any reason other than combat.")
            .defineInRange("leaveGraceSeconds", 3, 0, 60);
    public static final ModConfigSpec.IntValue MAX_RELAY_DISTANCE = BUILDER
            .comment("Maximum distance in chunks between a relay and its Core.")
            .defineInRange("maxRelayDistance", 5, 0, 32);
    public static final ModConfigSpec.BooleanValue DIAGONAL_ADJACENCY = BUILDER
            .comment("Whether diagonal chunks count as neighbours when chaining relays.")
            .define("diagonalAdjacency", true);
    public static final ModConfigSpec.IntValue OP_PERMISSION_LEVEL = BUILDER
            .comment("Permission level needed to lock or unlock other players' Cores.")
            .defineInRange("opPermissionLevel", 2, 0, 4);
    public static final ModConfigSpec.BooleanValue BORDER_PARTICLES = BUILDER
            .comment("Show particles at the edge of the flight area while flying near it.")
            .define("borderParticles", true);
    public static final ModConfigSpec.IntValue CHECK_INTERVAL_TICKS = BUILDER
            .comment("How often, in ticks, each player's flight eligibility is checked.")
            .defineInRange("checkIntervalTicks", 10, 1, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AerieConfig() {}
}
