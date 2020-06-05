package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.support.AbstractApplicationContext;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ThreadSafe
public abstract class AbstractRegistryFactory implements RegistryFactory {
    /**
     * Object lock
     */
    private final  Lock                  lock            = new ReentrantLock();
    private static Map<String, Registry> registriesCache = new ConcurrentHashMap<>();

    /**
     * Get or create a registry
     * <p>
     * Do NOT throw exception while startup the application. If throw a exception will cause
     * {@link AbstractApplicationContext#refresh()} catch the exception, then call the cancelRefresh(ex) to set active to false;
     * So {@link org.springframework.boot.context.event.EventPublishingRunListener#failed(ConfigurableApplicationContext, Throwable)}
     * will found the active is false, then context.publishEvent(event) will not be executed.
     * So {@link org.infinity.rpc.core.config.spring.startup.RpcLifecycleApplicationListener#onApplicationContextEvent(ApplicationContextEvent)}
     * will not be invoked while ContextClosedEvent occurred.
     *
     * @param registryUrl registry url
     * @return registry
     */
    @Override
    public Registry getRegistry(Url registryUrl) {
        String registryUri = registryUrl.getUri();
        try {
            lock.lock();
            Registry registry = registriesCache.get(registryUri);
            if (registry != null) {
                return registry;
            }
            long start = System.currentTimeMillis();
            registry = createRegistry(registryUrl);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Created registry [{}] in {} ms", registry.getClass().getSimpleName(), elapsed);
            registriesCache.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            // Do NOT throw exception while startup the application.
            log.error("Failed to create registry for url [" + registryUrl + "]", e);
            return null;
        } finally {
            lock.unlock();
        }
    }
}
