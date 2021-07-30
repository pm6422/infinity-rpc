package org.infinity.rpc.core.client.stub;

import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.ConsumerConfig;
import org.infinity.rpc.core.config.impl.ProtocolConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public abstract class ConsumerStubFactory {

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName) {
        return create(applicationConfig, registryConfig, protocolConfig, null, null, interfaceName,
                null, null, null, null, null, null, null, null,
                null, null, null);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String interfaceName,
                                         ProviderProcessable providerProcessor) {
        return create(applicationConfig, registryConfig, protocolConfig, null, null, interfaceName,
                null, null, null, null, null, null, null, null,
                null, null, providerProcessor);
    }

    public static ConsumerStub<?> create(ApplicationConfig applicationConfig,
                                         RegistryConfig registryConfig,
                                         ProtocolConfig protocolConfig,
                                         String providerAddresses,
                                         String interfaceName,
                                         ProviderProcessable providerProcessor) {
        return create(applicationConfig, registryConfig, protocolConfig, null, providerAddresses, interfaceName,
                null, null, null, null, null, null, null, null,
                null, null, providerProcessor);
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
                                         ProviderProcessable providerProcessor) {
        return create(applicationConfig, registryConfig, protocolConfig, null, providerAddresses, interfaceName,
                null, null, form, version, null, null, null, null,
                requestTimeout, retryCount, providerProcessor);
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
                                         ProviderProcessable providerProcessor) {
        return create(applicationConfig, registryConfig, protocolConfig, null, providerAddresses, interfaceName,
                null, serializer, form, version, null, null, null, null,
                requestTimeout, retryCount, providerProcessor);
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
                                         ProviderProcessable providerProcessor) {
        return create(applicationConfig, registryConfig, protocolConfig, consumerConfig, providerAddresses, interfaceName,
                null, serializer, form, version, null, null, null, null,
                requestTimeout, retryCount, providerProcessor);
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
                                         ProviderProcessable providerProcessor) {
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
        if (consumerConfig != null) {
            consumerStub.setLimitRate(consumerConfig.isLimitRate());
            consumerStub.setMaxPayload(consumerConfig.getMaxPayload());
        }

        // Must NOT call init()
        consumerStub.subscribeProviders(applicationConfig, protocolConfig, registryConfig, providerProcessor);
        return consumerStub;
    }
}

