package org.infinity.rpc.core.exchange;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;

public interface RpcCallable<T> {
    /**
     * @return
     */
    Class<T> getInterfaceClass();

    /**
     * @return
     */
    boolean isAvailable();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);

    /**
     *
     */
    void init();

    void destroy();
}
