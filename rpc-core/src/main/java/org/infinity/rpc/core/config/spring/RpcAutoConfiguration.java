package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@EnableConfigurationProperties({InfinityProperties.class})
@Configuration
public class RpcAutoConfiguration {

    @Autowired
    private InfinityProperties infinityProperties;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Create registry urls
     *
     * @param infinityProperties configuration properties
     * @return registry urls
     */
    @Bean
    public List<Url> registryUrls(InfinityProperties infinityProperties) {
        Url registryUrl = Url.of(infinityProperties.getRegistry().getName().value(),
                infinityProperties.getRegistry().getHost(),
                infinityProperties.getRegistry().getPort(),
                Registrable.class.getName());

        // Assign values to parameters
        registryUrl.addParameter(Url.PARAM_CHECK_HEALTH, Url.PARAM_CHECK_HEALTH_DEFAULT_VALUE);
        registryUrl.addParameter(Url.PARAM_ADDRESS, registryUrl.getAddress());
        registryUrl.addParameter(Url.PARAM_CONNECT_TIMEOUT, infinityProperties.getRegistry().getConnectTimeout().toString());
        registryUrl.addParameter(Url.PARAM_SESSION_TIMEOUT, infinityProperties.getRegistry().getSessionTimeout().toString());
        registryUrl.addParameter(Url.PARAM_RETRY_INTERVAL, infinityProperties.getRegistry().getRetryInterval().toString());
        // TODO: Support multiple registry centers
        return Arrays.asList(registryUrl);
    }

    @Bean
    public RpcConsumerProxy rpcConsumerProxy(InfinityProperties infinityProperties) {
        List<Url> registryUrls = registryUrls(infinityProperties);
        List<Registry> registries = new ArrayList<>();
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactoryImpl = getRegistryFactory(infinityProperties.getRegistry().getName().value());
            registries.add(registryFactoryImpl.getRegistry(registryUrl));
        }
        return new RpcConsumerProxy(registries);
    }

    /**
     * Get the registry factory based on protocol
     *
     * @param protocol protocol
     * @return registry factory
     */
    private RegistryFactory getRegistryFactory(String protocol) {
        // Get the property registry factory by protocol value
        RegistryFactory registryFactory = ServiceInstanceLoader.getServiceLoader(RegistryFactory.class).load(protocol);
        return registryFactory;
    }

    @Bean
    public ApplicationRunner nettyServerApplicationRunner() {
        return new NettyServerApplicationRunner();
    }
}
