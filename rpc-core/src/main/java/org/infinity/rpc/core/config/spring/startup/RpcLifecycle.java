package org.infinity.rpc.core.config.spring.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapperHolder;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.ShutdownHook;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to start and stop the RPC server
 */
@Slf4j
public class RpcLifecycle {
    /**
     * The start flag used to identify whether the RPC server already started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    /**
     * The stop flag used to identify whether the RPC server already stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Prevent instantiation of it outside the class
     */
    private RpcLifecycle() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link RpcLifecycle}
     */
    public static RpcLifecycle getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        // static variable will be instantiated on class loading.
        private static final RpcLifecycle INSTANCE = new RpcLifecycle();
    }

    public AtomicBoolean getStarted() {
        return started;
    }

    public AtomicBoolean getStopped() {
        return stopped;
    }

    /**
     * Start the RPC server
     *
     * @param infinityProperties RPC configuration properties
     */
    public void start(InfinityProperties infinityProperties, List<Url> registryUrls) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the RPC server");
        initConfig(infinityProperties);
        registerShutdownHook();
        registerApplication(infinityProperties, registryUrls);
        registerProviders(infinityProperties, registryUrls);
        // referProviders();
        log.info("Started the RPC server");
    }

    /**
     * Initialize the RPC server
     */
    private void initConfig(InfinityProperties infinityProperties) {
    }

    /**
     * Register the shutdown hook to system runtime
     */
    private void registerShutdownHook() {
        ShutdownHook.register();
    }

    /**
     * Register application information to registry
     *
     * @param infinityProperties configuration properties
     * @param registryUrls       registry urls
     */
    private void registerApplication(InfinityProperties infinityProperties, List<Url> registryUrls) {
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactory = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactory.getRegistry(registryUrl);
            registry.registerApplication(infinityProperties.getApplication().toApp());
        }
        log.debug("Registered RPC server application [{}] to registry", infinityProperties.getApplication().getName());
    }

    /**
     * Register RPC providers to registry
     *
     * @param infinityProperties RPC configuration properties
     */
    private void registerProviders(InfinityProperties infinityProperties, List<Url> registryUrls) {
        Map<String, ProviderWrapper<?>> wrappers = ProviderWrapperHolder.getInstance().getWrappers();
        if (MapUtils.isEmpty(wrappers)) {
            log.info("No RPC service providers found for registering to registry!");
            return;
        }
        // TODO: consider using the async thread pool to speed up the startup process
        wrappers.forEach((name, providerWrapper) -> {
            Url providerUrl = createProviderUrl(infinityProperties, providerWrapper);
            providerWrapper.setUrl(providerUrl);
            // DO the providers registering
            providerWrapper.register(infinityProperties.getApplication().toApp(), registryUrls, providerUrl);
        });
    }

    /**
     * Create provider url
     *
     * @param infinityProperties configuration properties
     * @param providerWrapper    provider instance wrapper
     * @return provider url
     */
    private Url createProviderUrl(InfinityProperties infinityProperties, ProviderWrapper<?> providerWrapper) {
        Url providerUrl = Url.providerUrl(
                infinityProperties.getProtocol().getName().getValue(),
                infinityProperties.getProtocol().getPort(),
                providerWrapper.getInterfaceName());

        // Configure url at global level
        providerUrl.addParameter(Url.PARAM_APP, infinityProperties.getApplication().getName());
        providerUrl.addParameter(Url.PARAM_CHECK_HEALTH_FACTORY, infinityProperties.getProtocol().getCheckHealthFactory());

        // Configure url at provider level
        providerUrl.addParameter(Url.PARAM_CHECK_HEALTH, String.valueOf(providerWrapper.isCheckHealth()));
        providerUrl.addParameter(Url.PARAM_MAX_RETRIES, String.valueOf(providerWrapper.getMaxRetries()));
        return providerUrl;
    }

    /**
     * Stop the RPC server
     *
     * @param rpcProperties RPC configuration properties
     */
    public void destroy(InfinityProperties rpcProperties, List<Url> registryUrls) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }
//        unregisterApplication(registryUrls);
        unregisterProviders(registryUrls);
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders(List<Url> registryUrls) {
        ProviderWrapperHolder.getInstance().getWrappers().forEach((name, providerWrapper) -> providerWrapper.unregister(registryUrls));
    }
}
