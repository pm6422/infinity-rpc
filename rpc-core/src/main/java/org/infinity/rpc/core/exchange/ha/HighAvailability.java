package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface HighAvailability<T> {

    // TODO: check use
    void setClientUrl(Url clientUrl);

    // TODO: check use
    Url getClientUrl();

    Responseable call(Requestable<T> request, LoadBalancer<T> loadBalancer);
}