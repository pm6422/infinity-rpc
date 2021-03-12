package org.infinity.rpc.core.client.faulttolerance;

import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

/**
 * Fault tolerance refers to the ability of a system to continue operating without interruption
 * when one or more of its components fail.
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface FaultTolerance {

    /**
     * Set client url
     *
     * @param clientUrl client url
     */
    void setClientUrl(Url clientUrl);

    /**
     * Get client url
     *
     * @return client url
     */
    Url getClientUrl();

    /**
     * Invoke the RPC provider
     *
     * @param loadBalancer load balancer
     * @param request      RPC request
     * @return RPC response
     */
    Responseable invoke(LoadBalancer loadBalancer, Requestable request);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static FaultTolerance getInstance(String name) {
        return ServiceLoader.forClass(FaultTolerance.class).load(name);
    }
}