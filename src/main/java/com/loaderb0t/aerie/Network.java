package com.loaderb0t.aerie;

import java.util.*;

/** Which chunks each owner may fly in, and which Core each relay belongs to. Pure rules, no Minecraft types. */
public final class Network {
    /** owner may be null: an unowned Core covers nothing. */
    public record CoreNode(long pos, long chunk, UUID owner, boolean active) {}

    public record RelayNode(long pos, long chunk, boolean complete) {}

    public record Result(Map<UUID, Set<Long>> flightChunks, Set<Long> connectedRelays) {}

    private Network() {}

    /** Same layout as ChunkPos.asLong. */
    public static long chunkKey(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }

    public static int chunkX(long key) {
        return (int) key;
    }

    public static int chunkZ(long key) {
        return (int) (key >>> 32);
    }

    /** Mutates bindings (relayPos → corePos): keeps existing ones, binds newly reached free relays. */
    public static Result recompute(Collection<CoreNode> cores, Collection<RelayNode> relays,
                                   Map<Long, Long> bindings, int maxDistance, boolean diagonal) {
        Set<Long> corePositions = new HashSet<>();
        for (CoreNode c : cores) corePositions.add(c.pos());
        Set<Long> relayPositions = new HashSet<>();
        for (RelayNode r : relays) relayPositions.add(r.pos());
        bindings.entrySet().removeIf(e -> !relayPositions.contains(e.getKey()) || !corePositions.contains(e.getValue()));

        Map<Long, List<RelayNode>> relaysByChunk = new HashMap<>();
        for (RelayNode r : relays) {
            if (r.complete()) relaysByChunk.computeIfAbsent(r.chunk(), k -> new ArrayList<>()).add(r);
        }

        Map<UUID, Set<Long>> flight = new HashMap<>();
        Set<Long> connected = new HashSet<>();
        List<CoreNode> sorted = new ArrayList<>(cores);
        sorted.sort(Comparator.comparingLong(CoreNode::pos));

        for (CoreNode core : sorted) {
            Set<Long> reached = new HashSet<>();
            Deque<Long> queue = new ArrayDeque<>();
            reached.add(core.chunk());
            queue.add(core.chunk());
            connectRelaysIn(core.chunk(), core, relaysByChunk, bindings, connected);
            while (!queue.isEmpty()) {
                long chunk = queue.poll();
                for (long next : neighbours(chunk, diagonal)) {
                    if (reached.contains(next) || chebyshev(next, core.chunk()) > maxDistance) continue;
                    if (connectRelaysIn(next, core, relaysByChunk, bindings, connected)) {
                        reached.add(next);
                        queue.add(next);
                    }
                }
            }
            if (core.active() && core.owner() != null) {
                flight.computeIfAbsent(core.owner(), k -> new HashSet<>()).addAll(reached);
            }
        }
        return new Result(flight, connected);
    }

    /** Connects every relay in the chunk that is free or already this core's. Returns true if any was. */
    private static boolean connectRelaysIn(long chunk, CoreNode core, Map<Long, List<RelayNode>> relaysByChunk,
                                           Map<Long, Long> bindings, Set<Long> connected) {
        boolean any = false;
        for (RelayNode r : relaysByChunk.getOrDefault(chunk, List.of())) {
            Long bound = bindings.get(r.pos());
            if (bound != null && bound != core.pos()) continue;
            bindings.put(r.pos(), core.pos());
            connected.add(r.pos());
            any = true;
        }
        return any;
    }

    private static long[] neighbours(long chunk, boolean diagonal) {
        int x = chunkX(chunk), z = chunkZ(chunk);
        long[] straight = {chunkKey(x + 1, z), chunkKey(x - 1, z), chunkKey(x, z + 1), chunkKey(x, z - 1)};
        if (!diagonal) return straight;
        return new long[]{straight[0], straight[1], straight[2], straight[3],
                chunkKey(x + 1, z + 1), chunkKey(x + 1, z - 1), chunkKey(x - 1, z + 1), chunkKey(x - 1, z - 1)};
    }

    private static int chebyshev(long a, long b) {
        return Math.max(Math.abs(chunkX(a) - chunkX(b)), Math.abs(chunkZ(a) - chunkZ(b)));
    }
}
