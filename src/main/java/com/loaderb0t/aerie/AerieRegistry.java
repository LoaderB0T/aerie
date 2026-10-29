package com.loaderb0t.aerie;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public final class AerieRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AerieMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AerieMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AerieMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AerieMod.MOD_ID);

    public static final DeferredBlock<Block> PLINTH = BLOCKS.registerSimpleBlock("plinth", stone(MapColor.COLOR_BLACK));
    public static final DeferredBlock<Block> COLUMN = BLOCKS.registerSimpleBlock("column", stone(MapColor.QUARTZ));
    public static final DeferredBlock<Block> FOCUS = BLOCKS.registerSimpleBlock("focus", crystal(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<CoreBlock> CORE = BLOCKS.register("core", () -> new CoreBlock(stone(MapColor.COLOR_CYAN)));
    public static final DeferredBlock<RelayCrownBlock> RELAY_CROWN = BLOCKS.register("relay_crown", () -> new RelayCrownBlock(crystal(MapColor.GOLD)));

    static {
        for (DeferredBlock<?> block : List.of(PLINTH, COLUMN, FOCUS, CORE, RELAY_CROWN)) ITEMS.registerSimpleBlockItem(block);
    }

    @SuppressWarnings("DataFlowIssue") // null data fixer type is the vanilla convention for mod block entities
    public static final Supplier<BlockEntityType<CoreBlockEntity>> CORE_BE = BLOCK_ENTITIES.register("core",
            () -> BlockEntityType.Builder.of(CoreBlockEntity::new, CORE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<RelayCrownBlockEntity>> RELAY_CROWN_BE = BLOCK_ENTITIES.register("relay_crown",
            () -> BlockEntityType.Builder.of(RelayCrownBlockEntity::new, RELAY_CROWN.get()).build(null));

    public static final Supplier<CreativeModeTab> TAB = TABS.register("aerie", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aerie"))
            .icon(() -> new ItemStack(CORE.get()))
            .displayItems((params, output) -> {
                output.accept(PLINTH);
                output.accept(COLUMN);
                output.accept(FOCUS);
                output.accept(CORE);
                output.accept(RELAY_CROWN);
            })
            .build());

    private AerieRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }

    private static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(3.5f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties crystal(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(3.5f).sound(SoundType.AMETHYST).lightLevel(s -> 7);
    }
}
