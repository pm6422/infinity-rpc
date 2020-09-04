package org.infinity.rpc.core.exchange.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.faulttolerance.AbstractClusterFaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

/**
 * Fail-fast fault tolerance strategy
 * to see is the "fail-fast" from the literal meaning, found errors in the system as much as possible,
 * so that the system can be pre-configured in accordance with the process execution error,
 * the corresponding mode is "fault-tolerant (fault tolerance)"
 * With JAVA collection fast failure, for example,
 * when multiple threads are operating on the same set of content, it may generate fail-fast event.
 * For example: When a thread A iterator to traverse through a collection process,
 * the content if the set is changed by the other thread;
 * then thread A collection of access,
 * ConcurrentModificationException will throw an exception (error found a good set execution error process),
 * resulting in fail-fast event.
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("failfast")
public class FailfastFaultToleranceStrategy<T> extends AbstractClusterFaultToleranceStrategy<T> {
    @Override
    public Responseable call(LoadBalancer<T> loadBalancer, Requestable request) {
        ProviderRequester<T> availableProviderRequester = loadBalancer.selectProviderNode(request);
        return availableProviderRequester.call(request);
    }
}
