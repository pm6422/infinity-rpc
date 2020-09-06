package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.config.spring.startup.NettyServerApplicationRunner;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {
    @Autowired
    private InfinityProperties infinityProperties;

    @Bean
    public RegistryInfo registryInfo() {
        List<Url> registryUrls = getRegistryUrls(infinityProperties);
        return new RegistryInfo(registryUrls, getRegistry(registryUrls));
    }

    /**
     * Create registry urls
     *
     * @param infinityProperties configuration properties
     * @return registry urls
     */
    private List<Url> getRegistryUrls(InfinityProperties infinityProperties) {
        Url registryUrl = Url.registryUrl(infinityProperties.getRegistry().getName().getValue(),
                infinityProperties.getRegistry().getHost(),
                infinityProperties.getRegistry().getPort());

        // Assign values to parameters
        registryUrl.addParameter(Url.PARAM_CHECK_HEALTH, Url.PARAM_CHECK_HEALTH_DEFAULT_VALUE);
        registryUrl.addParameter(Url.PARAM_ADDRESS, registryUrl.getAddress());
        registryUrl.addParameter(Url.PARAM_CONNECT_TIMEOUT, infinityProperties.getRegistry().getConnectTimeout().toString());
        registryUrl.addParameter(Url.PARAM_SESSION_TIMEOUT, infinityProperties.getRegistry().getSessionTimeout().toString());
        registryUrl.addParameter(Url.PARAM_RETRY_INTERVAL, infinityProperties.getRegistry().getRetryInterval().toString());

        // TODO: Support multiple registry centers
        List<Url> urls = Arrays.asList(registryUrl);
        return urls;
    }

    /**
     * @param registryUrls
     * @return
     */
    private List<Registry> getRegistry(List<Url> registryUrls) {
        List<Registry> registries = new ArrayList<>();
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactory = RegistryFactory.getInstance(infinityProperties.getRegistry().getName().getValue());
            registries.add(registryFactory.getRegistry(registryUrl));
        }
        return registries;
    }

    @Bean
    public ApplicationRunner nettyServerApplicationRunner() {
        return new NettyServerApplicationRunner();
    }
}
