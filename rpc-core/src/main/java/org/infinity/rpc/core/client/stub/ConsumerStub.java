package org.infinity.rpc.core.client.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.client.annotation.RpcConsumer;
import org.infinity.rpc.core.client.invoker.ServiceInvoker;
import org.infinity.rpc.core.client.listener.ProviderDiscoveryListener;
import org.infinity.rpc.core.client.listener.ProviderNotifyListener;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.proxy.impl.JdkProxy;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.ConsumerConfig;
import org.infinity.rpc.core.config.impl.ProtocolConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.name.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.utilities.network.AddressUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.constant.ApplicationConstants.APP;
import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.RegistryConstants.REGISTRY_VAL_NONE;

/**
 * RPC consumer stub
 * A stub in distributed computing is a piece of code that converts parameters passed between client and server
 * during a remote procedure call(RPC).
 * A consumer stub take charge of creating a proxy instance of the consumer interface class, any method invocation
 * will be delegated to the proxy instance.
 *
 * @param <T>: The interface class of the consumer
 */
@Slf4j
@Setter
@Getter
public class ConsumerStub<T> {

    public static final Map<String, String> OPTIONS = new LinkedHashMap<>();

    {
        OPTIONS.put(FORM, StringUtils.EMPTY);
        OPTIONS.put(VERSION, StringUtils.EMPTY);
        OPTIONS.put(APP, StringUtils.EMPTY);
        OPTIONS.put(SERIALIZER, StringUtils.EMPTY);
        OPTIONS.put(REQUEST_TIMEOUT, StringUtils.EMPTY);
        OPTIONS.put(RETRY_COUNT, StringUtils.EMPTY);
        OPTIONS.put(MAX_PAYLOAD, StringUtils.EMPTY);
        OPTIONS.put(THROW_EXCEPTION, StringUtils.EMPTY);
    }

    /**
     * Provider stub bean name
     */
    @NotEmpty(message = "The [beanName] property must NOT be null!")
    private           String         beanName;
    /**
     * The interface class of the consumer
     * It can be null if it is a generic call.
     */
    private           Class<T>       interfaceClass;
    /**
     * The provider interface fully-qualified name
     */
    @NotEmpty(message = "The [interfaceName] property of @Consumer must NOT be empty!")
    private           String         interfaceName;
    /**
     * Protocol
     */
    @NotEmpty(message = "The [protocol] property of @Consumer must NOT be empty!")
    private           String         protocol;
    /**
     * Serializer used to serialize and deserialize object
     */
    private           String         serializer;
    /**
     * One service interface may have multiple implementations(forms),
     * It used to distinguish between different implementations of service provider interface
     */
    private           String         form;
    /**
     * When the service changes, such as adding or deleting methods, and interface parameters change,
     * the provider and consumer application instances need to be upgraded.
     * In order to deploy in a production environment without affecting user use,
     * a gradual migration scheme is generally adopted.
     * First upgrade some provider application instances,
     * and then use the same version number to upgrade some consumer instances.
     * The old version of the consumer instance calls the old version of the provider instance.
     * Observe that there is no problem and repeat this process to complete the upgrade.
     */
    private           String         version;
//    /**
//     * 多注册中心，所以不能使用单个
//     * Registry
//     */
//    private String   registry;
    /**
     * Service provider invoker name used to create {@link #invokerInstance}
     */
    @NotEmpty(message = "The [invoker] property of @Consumer must NOT be empty!")
    private           String         invoker;
    /**
     * The service provider invoker instance
     */
    private           ServiceInvoker invokerInstance;
    /**
     * Consumer proxy name used to create {@link #proxyInstance} which is the implementation of consumer interface class
     */
    @NotEmpty(message = "The [proxy] property of @Consumer must NOT be empty!")
    private           String         proxy;
    /**
     * The consumer proxy instance, refer the return type of {@link JdkProxy#getProxy(ConsumerStub)}
     * Disable deserialization
     */
    private transient T              proxyInstance;
    /**
     *
     */
    @NotEmpty(message = "The [faultTolerance] property of @Consumer must NOT be empty!")
    private           String         faultTolerance;
    /**
     *
     */
    @NotEmpty(message = "The [loadBalancer] property of @Consumer must NOT be empty!")
    private           String         loadBalancer;
    /**
     *
     */
    @Min(value = 0, message = "The [timeout] property of @Consumer must NOT be a positive number!")
    private           Integer        requestTimeout;
    /**
     * The max retry count of RPC request
     */
    @Min(value = 0, message = "The [retryCount] property of @Consumer must NOT be a positive number!")
    @Max(value = 10, message = "The [retryCount] property of @Consumer must NOT be bigger than 10!")
    private           Integer        retryCount;
    /**
     * The max request message payload size in bytes
     */
    @Min(value = 0, message = "The [maxPayload] property of @Consumer must NOT be a positive number!")
    private           Integer        maxPayload;
    /**
     * Indicates whether rate limit enabled or not
     */
    private           boolean        limitRate;
    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     */
    private           String         providerAddresses;
    /**
     * The consumer url used to export to registry only for consumers discovery management,
     * but it have nothing to do with the service calling.
     */
    private           Url            url;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     */
    @PostConstruct
    public void init() {
        this.proxyInstance = Proxy.getInstance(defaultIfEmpty(proxy, PROXY_VAL_DEFAULT)).getProxy(this);
        if (StringUtils.isNotEmpty(beanName)) {
            // Automatically add {@link ConsumerStub} instance to {@link ConsumerStubHolder}
            ConsumerStubHolder.getInstance().add(beanName, this);
        }
    }

    /**
     * Subscribe the RPC providers from registry
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registry          registry
     */
    public void subscribeProviders(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, RegistryConfig registry) {
        subscribeProviders(applicationConfig, protocolConfig, Collections.singleton(registry), null);
    }

    /**
     * Subscribe the RPC providers from registry
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registry          registry
     * @param providerProcessor provider processor
     */
    public void subscribeProviders(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                                   RegistryConfig registry, ProviderProcessable providerProcessor) {
        subscribeProviders(applicationConfig, protocolConfig, Collections.singleton(registry), providerProcessor);
    }


    /**
     * Subscribe the RPC providers from registry
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registries        registries
     */
    public void subscribeProviders(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                                   Collection<RegistryConfig> registries) {
        subscribeProviders(applicationConfig, protocolConfig, registries, null);
    }

    /**
     * Subscribe the RPC providers from multiple registries
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registries        registries
     * @param providerProcessor provider processor
     */
    public void subscribeProviders(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                                   Collection<RegistryConfig> registries, ProviderProcessable providerProcessor) {
        List<Url> registryUrls = registries
                .stream()
                .map(RegistryConfig::getRegistryUrl)
                .collect(Collectors.toList());

        // Create consumer url
        url = this.createConsumerUrl(applicationConfig, protocolConfig);

        // Initialize service invoker before consumer initialization
        invokerInstance = ServiceInvoker.getInstance(defaultIfEmpty(invoker, INVOKER_VAL_DEFAULT));

        invokerInstance.init(interfaceName,
                defaultIfEmpty(faultTolerance, FAULT_TOLERANCE_VAL_DEFAULT),
                defaultIfEmpty(loadBalancer, LOAD_BALANCER_VAL_DEFAULT), url);

        if (StringUtils.isEmpty(providerAddresses)) {
            // Non-direct registry
            // Pass service provider invoker to listener, listener will update service invoker after provider urls changed
            ProviderDiscoveryListener listener = ProviderDiscoveryListener.of(invokerInstance, interfaceName, form, url, providerProcessor);
            listener.subscribe(registryUrls);
            return;
        }

        // Direct registry
        notifyDirectProviderUrls(protocolConfig, registryUrls, providerProcessor);
    }

    /**
     * Merge high priority properties to consumer stub and generate consumer url
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @return consumer url
     */
    private Url createConsumerUrl(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig) {
        url = Url.consumerUrl(defaultIfEmpty(protocol, PROTOCOL_VAL_DEFAULT),
                protocolConfig.getHost(), protocolConfig.getPort(), interfaceName, form, version);
        url.addOption(APP, applicationConfig.getName());
        url.addOption(SERIALIZER, serializer);
        url.addOption(REQUEST_TIMEOUT, requestTimeout);
        url.addOption(RETRY_COUNT, retryCount);
        url.addOption(MAX_PAYLOAD, maxPayload);

        String throwException = protocolConfig.getThrowException() == null ? null : protocolConfig.getThrowException().toString();
        url.addOption(THROW_EXCEPTION, throwException);
        return url;
    }

    private void notifyDirectProviderUrls(ProtocolConfig protocolConfig, List<Url> globalRegistryUrls,
                                          ProviderProcessable providerProcessor) {
        // Pass provider service invoker to listener, listener will update service invoker after provider urls changed
        ProviderNotifyListener listener = ProviderNotifyListener.of(invokerInstance, protocol, interfaceName, form, providerProcessor);

        for (Url globalRegistryUrl : globalRegistryUrls) {
            List<Url> directProviderUrls = createDirectProviderUrls(protocolConfig);
            Url directRegistryUrl = globalRegistryUrl.copy();
            // Change protocol to direct
            directRegistryUrl.setProtocol(REGISTRY_VAL_NONE);
            // 如果有directUrls，直接使用这些directUrls进行初始化，不用到注册中心discover
            // Directly notify the provider urls
            listener.onNotify(directRegistryUrl, directProviderUrls);
            log.info("Notified registries [{}] with direct provider urls {}", directRegistryUrl, directProviderUrls);
        }
    }

    private List<Url> createDirectProviderUrls(ProtocolConfig protocolConfig) {
        // Get the provider host and port
        List<Pair<String, Integer>> directUrlHostPortList = AddressUtils.parseAddress(providerAddresses);
        List<Url> directProviderUrls = new ArrayList<>(directUrlHostPortList.size());
        for (Pair<String, Integer> providerHostPortPair : directUrlHostPortList) {
            // Note: There are no extra options added to the direct provider url
            Url providerUrl = Url.providerUrl(PROTOCOL_VAL_INFINITY, providerHostPortPair.getLeft(),
                    providerHostPortPair.getRight(), interfaceName, form, version);
            directProviderUrls.add(providerUrl);
        }
        return directProviderUrls;
    }

    /**
     * Build the consumer stub bean name
     *
     * @param interfaceClassName the consumer service interface name
     * @param attributes         {@link RpcConsumer annotation attributes}
     * @return The name of bean that annotated {@link RpcConsumer @Consumer} in spring context
     */
    public static String buildConsumerStubBeanName(String interfaceClassName, Map<String, Object> attributes) {
        return ConsumerStubBeanNameBuilder
                .builder(interfaceClassName)
                .attributes(attributes)
                .build();
    }

    public static ConsumerStub<?> create(String interfaceName, ApplicationConfig application,
                                         RegistryConfig registry, ProtocolConfig protocol, ConsumerConfig consumer,
                                         ProviderProcessable providerProcessor) {
        return create(interfaceName, application, registry, protocol, consumer, providerProcessor,
                null, null, null, null, null, null);
    }

    public static ConsumerStub<?> create(String interfaceName, ApplicationConfig application,
                                         RegistryConfig registry, ProtocolConfig protocol, ConsumerConfig consumer,
                                         ProviderProcessable providerProcessor, String providerAddresses) {
        return create(interfaceName, application, registry, protocol, consumer, providerProcessor,
                providerAddresses, null, null, null, null, null);
    }

    public static ConsumerStub<?> create(String interfaceName, ApplicationConfig application,
                                         RegistryConfig registry, ProtocolConfig protocol, ConsumerConfig consumer,
                                         ProviderProcessable providerProcessor, String providerAddresses,
                                         String form, String version, Integer requestTimeout,
                                         Integer retryCount) {
        return create(interfaceName, application, registry, protocol, consumer, providerProcessor,
                providerAddresses, form, version, requestTimeout, retryCount, null);
    }

    public static ConsumerStub<?> create(String interfaceName, ApplicationConfig application,
                                         RegistryConfig registry, ProtocolConfig protocol, ConsumerConfig consumer,
                                         ProviderProcessable providerProcessor, String providerAddresses,
                                         String form, String version, Integer requestTimeout,
                                         Integer retryCount, String serializer) {
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceName(interfaceName);
        consumerStub.setSerializer(serializer);
        consumerStub.setProviderAddresses(providerAddresses);
        consumerStub.setForm(form);
        consumerStub.setVersion(version);
        consumerStub.setRequestTimeout(requestTimeout);
        consumerStub.setRetryCount(retryCount);
        // Must NOT call init()
        consumerStub.subscribeProviders(application, protocol, registry, providerProcessor);
        return consumerStub;
    }
}
