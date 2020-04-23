package org.infinity.rpc.core.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractRegistryFactory implements RegistryFactory {
    private static       Map<String, Registry> registries = new ConcurrentHashMap<String, Registry>();
    private static final ReentrantLock         lock       = new ReentrantLock();

    protected String getRegistryUri(Url url) {
        String registryUri = url.getUri();
        return registryUri;
    }

    @Override
    public Registry getRegistry(Url url) {
        String registryUri = getRegistryUri(url);
        try {
            lock.lock();
            Registry registry = registries.get(registryUri);
            if (registry != null) {
                return registry;
            }
            registry = createRegistry(url);
            if (registry == null) {
                throw new RuntimeException("Create registry false for url:" + url);
            }
            registries.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RuntimeException("Create registry false for url:" + url);
        } finally {
            lock.unlock();
        }
    }

    protected abstract Registry createRegistry(Url url);
}
