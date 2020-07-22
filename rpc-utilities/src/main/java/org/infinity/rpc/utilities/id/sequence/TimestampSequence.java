package org.infinity.rpc.utilities.id.sequence;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 通过requestId能够知道大致请求的时间
 *
 * <pre>
 * 	目前是currentTimeMillis * (2^20) + offset.incrementAndGet()
 * 	通过requestId / (2^20 * 1000) 能够得到秒
 * </pre>
 */
@ThreadSafe
public class TimestampSequence {
    protected static final AtomicLong offset               = new AtomicLong(0);
    protected static final int        BITS                 = 20;
    protected static final long       MAX_COUNT_PER_MILLIS = 1 << BITS;

    /**
     * 获取 requestId
     *
     * @return
     */
    public static long nextId() {
        long currentTime = System.currentTimeMillis();
        long count = offset.incrementAndGet();
        while (count >= MAX_COUNT_PER_MILLIS) {
            synchronized (TimestampSequence.class) {
                if (offset.get() >= MAX_COUNT_PER_MILLIS) {
                    offset.set(0);
                }
            }
            count = offset.incrementAndGet();
        }
        return (currentTime << BITS) + count;
    }
}