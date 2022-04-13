package com.luixtech.luixrpc.core.client.stub;

import com.luixtech.luixrpc.core.config.impl.ApplicationConfig;
import com.luixtech.luixrpc.core.config.impl.ConsumerConfig;
import com.luixtech.luixrpc.core.config.impl.ProtocolConfig;
import com.luixtech.luixrpc.core.config.impl.RegistryConfig;
import com.luixtech.luixrpc.core.listener.GlobalProviderDiscoveryListener;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public abstract class ConsumerStubFactory {

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, null, null, null, null, null, null, null,
                null, null, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         String form) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, null, form, null, null, null, null, null,
                null, null, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String form,
                                         String version) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), providerAddresses, interfaceName,
                null, null, form, version, null, null, null, null,
                null, null, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         GlobalProviderDiscoveryListener consumersListener) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, null, null, null, null, null, null, null,
                null, null, consumersListener);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         String form,
                                         GlobalProviderDiscoveryListener consumersListener) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, null, form, null, null, null, null, null,
                null, null, consumersListener);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         Integer requestTimeout,
                                         Integer retryCount) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), providerAddresses, interfaceName,
                null, null, null, null, null, null, null, null,
                requestTimeout, retryCount, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         Integer retryCount) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, serializer, form, version, null, null, null, null,
                requestTimeout, retryCount, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         String faultTolerance) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, serializer, form, version, null, faultTolerance, null, null,
                null, null, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         String faultTolerance) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), null, interfaceName,
                null, serializer, form, version, null, faultTolerance, null, null,
                requestTimeout, null, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         Integer retryCount) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), providerAddresses, interfaceName,
                null, serializer, form, version, null, null, null, null,
                requestTimeout, retryCount, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         Integer retryCount,
                                         String faultTolerance) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), providerAddresses, interfaceName,
                null, serializer, form, version, null, faultTolerance, null, null,
                requestTimeout, retryCount, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         Integer retryCount,
                                         GlobalProviderDiscoveryListener consumersListener) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), providerAddresses, interfaceName,
                null, null, form, version, null, null, null, null,
                requestTimeout, retryCount, consumersListener);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         Integer retryCount,
                                         GlobalProviderDiscoveryListener consumersListener) {
        return create(applicationConfig, registryConfig, protocolConfig, new ConsumerConfig(), providerAddresses, interfaceName,
                null, serializer, form, version, null, null, null, null,
                requestTimeout, retryCount, consumersListener);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         ConsumerConfig consumerConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String serializer,
                                         String form,
                                         String version,
                                         Integer requestTimeout,
                                         Integer retryCount,
                                         GlobalProviderDiscoveryListener consumersListener) {
        return create(applicationConfig, registryConfig, protocolConfig, consumerConfig, providerAddresses, interfaceName,
                null, serializer, form, version, null, null, null, null,
                requestTimeout, retryCount, consumersListener);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         ConsumerConfig consumerConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         String protocol,
                                         String serializer,
                                         String form,
                                         String version,
                                         String invoker,
                                         String faultTolerance,
                                         String loadBalancer,
                                         String proxyFactory,
                                         Integer requestTimeout,
                                         Integer retryCount,
                                         GlobalProviderDiscoveryListener consumersListener) {
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setProviderAddresses(providerAddresses);
        consumerStub.setInterfaceName(interfaceName);
        consumerStub.setProtocol(defaultIfEmpty(protocol, protocolConfig.getName()));
        consumerStub.setSerializer(defaultIfEmpty(serializer, protocolConfig.getSerializer()));
        consumerStub.setForm(defaultIfEmpty(form, consumerConfig.getForm()));
        consumerStub.setVersion(defaultIfEmpty(version, consumerConfig.getVersion()));
        consumerStub.setInvoker(defaultIfEmpty(invoker, consumerConfig.getInvoker()));
        consumerStub.setFaultTolerance(defaultIfEmpty(faultTolerance, consumerConfig.getFaultTolerance()));
        consumerStub.setLoadBalancer(defaultIfEmpty(loadBalancer, consumerConfig.getLoadBalancer()));
        consumerStub.setProxy(defaultIfEmpty(proxyFactory, consumerConfig.getProxyFactory()));
        consumerStub.setRequestTimeout(requestTimeout != null ? requestTimeout : consumerConfig.getRequestTimeout());
        consumerStub.setRetryCount(retryCount != null ? retryCount : consumerConfig.getRetryCount());
        consumerStub.setLimitRate(consumerConfig.isLimitRate());
        consumerStub.setMaxPayload(consumerConfig.getMaxPayload());

        // Must NOT call init()
        consumerStub.subscribeProviders(applicationConfig, protocolConfig, registryConfig, consumersListener);
        return consumerStub;
    }
}

