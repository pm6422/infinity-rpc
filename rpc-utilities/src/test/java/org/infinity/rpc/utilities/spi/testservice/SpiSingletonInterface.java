package org.infinity.rpc.utilities.spi.testservice;


import org.infinity.rpc.utilities.spi.annotation.SpiScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = SpiScope.SINGLETON)
public interface SpiSingletonInterface {
    long spiHello();
}