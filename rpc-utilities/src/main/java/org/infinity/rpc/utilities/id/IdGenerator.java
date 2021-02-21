package org.infinity.rpc.utilities.id;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class IdGenerator {
    private static final ShortIdGenerator     SHORT_ID_GENERATOR      = new ShortIdGenerator();
    /**
     * 毫秒内固定起始值开始
     */
    private static final SnowFlakeIdGenerator SNOW_FLAKE_ID_GENERATOR = new SnowFlakeIdGenerator(1L, false, false);

    private IdGenerator() {
    }

    /**
     * Generate a thread-safe digit format ID
     *
     * @return 18 bits length，e.g：317297928250941551
     */
    public static long generateSnowFlakeId() {
        return SNOW_FLAKE_ID_GENERATOR.nextId();
    }

    /**
     * Generate a thread-safe digit format ID
     *
     * @return 19 bits length，e.g：1672888135850179037
     */
    public static long generateTimestampId() {
        return TimestampIdGenerator.nextId();
    }

    /**
     * Generate a thread-safe digit format ID
     *
     * @return 12 bits length，e.g：306554419571
     */
    public static long generateShortId() {
        return SHORT_ID_GENERATOR.nextId();
    }

}
