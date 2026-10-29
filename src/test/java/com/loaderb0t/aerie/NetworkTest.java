package com.loaderb0t.aerie;

import com.loaderb0t.aerie.Network.CoreNode;
import com.loaderb0t.aerie.Network.RelayNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {
    final UUID x = UUID.randomUUID(), y = UUID.randomUUID();
    final Map<Long, Long> bindings = new HashMap<>();

    static long ck(int cx, int cz) {
        return Network.chunkKey(cx, cz);
    }

    // The chunk key doubles as a unique block position: one core or relay per chunk in these tests.
    static CoreNode core(int cx, int cz, UUID owner, boolean active) {
        return new CoreNode(ck(cx, cz), ck(cx, cz), owner, active);
    }

    static RelayNode relay(int cx, int cz) {
        return new RelayNode(ck(cx, cz), ck(cx, cz), true);
    }

    Network.Result run(List<CoreNode> cores, List<RelayNode> relays) {
        return Network.recompute(cores, relays, bindings, 5, true);
    }

    @Test
    void coreCoversOwnChunk() {
        var r = run(List.of(core(0, 0, x, true)), List.of());
        assertEquals(Set.of(ck(0, 0)), r.flightChunks().get(x));
    }

    @Test
    void adjacentRelayCovers() {
        var r = run(List.of(core(0, 0, x, true)), List.of(relay(1, 0)));
        assertEquals(Set.of(ck(0, 0), ck(1, 0)), r.flightChunks().get(x));
        assertTrue(r.connectedRelays().contains(ck(1, 0)));
    }

    @Test
    void gapIsNotBridged() {
        var r = run(List.of(core(0, 0, x, true)), List.of(relay(2, 0)));
        assertEquals(Set.of(ck(0, 0)), r.flightChunks().get(x));
        assertFalse(r.connectedRelays().contains(ck(2, 0)));
        assertFalse(bindings.containsKey(ck(2, 0)));
    }

    @Test
    void chainCovers() {
        var r = run(List.of(core(0, 0, x, true)), List.of(relay(3, 0), relay(1, 0), relay(2, 0)));
        assertEquals(Set.of(ck(0, 0), ck(1, 0), ck(2, 0), ck(3, 0)), r.flightChunks().get(x));
    }

    @Test
    void diagonalToggle() {
        var cores = List.of(core(0, 0, x, true));
        var relays = List.of(relay(1, 1));
        assertTrue(Network.recompute(cores, relays, new HashMap<>(), 5, true).flightChunks().get(x).contains(ck(1, 1)));
        assertFalse(Network.recompute(cores, relays, new HashMap<>(), 5, false).flightChunks().get(x).contains(ck(1, 1)));
    }

    @Test
    void maxDistanceStopsChain() {
        List<RelayNode> relays = new ArrayList<>();
        for (int i = 1; i <= 6; i++) relays.add(relay(i, 0));
        var covered = run(List.of(core(0, 0, x, true)), relays).flightChunks().get(x);
        for (int i = 0; i <= 5; i++) assertTrue(covered.contains(ck(i, 0)), "chunk " + i);
        assertFalse(covered.contains(ck(6, 0)));
    }

    @Test
    void inactiveCoreStillBinds() {
        var r = run(List.of(core(0, 0, x, false)), List.of(relay(1, 0)));
        assertFalse(r.flightChunks().containsKey(x));
        assertEquals(ck(0, 0), bindings.get(ck(1, 0)));
    }

    @Test
    void relayBelongsToFirstCore() {
        var r = run(List.of(core(0, 0, x, true), core(2, 0, y, true)), List.of(relay(1, 0)));
        assertEquals(ck(0, 0), bindings.get(ck(1, 0)));
        assertEquals(Set.of(ck(2, 0)), r.flightChunks().get(y));
    }

    @Test
    void boundRelayNotStolen() {
        // B at (0,0) sorts before A at (2,0), but the relay was already bound to A.
        bindings.put(ck(1, 0), ck(2, 0));
        var r = run(List.of(core(2, 0, x, true), core(0, 0, y, true)), List.of(relay(1, 0)));
        assertEquals(Set.of(ck(0, 0)), r.flightChunks().get(y));
        assertTrue(r.flightChunks().get(x).contains(ck(1, 0)));
    }

    @Test
    void rebindsWhenCoreGone() {
        bindings.put(ck(1, 0), ck(0, 0));
        var r = run(List.of(core(2, 0, y, true)), List.of(relay(1, 0)));
        assertEquals(ck(2, 0), bindings.get(ck(1, 0)));
        assertTrue(r.flightChunks().get(y).contains(ck(1, 0)));
    }

    @Test
    void incompleteRelayNeitherCoversNorBridges() {
        var incomplete = new RelayNode(ck(1, 0), ck(1, 0), false);
        var r = run(List.of(core(0, 0, x, true)), List.of(incomplete, relay(2, 0)));
        assertEquals(Set.of(ck(0, 0)), r.flightChunks().get(x));
    }

    @Test
    void brokenChainKeepsBindingAndRestores() {
        var a = core(0, 0, x, true);
        var b = core(3, 0, y, true);
        run(List.of(a), List.of(relay(1, 0), relay(2, 0)));
        assertEquals(ck(0, 0), bindings.get(ck(2, 0)));

        var broken = run(List.of(a, b), List.of(relay(2, 0)));
        assertEquals(ck(0, 0), bindings.get(ck(2, 0)));
        assertEquals(Set.of(ck(0, 0)), broken.flightChunks().get(x));
        assertEquals(Set.of(ck(3, 0)), broken.flightChunks().get(y));

        var restored = run(List.of(a, b), List.of(relay(1, 0), relay(2, 0)));
        assertEquals(Set.of(ck(0, 0), ck(1, 0), ck(2, 0)), restored.flightChunks().get(x));
    }

    @Test
    void sameOwnerTwoCoresUnion() {
        var first = core(0, 0, x, true);
        var second = core(3, 0, x, true);
        assertEquals(Set.of(ck(0, 0), ck(3, 0)), run(List.of(first, second), List.of()).flightChunks().get(x));
        assertEquals(Set.of(ck(3, 0)), run(List.of(second), List.of()).flightChunks().get(x));
    }

    @Test
    void negativeCoordinatesChainAcrossOrigin() {
        var r = run(List.of(core(-1, -1, x, true)), List.of(relay(0, -1), relay(1, -1)));
        assertEquals(Set.of(ck(-1, -1), ck(0, -1), ck(1, -1)), r.flightChunks().get(x));
        assertEquals(-7, Network.chunkX(ck(-7, 12)));
        assertEquals(12, Network.chunkZ(ck(-7, 12)));
    }
}
