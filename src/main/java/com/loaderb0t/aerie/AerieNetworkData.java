package com.loaderb0t.aerie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Per-dimension registry of Cores and relays. Flight checks read this instead of block entities,
 * so they work whether or not the shrine's chunks are loaded.
 */
public final class AerieNetworkData extends SavedData {
    private record CoreRecord(@Nullable UUID owner, String ownerName, boolean active) {}

    private record RelayRecord(boolean complete, @Nullable UUID placer) {}

    private static final Factory<AerieNetworkData> FACTORY = new Factory<>(AerieNetworkData::new, AerieNetworkData::load, null);

    private final Map<Long, CoreRecord> cores = new HashMap<>();
    private final Map<Long, RelayRecord> relays = new HashMap<>();
    private final Map<Long, Long> bindings = new HashMap<>();

    private @Nullable Network.Result result;
    private int resultDistance;
    private boolean resultDiagonal;

    public static AerieNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "aerie_network");
    }

    public void putCore(BlockPos pos, @Nullable UUID owner, String ownerName, boolean active) {
        var record = new CoreRecord(owner, ownerName, active);
        if (!record.equals(cores.put(pos.asLong(), record))) invalidate();
    }

    public void removeCore(BlockPos pos) {
        if (cores.remove(pos.asLong()) != null) invalidate();
    }

    public void putRelay(BlockPos pos, boolean complete, @Nullable UUID placer) {
        var record = new RelayRecord(complete, placer);
        if (!record.equals(relays.put(pos.asLong(), record))) invalidate();
    }

    public void removeRelay(BlockPos pos) {
        if (relays.remove(pos.asLong()) != null) invalidate();
    }

    public boolean canFly(UUID player, long chunkKey) {
        return result().flightChunks().getOrDefault(player, Set.of()).contains(chunkKey);
    }

    public boolean isConnected(BlockPos relay) {
        return result().connectedRelays().contains(relay.asLong());
    }

    public @Nullable String boundOwnerName(BlockPos relay) {
        result();
        Long core = bindings.get(relay.asLong());
        CoreRecord record = core == null ? null : cores.get(core);
        return record == null ? null : record.ownerName();
    }

    private void invalidate() {
        result = null;
        setDirty();
    }

    private Network.Result result() {
        int distance = AerieConfig.MAX_RELAY_DISTANCE.get();
        boolean diagonal = AerieConfig.DIAGONAL_ADJACENCY.get();
        if (result != null && distance == resultDistance && diagonal == resultDiagonal) return result;

        List<Network.CoreNode> coreNodes = new ArrayList<>();
        cores.forEach((pos, c) -> coreNodes.add(new Network.CoreNode(pos, chunkOf(pos), c.owner(), c.active())));
        List<Network.RelayNode> relayNodes = new ArrayList<>();
        relays.forEach((pos, r) -> relayNodes.add(new Network.RelayNode(pos, chunkOf(pos), r.complete(), r.placer())));
        Map<Long, Long> before = new HashMap<>(bindings);
        result = Network.recompute(coreNodes, relayNodes, bindings, distance, diagonal);
        resultDistance = distance;
        resultDiagonal = diagonal;
        if (!before.equals(bindings)) setDirty();
        return result;
    }

    private static long chunkOf(long pos) {
        return ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag coreList = new ListTag();
        cores.forEach((pos, c) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", pos);
            if (c.owner() != null) t.putUUID("Owner", c.owner());
            t.putString("OwnerName", c.ownerName());
            t.putBoolean("Active", c.active());
            coreList.add(t);
        });
        ListTag relayList = new ListTag();
        relays.forEach((pos, r) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", pos);
            t.putBoolean("Complete", r.complete());
            if (r.placer() != null) t.putUUID("Placer", r.placer());
            Long core = bindings.get(pos);
            if (core != null) t.putLong("Core", core);
            relayList.add(t);
        });
        tag.put("Cores", coreList);
        tag.put("Relays", relayList);
        return tag;
    }

    private static AerieNetworkData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new AerieNetworkData();
        for (Tag raw : tag.getList("Cores", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            UUID owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
            data.cores.put(t.getLong("Pos"), new CoreRecord(owner, t.getString("OwnerName"), t.getBoolean("Active")));
        }
        for (Tag raw : tag.getList("Relays", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            data.relays.put(t.getLong("Pos"), new RelayRecord(t.getBoolean("Complete"), t.hasUUID("Placer") ? t.getUUID("Placer") : null));
            if (t.contains("Core")) data.bindings.put(t.getLong("Pos"), t.getLong("Core"));
        }
        return data;
    }
}
