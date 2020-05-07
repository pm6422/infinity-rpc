package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.destory.ShutdownHook;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The registry can automatically recover services when encountered the failure.
 */
@Slf4j
public abstract class FailbackAbstractRegistry extends AbstractRegistry {

    private Set<Url>                                    failedRegisteredUrl   = new ConcurrentHashSet<>();
    private Set<Url>                                    failedUnregisteredUrl = new ConcurrentHashSet<>();
    private Map<Url, ConcurrentHashSet<NotifyListener>> failedSubscription    = new ConcurrentHashMap<>();
    private Map<Url, ConcurrentHashSet<NotifyListener>> failedUnsubscription  = new ConcurrentHashMap<>();

    /**
     * A retry single thread pool can reconnect registry periodically
     */
    private static ScheduledExecutorService retryThreadPool = Executors.newScheduledThreadPool(1);

    static {
        ShutdownHook.add(() -> {
            if (!retryThreadPool.isShutdown()) {
                retryThreadPool.shutdown();
            }
        });
    }

    public FailbackAbstractRegistry(Url url) {
        super(url);
        scheduleRetry(url);
    }

    /**
     * Schedule the retry attempt periodically
     *
     * @param url url
     */
    private void scheduleRetry(Url url) {
        long retryInterval = url.getIntParameter(Url.PARAM_RETRY_INTERVAL);
        // Retry to connect registry at retry interval
        retryThreadPool.scheduleAtFixedRate(() -> {
            // Do retry task
            doRetry();
        }, retryInterval, retryInterval, TimeUnit.MILLISECONDS);
    }

    private void doRetry() {
        doRetryFailedRegistration();
        doRetryFailedUnregistration();
        doRetryFailedSubscription();
        doRetryFailedUnsubscription();
    }

    private void doRetryFailedRegistration() {
        if (CollectionUtils.isEmpty(failedRegisteredUrl)) {
            return;
        }
        Iterator<Url> iterator = failedRegisteredUrl.iterator();
        while (iterator.hasNext()) {
            Url url = iterator.next();
            try {
                super.register(url);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to retry to register [{0}] by [{1}] and it will be retry later!", url, registryClassName), e);
            }
            iterator.remove();
        }
        log.info("Retried to register urls by {}", registryClassName);
    }

    private void doRetryFailedUnregistration() {
        if (CollectionUtils.isEmpty(failedUnregisteredUrl)) {
            return;
        }
        Iterator<Url> iterator = failedUnregisteredUrl.iterator();
        while (iterator.hasNext()) {
            Url url = iterator.next();
            try {
                super.unregister(url);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to retry to unregister [{0}] by [{1}] and it will be retry later!", url, registryClassName), e);
            }
            iterator.remove();
        }
        log.info("Retried to unregister urls by {}", registryClassName);
    }

    private void doRetryFailedSubscription() {
        if (MapUtils.isEmpty(failedSubscription)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<NotifyListener>> entry : failedSubscription.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                failedSubscription.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(failedSubscription)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<NotifyListener>> entry : failedSubscription.entrySet()) {
            Url url = entry.getKey();
            Iterator<NotifyListener> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                NotifyListener listener = iterator.next();
                try {
                    super.subscribe(url, listener);
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Failed to retry to subscribe listener [{0}] to url [{1}] by [{2}] " +
                            "and it will be retry later!", listener.getClass().getSimpleName(), url, registryClassName), e);
                }
                iterator.remove();
            }
        }
        log.info("Retried to subscribe listener to urls by {}", registryClassName);
    }

    private void doRetryFailedUnsubscription() {
        if (MapUtils.isEmpty(failedUnsubscription)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<NotifyListener>> entry : failedUnsubscription.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                failedUnsubscription.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(failedUnsubscription)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<NotifyListener>> entry : failedUnsubscription.entrySet()) {
            Url url = entry.getKey();
            Iterator<NotifyListener> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                NotifyListener listener = iterator.next();
                try {
                    super.unsubscribe(url, listener);
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Failed to retry to unsubscribe listener [{0}] to url [{1}] by [{2}] " +
                            "and it will be retry later!", listener.getClass().getSimpleName(), url, registryClassName), e);
                }
                iterator.remove();
            }
        }
        log.info("Retried to unsubscribe listener to urls by {}", registryClassName);
    }

    /**
     * Register the url
     *
     * @param url url
     */
    @Override
    public void register(Url url) {
        failedRegisteredUrl.remove(url);
        failedUnregisteredUrl.remove(url);

        try {
            super.register(url);
        } catch (Exception e) {
            if (isCheckingUrls(getRegistryUrl(), url)) {
                throw new RuntimeException(MessageFormat.format("Failed to register the url [{0}] to registry [{1}] by using [{2}]", url, getRegistryUrl(), registryClassName), e);
            }
            failedRegisteredUrl.add(url);
        }
    }

    @Override
    public void unregister(Url url) {
        failedRegisteredUrl.remove(url);
        failedUnregisteredUrl.remove(url);

        try {
            super.unregister(url);
        } catch (Exception e) {
            if (isCheckingUrls(getRegistryUrl(), url)) {
                throw new RuntimeException(String.format("[%s] false to unregistery %s to %s", registryClassName, url, getRegistryUrl()), e);
            }
            failedUnregisteredUrl.add(url);
        }
    }

    @Override
    public void subscribe(Url url, NotifyListener listener) {
        removeForFailedSubAndUnsub(url, listener);

        try {
            super.subscribe(url, listener);
        } catch (Exception e) {
            List<Url> cachedUrls = getCachedUrls(url);
            if (cachedUrls != null && cachedUrls.size() > 0) {
                listener.onSubscribe(getRegistryUrl(), cachedUrls);
            } else if (isCheckingUrls(getRegistryUrl(), url)) {
                log.warn(String.format("[%s] false to subscribe %s from %s", registryClassName, url, getRegistryUrl()), e);
                throw new RuntimeException(String.format("[%s] false to subscribe %s from %s", registryClassName, url, getRegistryUrl()), e);
            }
            addToFailedMap(failedSubscription, url, listener);
        }
    }

    @Override
    public void unsubscribe(Url url, NotifyListener listener) {
        removeForFailedSubAndUnsub(url, listener);

        try {
            super.unsubscribe(url, listener);
        } catch (Exception e) {
            if (isCheckingUrls(getRegistryUrl(), url)) {
                throw new RuntimeException(String.format("[%s] false to unsubscribe %s from %s", registryClassName, url, getRegistryUrl()),
                        e);
            }
            addToFailedMap(failedUnsubscription, url, listener);
        }
    }

    @Override
    public List<Url> discover(Url url) {
        try {
            return super.discover(url);
        } catch (Exception e) {
            // 如果discover失败，返回一个empty list吧，毕竟是个下行动作，
            log.warn(String.format("Failed to discover url:%s in registry (%s)", url, getRegistryUrl()), e);
            return Collections.EMPTY_LIST;
        }
    }

    private boolean isCheckingUrls(Url... urls) {
        for (Url url : urls) {
            if (!Boolean.parseBoolean(url.getParameter(UrlParam.check.getName(), UrlParam.check.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private void removeForFailedSubAndUnsub(Url url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscription.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscription.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void addToFailedMap(Map<Url, ConcurrentHashSet<NotifyListener>> failedMap, Url url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedMap.get(url);
        if (listeners == null) {
            failedMap.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = failedMap.get(url);
        }
        listeners.add(listener);
    }
}