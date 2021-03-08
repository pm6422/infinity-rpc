package org.infinity.rpc.core.client.cluster;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Importable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.util.List;

/**
 * One cluster for one protocol, only one server node under a cluster can receive the request
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface ProviderCluster<T> extends ProviderCallable<T> {
    void setProtocol(String protocol);

    String getProtocol();

    void setInterfaceName(String interfaceName);

    void setLoadBalancer(LoadBalancer<T> loadBalancer);

    LoadBalancer<T> getLoadBalancer();

    void setFaultTolerance(FaultTolerance<T> faultTolerance);

    FaultTolerance<T> getFaultTolerance();

    /**
     * Refresh provider callers when providers is online or offline
     *
     * @param importers provider call
     */
    void refresh(List<Importable<T>> importers);

    /**
     * Destroy
     */
    void destroy();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    @SuppressWarnings("unchecked")
    static <T> ProviderCluster<T> getInstance(String name) {
        return ServiceLoader.forClass(ProviderCluster.class).load(name);
    }

    /**
     * Create a provider cluster
     *
     * @param interfaceName      interface name
     * @param protocol           protocol name
     * @param clusterName        provider cluster name
     * @param loadBalancerName   load balancer name
     * @param faultToleranceName fault tolerance name
     * @param clientUrl          client url
     * @param <T>                provider interface class
     * @return provider cluster
     */
    static <T> ProviderCluster<T> createCluster(String interfaceName, String clusterName,
                                                String protocol, String faultToleranceName, String loadBalancerName, Url clientUrl) {
        // It support one cluster for one protocol for now, but do not support one cluster for one provider
        ProviderCluster<T> providerCluster = ProviderCluster.getInstance(clusterName);
        providerCluster.setInterfaceName(interfaceName);
        providerCluster.setProtocol(protocol);
        FaultTolerance<T> faultTolerance = FaultTolerance.getInstance(faultToleranceName);
        faultTolerance.setClientUrl(clientUrl);
        providerCluster.setFaultTolerance(faultTolerance);
        LoadBalancer<T> loadBalancer = LoadBalancer.getInstance(loadBalancerName);
        providerCluster.setLoadBalancer(loadBalancer);
        // Initialize
        providerCluster.init();
        return providerCluster;
    }
}
