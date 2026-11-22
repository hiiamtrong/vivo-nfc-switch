package com.hiiamtrong.nfcswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class WaitTest {
    @Test public void returnsTrueOnceTheConditionHolds() throws Exception {
        AtomicLong now = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();
        boolean ok = Wait.until(() -> calls.incrementAndGet() >= 3, 10_000, 500, now::get, ms -> now.addAndGet(ms));
        assertTrue(ok);
        assertEquals(1_000, now.get());
    }

    @Test public void givesUpAfterTheTimeout() throws Exception {
        AtomicLong now = new AtomicLong();
        boolean ok = Wait.until(() -> false, 2_000, 500, now::get, ms -> now.addAndGet(ms));
        assertFalse(ok);
        assertEquals(2_000, now.get());
    }

    @Test public void doesNotSleepWhenAlreadyDone() throws Exception {
        AtomicLong now = new AtomicLong();
        assertTrue(Wait.until(() -> true, 2_000, 500, now::get, ms -> now.addAndGet(ms)));
        assertEquals(0, now.get());
    }
}
