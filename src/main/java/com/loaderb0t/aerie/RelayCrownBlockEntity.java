package com.loaderb0t.aerie;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RelayCrownBlockEntity extends BlockEntity {
    public RelayCrownBlockEntity(BlockPos pos, BlockState state) {
        super(AerieRegistry.RELAY_CROWN_BE.get(), pos, state);
    }
}
