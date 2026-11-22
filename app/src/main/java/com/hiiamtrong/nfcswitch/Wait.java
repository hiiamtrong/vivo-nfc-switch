package com.hiiamtrong.nfcswitch;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

final class Wait {
    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    private Wait() {}

    /** Polls {@code done} every {@code stepMs} until it is true or {@code timeoutMs} has passed on {@code clock}. */
    static boolean until(BooleanSupplier done, long timeoutMs, long stepMs, LongSupplier clock, Sleeper sleeper)
            throws InterruptedException {
        long end = clock.getAsLong() + timeoutMs;
        while (!done.getAsBoolean()) {
            if (clock.getAsLong() >= end) return false;
            sleeper.sleep(stepMs);
        }
        return true;
    }
}
