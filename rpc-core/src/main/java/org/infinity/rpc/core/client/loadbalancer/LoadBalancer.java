package org.infinity.rpc.core.client.loadbalancer;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.util.List;

/**
 * {@link FaultTolerance} select providers via load balance algorithm.
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface LoadBalancer {
    /**
     * Get provider callers
     *
     * @return provider invokers
     */
    List<Invokable> getInvokers();

    /**
     * Refresh provider callers when online or offline
     *
     * @param invokers new discovered provider callers
     */
    void refresh(List<Invokable> invokers);

    /**
     * Select provider node via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider caller
     */
    Invokable selectProviderNode(Requestable request);

    /**
     * Select multiple provider nodes via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider callers
     */
    List<Invokable> selectProviderNodes(Requestable request);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    @SuppressWarnings("unchecked")
    static LoadBalancer getInstance(String name) {
        return ServiceLoader.forClass(LoadBalancer.class).load(name);
    }

    /**
     * Destroy
     */
    void destroy();

//
//    void setWeightString(String weightString);
}
