package org.infinity.rpc.core.config.spring.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.List;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 */
@Slf4j
@Data
@Builder
public class ConsumerWrapper<T> implements DisposableBean {
    /**
     *
     */
    private List<InfinityProperties.ProtocolConfig> protocolConfigs;
    /**
     *
     */
    private List<InfinityProperties.RegistryConfig> registryConfigs;
    /**
     * The interface class of the consumer
     */
    private Class<?>                                interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private String                                  instanceName;
    /**
     * The consumer proxy instance, refer the return type of {@link org.infinity.rpc.core.client.proxy.RpcConsumerProxy#getProxy(Class)}
     */
    private T                                       proxyInstance;

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }

    public T getProxyInstance() {
        if (proxyInstance == null) {
            initProxyInstance();
        }
        return proxyInstance;
    }

    public synchronized void initProxyInstance() {
        // One cluster per protocol
        List<Cluster<T>> clusters = new ArrayList<>(protocolConfigs.size());
        for (InfinityProperties.ProtocolConfig protocolConfig : protocolConfigs) {
            clusters.add(createCluster(protocolConfig));
        }
    }

    private Cluster<T> createCluster(InfinityProperties.ProtocolConfig protocolConfig) {

        Cluster cluster = ServiceInstanceLoader.getServiceLoader(Cluster.class).load(protocolConfig.getName().name());

        return cluster;
    }
}
