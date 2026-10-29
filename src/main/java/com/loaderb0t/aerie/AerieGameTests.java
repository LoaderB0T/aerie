package com.loaderb0t.aerie;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

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
}
