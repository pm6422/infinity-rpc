package org.infinity.luix.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.sender.Sendable;
import org.infinity.luix.core.client.sender.impl.RequestSender;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.server.exposer.ProviderExposable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractProtocol implements Protocol {
    protected static final Map<String, ProviderExposable> EXPOSED_PROVIDERS = new ConcurrentHashMap<>();

    @Override
    public ProviderExposable exposeProvider(Url providerUrl) {
        if (providerUrl == null) {
            throw new RpcFrameworkException("Provider url must NOT be null!");
        }

        String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);
        synchronized (EXPOSED_PROVIDERS) {
            ProviderExposable exposer = EXPOSED_PROVIDERS.get(providerKey);
            if (exposer != null) {
                throw new RpcFrameworkException("Can NOT re-expose provider [" + providerUrl + "]");
            }

            exposer = doExpose(providerUrl);
            exposer.init();

            EXPOSED_PROVIDERS.put(providerKey, exposer);
            log.info("Exposed provider [{}]", providerUrl);
            return exposer;
        }
    }

    @Override
    public void hideProvider(Url providerUrl) {
        if (providerUrl == null) {
            throw new RpcFrameworkException("Provider url must NOT be null!");
        }

        String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);
        synchronized (EXPOSED_PROVIDERS) {
            ProviderExposable exposer = EXPOSED_PROVIDERS.get(providerKey);
            if (exposer == null) {
                throw new RpcFrameworkException("Provider [" + providerUrl + "] does NOT exist!");
            }
            exposer.cancelExpose();
        }
    }

    @Override
    public Sendable createRequestSender(String interfaceName, Url providerUrl) {
        // todo: create different caller associated with the protocol
        return new RequestSender(interfaceName, providerUrl);
    }

    /**
     * Do expose provider
     *
     * @param providerUrl provider url
     * @return exposer
     */
    protected abstract ProviderExposable doExpose(Url providerUrl);

    @Override
    public void destroy() {
        EXPOSED_PROVIDERS.values().forEach(exposer -> {
            try {
                exposer.destroy();
                log.info("Destroyed [" + exposer + "]");
            } catch (Throwable t) {
                log.error("Failed to destroy [" + exposer + "]", t);
            }
        });
        EXPOSED_PROVIDERS.clear();
    }
}
