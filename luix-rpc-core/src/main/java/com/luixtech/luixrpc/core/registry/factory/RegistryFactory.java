package com.luixtech.luixrpc.core.registry.factory;


import com.luixtech.luixrpc.core.registry.Registry;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.utilities.serviceloader.ServiceLoader;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.Spi;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiScope;

/**
 * Registry factory used to create registry
 */
@Spi(scope = SpiScope.SINGLETON)
public interface RegistryFactory {

    /**
     * Get or create a registry
     *
     * @param registryUrl registry url
     * @return specified registry instance
     */
    Registry getRegistry(Url registryUrl);

    /**
     * Create a registry
     *
     * @param registryUrl registry URL
     * @return specified registry instance
     */
    Registry createRegistry(Url registryUrl);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static RegistryFactory getInstance(String name) {
        return ServiceLoader.forClass(RegistryFactory.class).load(name);
    }
}
