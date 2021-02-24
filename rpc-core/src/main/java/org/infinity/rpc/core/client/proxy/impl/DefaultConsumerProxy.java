package org.infinity.rpc.core.client.proxy.impl;


import org.infinity.rpc.core.client.invocationhandler.impl.ConsumerInvocationHandler;
import org.infinity.rpc.core.client.proxy.ConsumerProxyFactory;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.lang.reflect.Proxy;

@ServiceName("default")
public class DefaultConsumerProxy implements ConsumerProxyFactory {
    /**
     * Get implementation proxy of consumer interface class
     *
     * @param stub Consumer stub
     * @param <T>  The interface class of the consumer
     * @return The consumer proxy instance
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T getProxy(ConsumerStub<T> stub) {
        Object proxy = Proxy.newProxyInstance(
                stub.getInterfaceClass().getClassLoader(),
                new Class<?>[]{stub.getInterfaceClass()},
                new ConsumerInvocationHandler<>(stub));
        return (T) proxy;
    }
}






