package org.infinity.rpc.core.exchange;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;

public interface RpcCallable<T> {
    /**
     * @return interface class
     */
    Class<T> getInterfaceClass();

    /**
     * @return true: available, false: unavailable
     */
    boolean isAvailable();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable<T> call(Requestable<T> request);

    /**
     *
     */
    void init();

    void destroy();
}
