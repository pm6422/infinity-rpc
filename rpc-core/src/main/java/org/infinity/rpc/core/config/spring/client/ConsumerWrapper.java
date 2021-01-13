package org.infinity.rpc.core.config.spring.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.listener.SubscribeProviderListener;
import org.infinity.rpc.core.url.Url;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.constant.ConsumerConstants.DIRECT_URL;
import static org.infinity.rpc.core.constant.ConsumerConstants.TIMEOUT;
import static org.infinity.rpc.core.constant.ServiceConstants.GROUP;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 *
 * @param <T>: The interface class of the consumer
 */
@Slf4j
@Getter
public class ConsumerWrapper<T> implements DisposableBean {
    /**
     * The name of consumer wrapper instance
     */
    private final String              consumerWrapperBeanName;
    /**
     * The interface class of the consumer
     */
    private final Class<T>            interfaceClass;
    /**
     *
     */
    private       ProviderCluster     providerCluster;
    /**
     * The consumer proxy instance, refer the return type of {@link ConsumerProxy#getProxy(ConsumerWrapper)}
     */
    private final T                            proxyInstance;
    /**
     *
     */
    private       SubscribeProviderListener<T> subscribeProviderListener;
    /**
     *
     */
    private       Url                          clientUrl;
    /**
     *
     */
    private       String              directUrl;
    /**
     *
     */
    private       int                 timeout;
    /**
     *
     */
    private       String              group;
    /**
     *
     */
    private       String              version;

    public ConsumerWrapper(String consumerWrapperBeanName, Class<T> interfaceClass) {
        Assert.hasText(consumerWrapperBeanName, "Consumer wrapper bean name must not be empty!");
        Assert.notNull(interfaceClass, "Consumer interface class must not be null!");

        this.consumerWrapperBeanName = consumerWrapperBeanName;
        this.interfaceClass = interfaceClass;
        this.proxyInstance = ConsumerProxy.getProxy(this);
    }

    public void init(InfinityProperties infinityProperties, List<Url> registryUrls, Map<String, Object> consumerAttributesMap) {
        clientUrl = Url.clientUrl(infinityProperties.getProtocol().getName().name(), interfaceClass.getName());
        // Initialize provider cluster before consumer initialization
        providerCluster = createProviderCluster(infinityProperties);
        subscribeProviderListener = SubscribeProviderListener.of(interfaceClass, providerCluster, registryUrls, clientUrl);
        // Set attribute values of @Consumer annotation
        directUrl = (String) consumerAttributesMap.get(DIRECT_URL);
        timeout = (int) consumerAttributesMap.get(TIMEOUT);
        group = (String) consumerAttributesMap.get(GROUP);
        version = (String) consumerAttributesMap.get(VERSION);
    }

    private ProviderCluster createProviderCluster(InfinityProperties infinityProperties) {
        // todo: support multiple protocols
        // 当配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        return ProviderCluster.createCluster(infinityProperties.getProtocol().getName().name(),
                infinityProperties.getProtocol().getCluster(),
                infinityProperties.getProtocol().getLoadBalancer(),
                infinityProperties.getProtocol().getFaultTolerance());
    }

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }
}
