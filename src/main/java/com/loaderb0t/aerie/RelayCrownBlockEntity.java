package com.loaderb0t.aerie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RelayCrownBlockEntity extends BlockEntity {
    /** Who built this relay: a free relay binds to their Core first. Null for relays placed by non-players. */
    private @Nullable UUID placer;

    public RelayCrownBlockEntity(BlockPos pos, BlockState state) {
        super(AerieRegistry.RELAY_CROWN_BE.get(), pos, state);
    }

    private boolean complete() {
        return AerieMultiblocks.RELAY.validate(level, worldPosition) != null;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, RelayCrownBlockEntity be) {
        if (level.getGameTime() % 20 != 0) return;
        AerieNetworkData.get((ServerLevel) level).putRelay(pos, be.complete(), be.placer);
    }

    void setPlacer(UUID placer) {
        this.placer = placer;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (placer != null) tag.putUUID("Placer", placer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        placer = tag.hasUUID("Placer") ? tag.getUUID("Placer") : null;
    }

    /** Status only: ownership is managed at the Core. */
    void use(Player player) {
        var data = AerieNetworkData.get((ServerLevel) level);
        Component message;
        if (!complete()) message = Component.translatable("aerie.relay.incomplete");
        else if (data.isConnected(worldPosition)) message = Component.translatable("aerie.relay.connected", data.boundOwnerName(worldPosition));
        else message = Component.translatable("aerie.relay.unconnected");
        player.displayClientMessage(message, true);
    }
}
