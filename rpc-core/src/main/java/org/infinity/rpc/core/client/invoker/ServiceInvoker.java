package org.infinity.rpc.core.client.invoker;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.infinity.rpc.utilities.serviceloader.annotation.Spi;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiScope;

/**
 * A service invoker used to invoke service provider at client side
 * One invoker for one protocol
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface ServiceInvoker {
    /**
     * Initialize
     */
    void init();

    /**
     * Create a service provider invoker instance
     *
     * @param interfaceName      interface name
     * @param faultToleranceName fault tolerance name
     * @param loadBalancerName   load balancer name
     * @param consumerUrl        consumer url
     * @return service invoker instance
     */
    ServiceInvoker createServiceInvoker(String interfaceName,
                                        String faultToleranceName,
                                        String loadBalancerName,
                                        Url consumerUrl);

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable invoke(Requestable request);

    /**
     * Check whether it is available
     *
     * @return {@code true} if it was active and {@code false} otherwise
     */
    boolean isActive();

    /**
     * Set provider interface name
     *
     * @param interfaceName interface name
     */
    void setInterfaceName(String interfaceName);

    /**
     * Set fault tolerance
     *
     * @param faultTolerance fault tolerance
     */
    void setFaultTolerance(FaultTolerance faultTolerance);

    /**
     * Get fault tolerance
     *
     * @return faultTolerance fault tolerance
     */
    FaultTolerance getFaultTolerance();

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
    static ServiceInvoker getInstance(String name) {
        return ServiceLoader.forClass(ServiceInvoker.class).load(name);
    }
}
