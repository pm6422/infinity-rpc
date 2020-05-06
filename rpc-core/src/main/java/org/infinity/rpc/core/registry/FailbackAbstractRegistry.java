package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The registry can automatically recover when encountered the failure.
 */
@Slf4j
public abstract class FailbackAbstractRegistry extends AbstractRegistry {

    private        Set<Url>                                    failedRegistered   = new ConcurrentHashSet<>();
    private        Set<Url>                                    failedUnregistered = new ConcurrentHashSet<>();
    private        Map<Url, ConcurrentHashSet<NotifyListener>> failedSubscribed   = new ConcurrentHashMap<>();
    private        Map<Url, ConcurrentHashSet<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<>();
    /**
     * A retry thread pool can execute task periodically
     */
    private static ScheduledExecutorService                    retryExecutor      = Executors.newScheduledThreadPool(1);

    static {
        ShutdownHook.add(() -> {
            if (!retryExecutor.isShutdown()) {
                retryExecutor.shutdown();
            }
        });
    }

    public FailbackAbstractRegistry(Url url) {
        super(url);
        long retryPeriod = url.getIntParameter(Url.PARAM_RETRY_INTERVAL);
        retryExecutor.scheduleAtFixedRate(() -> {
            try {
                retry();
            } catch (Exception e) {
                log.warn(String.format("[%s] False when retry in failback registry", registryClassName), e);
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    /**
     * Register the url
     *
     * @param url url
     */
    @Override
    public void register(Url url) {
        failedRegistered.remove(url);
        failedUnregistered.remove(url);

        try {
            super.register(url);
        } catch (Exception e) {
            if (isCheckingUrls(getRegistryUrl(), url)) {
                throw new RuntimeException(MessageFormat.format("Failed to register the url [{0}] to registry [{1}] by using [{2}]", url, getRegistryUrl(), registryClassName), e);
            }
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(Url url) {
        failedRegistered.remove(url);
        failedUnregistered.remove(url);

        try {
            super.unregister(url);
        } catch (Exception e) {
            if (isCheckingUrls(getRegistryUrl(), url)) {
                throw new RuntimeException(String.format("[%s] false to unregistery %s to %s", registryClassName, url, getRegistryUrl()), e);
            }
            failedUnregistered.add(url);
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
            addToFailedMap(failedSubscribed, url, listener);
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
            addToFailedMap(failedUnsubscribed, url, listener);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(url);
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

    private void retry() {
        if (!failedRegistered.isEmpty()) {
            Set<Url> failed = new HashSet<Url>(failedRegistered);
            log.info("[{}] Retry register {}", registryClassName, failed);
            try {
                for (Url url : failed) {
                    super.register(url);
                    failedRegistered.remove(url);
                }
            } catch (Exception e) {
                log.warn(String.format("[%s] Failed to retry register, retry later, failedRegistered.size=%s, cause=%s",
                        registryClassName, failedRegistered.size(), e.getMessage()), e);
            }

        }
        if (!failedUnregistered.isEmpty()) {
            Set<Url> failed = new HashSet<Url>(failedUnregistered);
            log.info("[{}] Retry unregister {}", registryClassName, failed);
            try {
                for (Url url : failed) {
                    super.unregister(url);
                    failedUnregistered.remove(url);
                }
            } catch (Exception e) {
                log.warn(String.format("[%s] Failed to retry unregister, retry later, failedUnregistered.size=%s, cause=%s",
                        registryClassName, failedUnregistered.size(), e.getMessage()), e);
            }

        }
        if (!failedSubscribed.isEmpty()) {
            Map<Url, Set<NotifyListener>> failed = new HashMap<Url, Set<NotifyListener>>(failedSubscribed);
            for (Map.Entry<Url, Set<NotifyListener>> entry : new HashMap<Url, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                log.info("[{}] Retry subscribe {}", registryClassName, failed);
                try {
                    for (Map.Entry<Url, Set<NotifyListener>> entry : failed.entrySet()) {
                        Url url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            super.subscribe(url, listener);
                            listeners.remove(listener);
                        }
                    }
                } catch (Exception e) {
                    log.warn(String.format("[%s] Failed to retry subscribe, retry later, failedSubscribed.size=%s, cause=%s",
                            registryClassName, failedSubscribed.size(), e.getMessage()), e);
                }
            }
        }
        if (!failedUnsubscribed.isEmpty()) {
            Map<Url, Set<NotifyListener>> failed = new HashMap<Url, Set<NotifyListener>>(failedUnsubscribed);
            for (Map.Entry<Url, Set<NotifyListener>> entry : new HashMap<Url, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                log.info("[{}] Retry unsubscribe {}", registryClassName, failed);
                try {
                    for (Map.Entry<Url, Set<NotifyListener>> entry : failed.entrySet()) {
                        Url url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            super.unsubscribe(url, listener);
                            listeners.remove(listener);
                        }
                    }
                } catch (Exception e) {
                    log.warn(String.format("[%s] Failed to retry unsubscribe, retry later, failedUnsubscribed.size=%s, cause=%s",
                            registryClassName, failedUnsubscribed.size(), e.getMessage()), e);
                }
            }
        }
    }
}