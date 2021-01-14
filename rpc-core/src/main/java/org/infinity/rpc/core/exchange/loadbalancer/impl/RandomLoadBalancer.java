package org.infinity.rpc.core.exchange.loadbalancer.impl;

import org.infinity.rpc.core.exchange.loadbalancer.AbstractLoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @param <T>: The interface class of the provider
 */
@ServiceName("random")
public class RandomLoadBalancer<T> extends AbstractLoadBalancer<T> {

    @Override
    protected ProviderCaller<T> doSelectNode(Requestable request) {
        int index = getIndex(providerCallers);
        for (int i = 0; i < providerCallers.size(); i++) {
            ProviderCaller<T> providerCaller = providerCallers.get((i + index) % providerCallers.size());
            if (providerCaller.isActive()) {
                return providerCaller;
            }
        }
        return null;
    }

    @Override
    protected List<ProviderCaller<T>> doSelectNodes(Requestable request) {
        List<ProviderCaller<T>> selected = new ArrayList<>();
        int index = getIndex(providerCallers);
        for (int i = 0; i < providerCallers.size(); i++) {
            ProviderCaller<T> providerCaller = providerCallers.get((i + index) % providerCallers.size());
            if (providerCaller.isActive()) {
                selected.add(providerCaller);
            }
        }
        return selected;
    }

    private int getIndex(List<ProviderCaller<T>> providerCallers) {
        return (int) (ThreadLocalRandom.current().nextDouble() * providerCallers.size());
    }
}
