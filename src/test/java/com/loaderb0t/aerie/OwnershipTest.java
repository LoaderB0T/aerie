package com.loaderb0t.aerie;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.loaderb0t.aerie.Ownership.Result.*;
import static org.junit.jupiter.api.Assertions.*;

class OwnershipTest {
    final UUID a = UUID.randomUUID(), b = UUID.randomUUID();

    @Test
    void firstClickClaims() {
        var o = new Ownership();
        assertEquals(CLAIMED, o.use(a, false, false));
        assertEquals(a, o.owner());
    }

    @Test
    void ownerClickIsStatus() {
        var o = new Ownership();
        o.use(a, false, false);
        assertEquals(STATUS, o.use(a, false, false));
    }

    @Test
    void otherClaimsWhenUnlocked() {
        var o = new Ownership();
        o.use(a, false, false);
        assertEquals(CLAIMED, o.use(b, false, false));
        assertEquals(b, o.owner());
    }

    @Test
    void ownerShiftLocks() {
        var o = new Ownership();
        o.use(a, false, false);
        assertEquals(LOCKED, o.use(a, true, false));
        assertTrue(o.locked());
    }

    @Test
    void lockedDeniesOthersEvenOp() {
        var o = new Ownership();
        o.use(a, false, false);
        o.use(a, true, false);
        assertEquals(DENIED, o.use(b, false, false));
        assertEquals(DENIED, o.use(b, false, true));
        assertEquals(a, o.owner());
    }

    @Test
    void nonOpShiftDenied() {
        var o = new Ownership();
        o.use(a, false, false);
        assertEquals(DENIED, o.use(b, true, false));
    }

    @Test
    void opShiftUnlocksThenOtherClaimsThenOpLocks() {
        var o = new Ownership();
        o.use(a, false, false);
        o.use(a, true, false);
        assertEquals(UNLOCKED, o.use(b, true, true));
        UUID c = UUID.randomUUID();
        assertEquals(CLAIMED, o.use(c, false, false));
        assertEquals(LOCKED, o.use(b, true, true));
        assertEquals(c, o.owner());
    }

    @Test
    void shiftOnUnownedIsStatus() {
        assertEquals(STATUS, new Ownership().use(a, true, true));
    }
}
