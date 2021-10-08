package org.infinity.luix.core.server.exposer;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.url.Url;

@Slf4j
public abstract class AbstractProviderExposer implements ProviderExposable {
    protected volatile boolean active = false;
    protected          Url     providerUrl;

    public AbstractProviderExposer(Url providerUrl) {
        this.providerUrl = providerUrl;
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public synchronized void expose() {
        boolean result = doInit();
        if (result) {
            active = true;
            log.info("Exposed provider [{}]", providerUrl);
        } else {
            throw new RpcFrameworkException("Failed to initialize provider exposer");
        }
    }

    /**
     * Do initialization
     *
     * @return {@code true} if it was initialized and {@code false} otherwise
     */
    protected abstract boolean doInit();

    @Override
    public String toString() {
        return this.getClass().getSimpleName().concat(":").concat(providerUrl.toFullStr());
    }
}
