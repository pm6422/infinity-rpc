package org.infinity.rpc.utilities.id;

import org.infinity.rpc.utilities.id.sequence.ShortSequence;
import org.infinity.rpc.utilities.id.sequence.SnowFlakeSequence;
import org.infinity.rpc.utilities.id.sequence.TimestampSequence;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class IdGenerator {
    private IdGenerator() {
    }

    // 毫秒内固定起始值开始
    private static final SnowFlakeSequence SNOW_FLAKE_SEQUENCE = new SnowFlakeSequence(1L, false, false);
    private static final ShortSequence     shortSequence       = new ShortSequence();

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return
     */
    public static long generateSnowFlakeId() {
        return SNOW_FLAKE_SEQUENCE.nextId();
    }

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return
     */
    public static long generateTimestampId() {
        return TimestampSequence.nextId();
    }

    /**
     * Non-thread-safe
     * Can Not guarantee unique on multi-threads environment
     *
     * @return
     */
    public static long generateShortId() {
        return shortSequence.nextId();
    }
}
