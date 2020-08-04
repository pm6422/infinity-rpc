package org.infinity.rpc.utilities.spi.testservice;

import org.infinity.rpc.utilities.spi.annotation.NameAs;

import java.util.concurrent.atomic.AtomicLong;

@NameAs("spiPrototypeTest2")
public class SpiPrototypeTestImpl2 implements SpiPrototypeInterface {
    private static AtomicLong counter = new AtomicLong(0);
    private long index = 0;

    public SpiPrototypeTestImpl2() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        System.out.println("SpiPrototypeTestImpl_" + index + " say hello");
        return index;
    }

}
