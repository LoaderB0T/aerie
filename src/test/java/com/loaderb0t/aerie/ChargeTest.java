package com.loaderb0t.aerie;

import org.junit.jupiter.api.Test;

import static com.loaderb0t.aerie.Charge.Event.*;
import static org.junit.jupiter.api.Assertions.*;

class ChargeTest {
    @Test
    void chargesToCompletionOnce() {
        var c = new Charge();
        for (int i = 0; i < 14; i++) assertEquals(NONE, c.tick(20, true, false, 300));
        assertEquals(COMPLETED, c.tick(20, true, false, 300));
        assertTrue(c.charged(300));
        assertEquals(NONE, c.tick(20, true, false, 300));
    }

    @Test
    void incompleteResets() {
        var c = new Charge();
        c.tick(20, true, false, 300);
        assertEquals(RESET, c.tick(20, false, false, 300));
        assertEquals(0, c.progress());
    }

    @Test
    void incompleteFromZeroIsNone() {
        assertEquals(NONE, new Charge().tick(20, false, false, 300));
    }

    @Test
    void poweredPauses() {
        var c = new Charge();
        c.tick(20, true, false, 300);
        c.tick(20, true, true, 300);
        assertEquals(20, c.progress());
    }

    @Test
    void requiredTicksChangeKeepsProgress() {
        var c = new Charge();
        c.load(300, 0);
        assertTrue(c.charged(200));
        assertFalse(c.charged(400));
        assertEquals(COMPLETED, c.tick(100, true, false, 400));
        assertEquals(400, c.progress());
    }

    @Test
    void anchorMoveResets() {
        var c = new Charge();
        assertFalse(c.checkAnchor(5L));
        c.tick(20, true, false, 300);
        assertFalse(c.checkAnchor(5L));
        assertTrue(c.checkAnchor(9L));
        assertEquals(0, c.progress());
    }
}
