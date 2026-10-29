package com.loaderb0t.aerie;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CoreBlockEntity extends BlockEntity {
    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(AerieRegistry.CORE_BE.get(), pos, state);
    }
}
