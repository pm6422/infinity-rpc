package org.infinity.rpc.core.server;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to start and stop the RPC server
 */
@Slf4j
public class RpcServerLifecycle {
    /**
     * The initialization flag used to identify whether the RPC server already initialized.
     */
    private AtomicBoolean initialized = new AtomicBoolean(false);
    /**
     * The start flag used to identify whether the RPC server already started.
     */
    private AtomicBoolean started     = new AtomicBoolean(false);
    /**
     * The stop flag used to identify whether the RPC server already stopped.
     */
    private AtomicBoolean stopped     = new AtomicBoolean(false);

    /**
     * Prohibit instantiate an instance
     */
    private RpcServerLifecycle() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link RpcServerLifecycle}
     */
    public static RpcServerLifecycle getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        private static final RpcServerLifecycle INSTANCE = new RpcServerLifecycle();// static variable will be instantiated on class loading.
    }

    public AtomicBoolean getInitialized() {
        return initialized;
    }

    public AtomicBoolean getStarted() {
        return started;
    }

    public AtomicBoolean getStopped() {
        return stopped;
    }

    /**
     * Start the RPC server
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the RPC server");
        initConfig();
        registerProviders();
        log.info("Started the RPC server");
    }

    /**
     * Initialize the RPC server
     */
    private void initConfig() {
        if (!initialized.compareAndSet(false, true)) {
            // already initialized
            return;
        }
    }

    /**
     * Register RPC providers to registry
     */
    private void registerProviders() {
        ProviderWrapperHolder.getInstance().getWrappers().forEach((name, providerWrapper) -> {
            providerWrapper.register();
        });
    }

    public void stop() {
    }
}
