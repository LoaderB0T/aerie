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

public final class CoreBlockEntity extends BlockEntity {
    private final Ownership ownership = new Ownership();
    private final Charge charge = new Charge();
    private String ownerName = "";
    private boolean attuning;

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(AerieRegistry.CORE_BE.get(), pos, state);
    }

    Ownership ownership() {
        return ownership;
    }

    Charge charge() {
        return charge;
    }

    private static int requiredTicks() {
        return AerieConfig.CHARGE_TIME_SECONDS.get() * 20;
    }

    private boolean complete() {
        return ownership.owner() != null && AerieMultiblocks.SHRINE.validate(level, worldPosition) != null;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, CoreBlockEntity be) {
        ServerLevel serverLevel = (ServerLevel) level;
        long time = level.getGameTime();
        if (be.attuning && time % 4 == 0) AerieParticles.charging(serverLevel, pos, be.charge.fraction(requiredTicks()));
        if (time % 20 != 0) return;

        int required = requiredTicks();
        boolean moved = be.charge.checkAnchor(pos.asLong());
        int before = be.charge.progress();
        boolean complete = be.complete();
        boolean powered = level.hasNeighborSignal(pos);
        if (be.charge.tick(20, complete, powered, required) == Charge.Event.COMPLETED) AerieParticles.attuned(serverLevel, pos);
        boolean charged = be.charge.charged(required);
        boolean active = complete && charged && !powered;
        be.attuning = complete && !powered && !charged;
        if (active) AerieParticles.active(serverLevel, pos);
        AerieNetworkData.get(serverLevel).putCore(pos, be.ownership.owner(), be.ownerName, active);
        if (moved || before != be.charge.progress()) be.setChanged();
    }

    void use(Player player) {
        Ownership.Result result = ownership.use(player.getUUID(), player.isShiftKeyDown(),
                player.hasPermissions(AerieConfig.OP_PERMISSION_LEVEL.get()));
        Component message = switch (result) {
            case CLAIMED -> {
                charge.reset();
                ownerName = player.getGameProfile().getName();
                setChanged();
                yield Component.translatable("aerie.claimed");
            }
            case LOCKED -> {
                setChanged();
                yield Component.translatable("aerie.locked", ownerName);
            }
            case UNLOCKED -> {
                setChanged();
                yield Component.translatable("aerie.unlocked");
            }
            case DENIED -> Component.translatable("aerie.denied", ownerName);
            case STATUS -> status();
        };
        player.displayClientMessage(message, true);
    }

    private Component status() {
        if (ownership.owner() == null) return Component.translatable("aerie.status.unowned");
        if (!complete()) return Component.translatable("aerie.status.incomplete");
        if (level.hasNeighborSignal(worldPosition)) return Component.translatable("aerie.status.disabled");
        int required = requiredTicks();
        if (!charge.charged(required)) {
            return Component.translatable("aerie.status.charging", (int) (charge.fraction(required) * 100));
        }
        Component active = Component.translatable("aerie.status.active", ownerName);
        return ownership.locked() ? active.copy().append(Component.translatable("aerie.status.locked_suffix")) : active;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownership.owner() != null) tag.putUUID("Owner", ownership.owner());
        tag.putBoolean("Locked", ownership.locked());
        tag.putString("OwnerName", ownerName);
        tag.putInt("Charge", charge.progress());
        tag.putLong("Anchor", charge.anchor());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownership.load(tag.hasUUID("Owner") ? tag.getUUID("Owner") : null, tag.getBoolean("Locked"));
        ownerName = tag.getString("OwnerName");
        charge.load(tag.getInt("Charge"), tag.contains("Anchor") ? tag.getLong("Anchor") : Charge.NO_ANCHOR);
    }
}
