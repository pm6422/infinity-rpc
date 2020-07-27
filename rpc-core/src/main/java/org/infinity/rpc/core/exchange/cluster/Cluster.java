package org.infinity.rpc.core.exchange.cluster;

import org.infinity.rpc.core.exchange.RpcCallable;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = Scope.PROTOTYPE)
public interface Cluster<T> extends RpcCallable<T> {
    /**
     * Refresh requesters when online or offline
     *
     * @param requesters
     */
    void onRefresh(List<Requester<T>> requesters);

    void setLoadBalancer(LoadBalancer<T> loadBalance);

    LoadBalancer<T> getLoadBalancer();

    void setHighAvailability(HighAvailability<T> haStrategy);

    List<Requester<T>> getRequesters();
}
