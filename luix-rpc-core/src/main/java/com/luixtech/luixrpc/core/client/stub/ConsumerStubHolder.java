package com.luixtech.luixrpc.core.client.stub;

import com.luixtech.luixrpc.utilities.concurrent.ThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ConsumerStubHolder {

    private static final ConsumerStubHolder           INSTANCE = new ConsumerStubHolder();
    /**
     * RPC provider stub map
     */
    private final        Map<String, ConsumerStub<?>> cache    = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private ConsumerStubHolder() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link ConsumerStubHolder}
     */
    public static ConsumerStubHolder getInstance() {
        return INSTANCE;
    }

    public synchronized void add(String name, ConsumerStub<?> consumerStub) {
        cache.putIfAbsent(name, consumerStub);
    }

    public synchronized Map<String, ConsumerStub<?>> getMap() {
        return cache;
    }
}
