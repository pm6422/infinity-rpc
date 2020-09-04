package org.infinity.rpc.core.exchange.cluster;

import org.infinity.rpc.core.exchange.ProviderCallable;
import org.infinity.rpc.core.exchange.faulttolerance.FaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

/**
 * One cluster for one protocol, only one server node under a cluster can receive the request
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface ProviderCluster<T> extends ProviderCallable<T> {
    /**
     * @param registryInfo
     */
    void setRegistryInfo(RegistryInfo registryInfo);

    /**
     * Refresh requesters when online or offline
     *
     * @param providerRequesters
     */
    void onRefresh(List<ProviderRequester<T>> providerRequesters);

    void setLoadBalancer(LoadBalancer<T> loadBalancer);

    LoadBalancer<T> getLoadBalancer();

    void setFaultToleranceStrategy(FaultToleranceStrategy<T> faultToleranceStrategy);

    FaultToleranceStrategy<T> getFaultToleranceStrategy();

    List<ProviderRequester<T>> getRequesters();

    // todo: remove clientUrl param
    static <T> ProviderCluster<T> createCluster(String clusterName, String loadBalancerName, String faultToleranceName, Url clientUrl) {
        // It support one cluster for one protocol for now, but do not support one cluster for one provider
        ProviderCluster<T> providerCluster = ServiceInstanceLoader.getServiceLoader(ProviderCluster.class).load(clusterName);
        LoadBalancer<T> loadBalancer = ServiceInstanceLoader.getServiceLoader(LoadBalancer.class).load(loadBalancerName);
        FaultToleranceStrategy<T> faultTolerance = ServiceInstanceLoader.getServiceLoader(FaultToleranceStrategy.class).load(faultToleranceName);
        faultTolerance.setClientUrl(clientUrl);

        providerCluster.setFaultToleranceStrategy(faultTolerance);
        providerCluster.setLoadBalancer(loadBalancer);
        // Initialize
        providerCluster.init();

        // Add to cluster holder
        ProviderClusterHolder.getInstance().addCluster(clusterName, providerCluster);

        return providerCluster;
    }
}
