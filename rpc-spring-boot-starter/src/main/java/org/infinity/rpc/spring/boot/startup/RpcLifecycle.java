package org.infinity.rpc.spring.boot.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.server.stub.ProviderStubHolder;
import org.infinity.rpc.core.switcher.impl.SwitcherHolder;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static org.infinity.rpc.core.constant.RegistryConstants.REGISTRY_VAL_NONE;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.core.server.stub.ProviderStub.buildProviderStubBeanName;

/**
 * Used to start and stop the RPC server
 */
@Slf4j
public class RpcLifecycle {
    private static final RpcLifecycle  INSTANCE = new RpcLifecycle();
    /**
     * Indicates whether the RPC server already started or not
     */
    private final        AtomicBoolean started  = new AtomicBoolean(false);
    /**
     * Indicates whether the RPC server already stopped or not
     */
    private final        AtomicBoolean stopped  = new AtomicBoolean(false);

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
        return INSTANCE;
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
     * @param beanFactory        bean factory
     * @param infinityProperties RPC configuration properties
     */
    public void start(DefaultListableBeanFactory beanFactory, InfinityProperties infinityProperties) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the RPC server");
        registerShutdownHook();
        registerBuildInProviderStubs(beanFactory, infinityProperties);
        publish(infinityProperties);
        subscribe(infinityProperties);
        log.info("Started the RPC server");
    }


    /**
     * Register the shutdown hook to system runtime
     */
    private void registerShutdownHook() {
        ShutdownHook.register();
    }

    /**
     * Register build-in provider stubs
     *
     * @param beanFactory        bean factory
     * @param infinityProperties RPC configuration properties
     */
    private void registerBuildInProviderStubs(DefaultListableBeanFactory beanFactory,
                                              InfinityProperties infinityProperties) {
        String beanName = buildProviderStubBeanName(BuildInService.class.getName());
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderStub.class);
        builder.addPropertyValue(BEAN_NAME, beanName);
        builder.addPropertyValue(INTERFACE_CLASS, BuildInService.class);
        builder.addPropertyValue(INTERFACE_NAME, BuildInService.class.getName());
        builder.addPropertyValue(PROTOCOL, infinityProperties.getProtocol().getName());
        builder.addPropertyReference("instance", StringUtils.uncapitalize(BuildInService.class.getSimpleName()));

        beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
        // Method getBean() will trigger bean initialization
        beanFactory.getBean(beanName, ProviderStub.class);
    }

    /**
     * Publish RPC providers to registries
     *
     * @param infinityProperties RPC configuration properties
     */
    private void publish(InfinityProperties infinityProperties) {
        infinityProperties.getRegistryList().forEach(registryConfig -> {
            if (!registryConfig.getName().equals(REGISTRY_VAL_NONE)) {
                // Non-direct registry

                // Publish providers
                publishProviders(infinityProperties, registryConfig);
            }
        });
    }

    private void publishProviders(InfinityProperties infinityProperties, RegistryConfig registryConfig) {
        Map<String, ProviderStub<?>> providerStubs = ProviderStubHolder.getInstance().get();
        if (MapUtils.isEmpty(providerStubs)) {
            log.info("No RPC service providers found to register to registry [{}]", registryConfig.getName());
            return;
        }
        providerStubs.forEach((name, providerStub) -> {
            // Register providers
            providerStub.register(infinityProperties.getApplication(), infinityProperties.getAvailableProtocol(), registryConfig);
        });

        if (infinityProperties.getProvider().isAutoExpose()) {
            // Activate RPC service providers
            SwitcherHolder.getInstance().setValue(SwitcherHolder.SERVICE_ACTIVE, true);
        }
    }

    /**
     * Subscribe provider from registries
     *
     * @param infinityProperties RPC configuration properties
     */
    private void subscribe(InfinityProperties infinityProperties) {
        Map<String, ConsumerStub<?>> consumerStubs = ConsumerStubHolder.getInstance().get();
        if (MapUtils.isEmpty(consumerStubs)) {
            log.info("No RPC consumers found on the registries");
            return;
        }
        consumerStubs.forEach((name, consumerStub) ->
                consumerStub.subscribeProviders(infinityProperties.getApplication(),
                        infinityProperties.getAvailableProtocol(),
                        infinityProperties.getRegistryList()));
    }

    /**
     * Stop the RPC server
     *
     * @param infinityProperties RPC configuration properties
     */
    public void destroy(InfinityProperties infinityProperties) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }

        infinityProperties.getRegistryList().forEach(registryConfig -> {
            //unregisterApplication(registryUrls);
            unregisterProviders(registryConfig.getRegistryUrl());
        });
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.getRegisteredProviderUrls().forEach(registry::unregister);
            log.debug("Unregistered all the RPC providers from registry [{}]", registryUrl.getProtocol());
        }
    }
}
