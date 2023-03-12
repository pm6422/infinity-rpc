package com.luixtech.rpc.spring.boot.starter.startup;

import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.client.stub.ConsumerStubHolder;
import com.luixtech.rpc.core.common.RpcMethod;
import com.luixtech.rpc.core.config.impl.RegistryConfig;
import com.luixtech.rpc.core.registry.Registry;
import com.luixtech.rpc.core.registry.factory.RegistryFactory;
import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.server.stub.MethodConfig;
import com.luixtech.rpc.core.server.stub.ProviderStub;
import com.luixtech.rpc.core.server.stub.ProviderStubHolder;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import com.luixtech.utilities.destory.ShutdownHook;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luixtech.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static com.luixtech.rpc.core.constant.RegistryConstants.REGISTRY_VAL_NONE;
import static com.luixtech.rpc.core.constant.ServiceConstants.*;
import static com.luixtech.rpc.core.server.stub.ProviderStub.buildProviderStubBeanName;
import static com.luixtech.rpc.core.utils.MethodParameterUtils.getMethodSignature;
import static com.luixtech.rpc.spring.boot.starter.utils.ProxyUtils.getTargetClass;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

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
     * @param beanFactory    bean factory
     * @param luixRpcProperties RPC configuration properties
     */
    public void start(DefaultListableBeanFactory beanFactory, LuixRpcProperties luixRpcProperties) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the Luix RPC server");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        registerShutdownHook();
        registerBuildInProviderStubs(beanFactory, luixRpcProperties);
        register(luixRpcProperties);
        subscribe(luixRpcProperties);
        stopWatch.stop();
        log.info("Started the Luix RPC server in {} ms", stopWatch.getTotalTimeMillis());
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
     * @param beanFactory    bean factory
     * @param luixRpcProperties RPC configuration properties
     */
    private void registerBuildInProviderStubs(DefaultListableBeanFactory beanFactory,
                                              LuixRpcProperties luixRpcProperties) {
        String beanName = buildProviderStubBeanName(BuildInService.class.getName());
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderStub.class);
        builder.addPropertyValue(BEAN_NAME, beanName);
        builder.addPropertyValue(INTERFACE_CLASS, BuildInService.class);
        builder.addPropertyValue(INTERFACE_NAME, BuildInService.class.getName());
        builder.addPropertyValue(PROTOCOL, luixRpcProperties.getProtocol().getName());
        builder.addPropertyReference("instance", StringUtils.uncapitalize(BuildInService.class.getSimpleName()));

        beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
        // Method getBean() will trigger bean initialization
        beanFactory.getBean(beanName, ProviderStub.class);
    }

    /**
     * Register RPC providers and consumers to registries
     *
     * @param luixRpcProperties RPC configuration properties
     */
    private void register(LuixRpcProperties luixRpcProperties) {
        luixRpcProperties.getRegistryList().forEach(registryConfig -> {
            if (!registryConfig.getName().equals(REGISTRY_VAL_NONE)) {
                // Non-direct registry

                // Register providers
                registerProviders(luixRpcProperties, registryConfig);
                // Register consumers
                registerConsumer(luixRpcProperties, registryConfig);
            }
        });
    }

    private void registerProviders(LuixRpcProperties luixRpcProperties, RegistryConfig registryConfig) {
        Map<String, ProviderStub<?>> providerStubs = ProviderStubHolder.getInstance().getMap();
        if (MapUtils.isEmpty(providerStubs)) {
            log.info("No RPC service providers found on registry [{}]", registryConfig.getName());
            return;
        }
        providerStubs.forEach((name, providerStub) -> {
            // Set method level configuration
            Arrays.stream(getTargetClass(providerStub.getInstance()).getMethods()).forEach(method ->
                    setMethodConfig(providerStub, method)
            );
            if (providerStub.getInterfaceClass() != null) {
                Arrays.stream(providerStub.getInterfaceClass().getMethods()).forEach(method ->
                        setMethodConfig(providerStub, method)
                );
            }

            // Register provider to registry
            providerStub.register(luixRpcProperties.getApplication(), luixRpcProperties.getAvailableProtocol(), registryConfig);
        });

        if (luixRpcProperties.getProvider().isAutoExpose()) {
            // Activate RPC service providers
            providerStubs.forEach((name, providerStub) ->
                    providerStub.activate()
            );
        }
    }

    private void registerConsumer(LuixRpcProperties luixRpcProperties, RegistryConfig registryConfig) {
        Map<String, ConsumerStub<?>> consumerStubs = ConsumerStubHolder.getInstance().getMap();
        if (MapUtils.isEmpty(consumerStubs)) {
            log.info("No RPC service consumers found on registry [{}]", registryConfig.getName());
            return;
        }
        consumerStubs.forEach((name, consumerStub) -> {
            // Register and activate consumer to registry
            consumerStub.registerAndActivate(luixRpcProperties.getApplication(), luixRpcProperties.getAvailableProtocol(),
                    registryConfig);
        });
    }

    private void setMethodConfig(ProviderStub<?> providerStub, Method method) {
        RpcMethod annotation = AnnotationUtils.getAnnotation(method, RpcMethod.class);
        if (annotation != null) {
            MethodConfig methodConfig = MethodConfig.builder()
                    .retryCount(defaultIfEmpty(annotation.retryCount(), null))
                    .requestTimeout(defaultIfEmpty(annotation.requestTimeout(), null))
                    .build();
            providerStub.getMethodConfig().putIfAbsent(getMethodSignature(method), methodConfig);
        }
    }

    /**
     * Subscribe provider from registries by consumer
     *
     * @param luixRpcProperties RPC configuration properties
     */
    private void subscribe(LuixRpcProperties luixRpcProperties) {
        Map<String, ConsumerStub<?>> consumerStubs = ConsumerStubHolder.getInstance().getMap();
        if (MapUtils.isEmpty(consumerStubs)) {
            return;
        }
        consumerStubs.forEach((name, consumerStub) -> {
            // Bind provider services discovery listener to consumer services
            consumerStub.subscribeProviders(luixRpcProperties.getApplication(), luixRpcProperties.getAvailableProtocol(),
                    luixRpcProperties.getRegistryList());
        });
    }

    /**
     * Stop the RPC server
     *
     * @param luixRpcProperties RPC configuration properties
     */
    public void destroy(LuixRpcProperties luixRpcProperties) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }

        luixRpcProperties.getRegistryList().forEach(registryConfig ->
                deregisterConsumers(registryConfig.getRegistryUrl())
        );

        luixRpcProperties.getRegistryList().forEach(registryConfig ->
                deregisterProviders(registryConfig.getRegistryUrl())
        );

        // Notes: debug breakpoint here does not work and log.info() does not work
        System.out.println("Stopped the RPC server");
    }


    /**
     * Deregister RPC providers from registry
     *
     * @param registryUrls registry urls
     */
    private void deregisterProviders(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                System.out.println("No registry found!");
                return;
            }
            registry.getRegisteredProviderUrls().forEach(registry::deregister);
            System.out.println("Deregistered all the RPC providers from registry [" + registryUrl.getProtocol() + "]");
        }
    }

    /**
     * Deregister RPC consumers from registry
     *
     * @param registryUrls registry urls
     */
    private void deregisterConsumers(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                System.out.println("No registry found!");
                return;
            }
            registry.getRegisteredConsumerUrls().forEach(registry::deregister);
            System.out.println("Deregistered all the RPC consumers from registry [" + registryUrl.getProtocol() + "]");
        }
    }
}
