package com.loaderb0t.aerie;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** In-world checks, run headless with `gradlew runGameTestServer`. Only active when the aerie namespace is enabled. */
@GameTestHolder(AerieMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AerieGameTests {
    private static final List<String> BLOCKS = List.of("plinth", "column", "focus", "core", "relay_crown");

    private AerieGameTests() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AerieMod.MOD_ID, path);
    }

    @GameTest(template = "empty")
    public static void dataLoaded(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var nonMovable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "non_movable"));
        for (String block : BLOCKS) {
            helper.assertTrue(server.getRecipeManager().byKey(id(block)).isPresent(), "missing recipe " + block);
            var loot = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id("blocks/" + block)));
            helper.assertTrue(loot != LootTable.EMPTY, "missing loot table " + block);
        }
        helper.assertTrue(AerieRegistry.CORE.get().defaultBlockState().is(nonMovable), "core is not create:non_movable");
        helper.succeed();
    }

    // Shrine with its Core at relative (3, 2, 3); the template is 8x6x8 so it fits.
    private static final BlockPos CORE = new BlockPos(3, 2, 3);

    private static void buildShrine(GameTestHelper helper) {
        helper.setBlock(CORE.below(), AerieRegistry.PLINTH.get());
        helper.setBlock(CORE, AerieRegistry.CORE.get());
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                BlockPos base = CORE.offset(dx, -1, dz);
                helper.setBlock(base, AerieRegistry.PLINTH.get());
                helper.setBlock(base.above(), AerieRegistry.COLUMN.get());
                helper.setBlock(base.above(2), AerieRegistry.FOCUS.get());
            }
        }
    }

    private static CoreBlockEntity core(GameTestHelper helper) {
        return (CoreBlockEntity) helper.getBlockEntity(CORE);
    }

    private static boolean ownerCanFly(GameTestHelper helper, UUID owner) {
        return AerieNetworkData.get(helper.getLevel()).canFly(owner, new ChunkPos(helper.absolutePos(CORE)).toLong());
    }

    @GameTest(template = "empty")
    public static void shrineValidates(GameTestHelper helper) {
        buildShrine(helper);
        helper.assertTrue(AerieMultiblocks.SHRINE.validate(helper.getLevel(), helper.absolutePos(CORE)) != null, "complete shrine did not validate");
        // Unchecked cells are free for decoration.
        helper.setBlock(CORE.offset(1, -1, 0), Blocks.STONE_BRICKS);
        helper.setBlock(CORE.offset(0, 1, 1), Blocks.LANTERN);
        helper.assertTrue(AerieMultiblocks.SHRINE.validate(helper.getLevel(), helper.absolutePos(CORE)) != null, "decoration broke the shrine");
        helper.setBlock(CORE.offset(2, 1, 2), Blocks.AIR);
        helper.assertTrue(AerieMultiblocks.SHRINE.validate(helper.getLevel(), helper.absolutePos(CORE)) == null, "shrine without a focus validated");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void chargedShrineGrantsAndBreakingRevokes(GameTestHelper helper) {
        buildShrine(helper);
        var be = core(helper);
        UUID owner = UUID.randomUUID(); // tests share one world: never share an owner
        be.ownership().load(owner, false);
        int required = AerieConfig.CHARGE_TIME_SECONDS.get() * 20;
        be.charge().load(required - 20, helper.absolutePos(CORE).asLong());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ownerCanFly(helper, owner), "owner cannot fly after charge"))
                .thenExecute(() -> helper.setBlock(CORE.offset(-2, 1, -2), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertFalse(ownerCanFly(helper, owner), "owner still flies with a broken shrine"))
                .thenExecute(() -> helper.assertTrue(core(helper).charge().progress() == 0, "charge not reset"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void redstonePausesWithoutReset(GameTestHelper helper) {
        buildShrine(helper);
        var be = core(helper);
        UUID owner = UUID.randomUUID(); // tests share one world: never share an owner
        be.ownership().load(owner, false);
        int required = AerieConfig.CHARGE_TIME_SECONDS.get() * 20;
        be.charge().load(required, helper.absolutePos(CORE).asLong());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ownerCanFly(helper, owner), "charged core not active"))
                .thenExecute(() -> helper.setBlock(CORE.east(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertFalse(ownerCanFly(helper, owner), "powered core still active"))
                .thenExecute(() -> helper.assertTrue(core(helper).charge().progress() == required, "redstone changed the charge: " + core(helper).charge().progress() + " vs " + required))
                .thenExecute(() -> helper.setBlock(CORE.east(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(ownerCanFly(helper, owner), "core did not resume"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void movedCoreLosesCharge(GameTestHelper helper) {
        buildShrine(helper);
        var be = core(helper);
        UUID owner = UUID.randomUUID(); // tests share one world: never share an owner
        be.ownership().load(owner, false);
        int required = AerieConfig.CHARGE_TIME_SECONDS.get() * 20;
        be.charge().load(required, helper.absolutePos(CORE).asLong() + 1); // attuned somewhere else
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(core(helper).charge().anchor() == helper.absolutePos(CORE).asLong(), "anchor not updated"))
                .thenExecute(() -> helper.assertFalse(ownerCanFly(helper, owner), "moved core kept its charge"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void relayTotemConnectsAndDropsWhenBroken(GameTestHelper helper) {
        buildShrine(helper);
        core(helper).ownership().load(UUID.randomUUID(), false);
        // Within 16 blocks of the Core, so in the core chunk or a (diagonal) neighbour: always connectable.
        BlockPos base = new BlockPos(6, 1, 6);
        BlockPos crown = base.above(2);
        helper.setBlock(base, AerieRegistry.PLINTH.get());
        helper.setBlock(base.above(), AerieRegistry.COLUMN.get());
        helper.setBlock(crown, AerieRegistry.RELAY_CROWN.get());
        var data = AerieNetworkData.get(helper.getLevel());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(data.isConnected(helper.absolutePos(crown)), "relay did not connect"))
                .thenExecute(() -> helper.setBlock(base.above(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertFalse(data.isConnected(helper.absolutePos(crown)), "incomplete relay still connected"))
                .thenSucceed();
    }

    /** Like GameTestHelper.makeMockServerPlayerInLevel, whose player always reports isCreative() == true. */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "aerie-test"), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL); // the game-test server defaults to creative, which sets abilities.mayfly
        return player;
    }

    /** A survival player standing above a fully charged shrine they own. */
    private static ServerPlayer ownerAboveChargedShrine(GameTestHelper helper) {
        buildShrine(helper);
        ServerPlayer player = survivalPlayer(helper);
        Vec3 at = helper.absoluteVec(Vec3.atCenterOf(CORE.above(2)));
        player.teleportTo(at.x, at.y, at.z);
        var be = core(helper);
        be.ownership().load(player.getUUID(), false);
        be.charge().load(AerieConfig.CHARGE_TIME_SECONDS.get() * 20, helper.absolutePos(CORE).asLong());
        return player;
    }

    private static boolean hasAerieFlight(ServerPlayer player) {
        return player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).hasModifier(FlightHandler.MODIFIER_ID);
    }

    private static void moveChunksAway(ServerPlayer player, int chunks) {
        player.teleportTo(player.getX() + chunks * 16, player.getY(), player.getZ());
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void flightGrantedInAreaAndGraceOnLeaving(GameTestHelper helper) {
        ServerPlayer player = ownerAboveChargedShrine(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(AerieNetworkData.get(helper.getLevel()).canFly(player.getUUID(), player.chunkPosition().toLong()), "core not active"))
                .thenExecute(() -> FlightHandler.check(player))
                .thenExecute(() -> helper.assertTrue(hasAerieFlight(player) && player.mayFly(), "flight not granted in area"))
                .thenExecute(() -> {
                    player.getAbilities().flying = true;
                    moveChunksAway(player, 3);
                    FlightHandler.check(player);
                })
                .thenExecute(() -> helper.assertFalse(hasAerieFlight(player) || player.getAbilities().flying, "still flying outside the area"))
                .thenExecute(() -> helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "no slow-fall grace on leaving; effects=" + player.getActiveEffects() + " mayFly=" + player.mayFly()))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void monsterHitDropsFlightWithLockout(GameTestHelper helper) {
        ServerPlayer player = ownerAboveChargedShrine(helper);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    FlightHandler.check(player);
                    helper.assertTrue(hasAerieFlight(player), "flight not granted");
                })
                .thenIdle(61) // new players are invulnerable for 60 ticks
                .thenExecute(() -> {
                    player.getAbilities().flying = true;
                    // On Peaceful (the game-test default) monster damage to players is 0, so no damage event fires.
                    helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
                    Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, CORE.above(3));
                    helper.assertTrue(player.hurt(player.damageSources().mobAttack(zombie), 1f), "zombie hit was not applied");
                })
                .thenExecute(() -> helper.assertFalse(hasAerieFlight(player) || player.getAbilities().flying, "monster hit did not drop flight"))
                .thenExecute(() -> helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), "combat drop must not give grace"))
                .thenExecute(() -> FlightHandler.check(player))
                .thenExecute(() -> helper.assertFalse(hasAerieFlight(player), "flight came back during lockout"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void hittingMonsterDropsFlight(GameTestHelper helper) {
        ServerPlayer player = ownerAboveChargedShrine(helper);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    FlightHandler.check(player);
                    helper.assertTrue(hasAerieFlight(player), "flight not granted");
                })
                .thenExecute(() -> {
                    Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, CORE.above(3));
                    zombie.hurt(player.damageSources().playerAttack(player), 1f);
                })
                .thenExecute(() -> helper.assertFalse(hasAerieFlight(player), "hitting a monster did not drop flight"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void otherFlightSourceIsKept(GameTestHelper helper) {
        ServerPlayer player = ownerAboveChargedShrine(helper);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    FlightHandler.check(player);
                    helper.assertTrue(hasAerieFlight(player), "flight not granted");
                })
                .thenExecute(() -> {
                    player.getAbilities().mayfly = true; // another mod's flight
                    player.getAbilities().flying = true;
                    moveChunksAway(player, 3);
                    FlightHandler.check(player);
                })
                .thenExecute(() -> helper.assertTrue(!hasAerieFlight(player) && player.getAbilities().flying, "Aerie revoked someone else's flight"))
                .thenExecute(() -> helper.assertTrue(player.getAbilities().mayfly, "Aerie touched abilities.mayfly"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void claimResetsCharge(GameTestHelper helper) {
        buildShrine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var be = core(helper);
        UUID owner = UUID.randomUUID(); // tests share one world: never share an owner
        be.ownership().load(owner, false);
        be.charge().load(1000, helper.absolutePos(CORE).asLong());
        helper.useBlock(CORE, player);
        helper.assertTrue(player.getUUID().equals(be.ownership().owner()), "right-click did not claim");
        helper.assertTrue(be.charge().progress() == 0, "claim did not reset charge");
        helper.succeed();
    }
}
