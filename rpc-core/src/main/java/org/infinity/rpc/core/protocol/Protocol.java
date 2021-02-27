package org.infinity.rpc.core.protocol;

import org.infinity.rpc.core.client.request.ProviderCaller;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol {

    /**
     * Create provider caller
     *
     * @param interfaceName provider interface name
     * @param providerUrl   provider url
     * @param <T>           provider instance
     * @return provider caller
     */
    <T> ProviderCaller<T> createProviderCaller(String interfaceName, Url providerUrl);

    /**
     * 暴露服务
     *
     * @param <T>          provider interface
     * @param providerStub provider stub
     * @return exporter
     */
    <T> Exportable<T> export(ProviderStub<T> providerStub);

    /**
     * Destroy
     */
    void destroy();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static Protocol getInstance(String name) {
        return ServiceLoader.forClass(Protocol.class).load(name);
    }
}
