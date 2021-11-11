package org.infinity.luix.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.core.registry.listener.ClientListener;
import org.infinity.luix.core.registry.listener.ProviderListener;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.annotation.EventPublisher;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;
import org.infinity.luix.utilities.concurrent.NotThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.infinity.luix.core.constant.ProtocolConstants.CODEC;

/**
 * Abstract registry
 */
@Slf4j
@NotThreadSafe
public abstract class AbstractRegistry implements Registry {
    /**
     * The registry subclass name
     */
    private final String                           registryClassName                 = this.getClass().getSimpleName();
    /**
     * Registry url
     */
    protected     Url                              registryUrl;
    /**
     * Registered provider urls cache
     */
    private final Set<Url>                         registeredProviderUrls            = new ConcurrentHashSet<>();
    /**
     * Provider urls cache grouped by 'type' parameter value of {@link Url}
     */
    private final Map<Url, Map<String, List<Url>>> providerUrlsPerTypePerConsumerUrl = new ConcurrentHashMap<>();

    @Override
    public String getRegistryClassName() {
        return registryClassName;
    }

    @Override
    public Url getRegistryUrl() {
        return registryUrl;
    }

    /**
     * Get the registered provider urls cache
     *
     * @return provider urls
     */
    @Override
    public Set<Url> getRegisteredProviderUrls() {
        return registeredProviderUrls;
    }

    public AbstractRegistry(Url registryUrl) {
        Validate.notNull(registryUrl, "Registry url must NOT be null!");
        this.registryUrl = registryUrl;
    }

    @Override
    public String getType() {
        return registryUrl.getProtocol();
    }

    /**
     * Register a provider url to registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void register(Url providerUrl) {
        Validate.notNull(providerUrl, "Provider url must NOT be null!");
        doRegister(removeUnnecessaryParams(providerUrl.copy()));
        log.info("Registered the provider url [{}] to registry [{}]", providerUrl, registryUrl.getIdentity());
        // Added it to the cache after registered
        registeredProviderUrls.add(providerUrl);
    }

    /**
     * Unregister the provider url from registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void unregister(Url providerUrl) {
        Validate.notNull(providerUrl, "Provider url must NOT be null!");
        doUnregister(removeUnnecessaryParams(providerUrl.copy()));
        log.info("Unregistered the url [{}] from registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
        // Removed it from the container after unregistered
        registeredProviderUrls.remove(providerUrl);
    }

    /**
     * Register the url to 'active' node of registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void activate(Url providerUrl) {
        if (providerUrl != null) {
            doActivate(removeUnnecessaryParams(providerUrl.copy()));
            log.info("Activated the url [{}] on registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
        } else {
            // Move all the provider urls to 'active' node
            doActivate(null);
        }
    }

    /**
     * Register the url to 'inactive' node of registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void deactivate(Url providerUrl) {
        if (providerUrl != null) {
            doDeactivate(removeUnnecessaryParams(providerUrl.copy()));
            log.info("Deactivated the url [{}] on registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
        } else {
            doDeactivate(null);
        }
    }

    /**
     * Remove the unnecessary url param to register to registry in order to not to be seen by consumer
     *
     * @param url url
     */
    private Url removeUnnecessaryParams(Url url) {
        // codec parameter can not be registered to registry,
        // because client side may could not request successfully if client side does not have the codec.
        url.getOptions().remove(CODEC);
        return url;
    }

    /**
     * Subscribe the consumer url to specified listener
     *
     * @param consumerUrl consumer url
     * @param listener    listener
     */
    @Override
    public void subscribe(Url consumerUrl, ClientListener listener) {
        Validate.notNull(consumerUrl, "Client url must NOT be null!");
        Validate.notNull(listener, "Client listener must NOT be null!");

        doSubscribe(consumerUrl, listener);
        log.info("Subscribed the url [{}] to listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    /**
     * Unsubscribe the url from specified listener
     *
     * @param consumerUrl provider url
     * @param listener    listener
     */
    @Override
    public void unsubscribe(Url consumerUrl, ClientListener listener) {
        Validate.notNull(consumerUrl, "Client url must NOT be null!");
        Validate.notNull(listener, "Client listener must NOT be null!");

        doUnsubscribe(consumerUrl, listener);
        log.info("Unsubscribed the url [{}] from listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    /**
     * todo: check usage
     * Get all the provider urls based on the consumer url
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Url> discover(Url consumerUrl) {
        if (consumerUrl == null) {
            log.warn("Url must NOT be null!");
            return Collections.EMPTY_LIST;
        }
        List<Url> results = new ArrayList<>();
        Map<String, List<Url>> urlsPerType = providerUrlsPerTypePerConsumerUrl.get(consumerUrl);
        if (MapUtils.isNotEmpty(urlsPerType)) {
            // Get all the provider urls from cache no matter what the type is
            results = urlsPerType.values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            // Read the provider urls from registry if local cache does not exist
            List<Url> discoveredUrls = doDiscover(consumerUrl);
            if (CollectionUtils.isNotEmpty(discoveredUrls)) {
                // Make a url copy and add to results
                results = discoveredUrls.stream().map(Url::copy).collect(Collectors.toList());
            }
        }
        return results;
    }

    /**
     * Get provider urls from cache
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    protected List<Url> getCachedProviderUrls(Url consumerUrl) {
        Map<String, List<Url>> urls = providerUrlsPerTypePerConsumerUrl.get(consumerUrl);
        if (MapUtils.isEmpty(urls)) {
            return Collections.emptyList();
        }
        return urls.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Group urls by url type parameter, and put it in local cache, then execute the listener
     *
     * @param providerUrls provider urls
     * @param consumerUrl  consumer url
     * @param listener     listener
     */
    protected void notify(List<Url> providerUrls, Url consumerUrl, ClientListener listener) {
        if (listener == null || CollectionUtils.isEmpty(providerUrls)) {
            return;
        }
        // Group urls by type parameter value of url
        Map<String, List<Url>> providerUrlsPerType = groupUrlsByType(providerUrls);

        Map<String, List<Url>> cachedProviderUrlsPerType = providerUrlsPerTypePerConsumerUrl.get(consumerUrl);
        if (cachedProviderUrlsPerType == null) {
            cachedProviderUrlsPerType = new ConcurrentHashMap<>();
            providerUrlsPerTypePerConsumerUrl.putIfAbsent(consumerUrl, cachedProviderUrlsPerType);
        }

        // Update urls cache
        cachedProviderUrlsPerType.putAll(providerUrlsPerType);

        for (Map.Entry<String, List<Url>> entry : providerUrlsPerType.entrySet()) {
            @EventPublisher("providersDiscoveryEvent")
            List<Url> providerUrlList = entry.getValue();
            listener.onNotify(registryUrl, providerUrlList);
        }
    }

    /**
     * Group urls by type parameter value of url
     *
     * @param urls urls
     * @return grouped url per url parameter type
     */
    private Map<String, List<Url>> groupUrlsByType(List<Url> urls) {
        Map<String, List<Url>> urlsPerType = new HashMap<>();
        for (Url url : urls) {
            String type = url.getOption(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
            List<Url> urlList = urlsPerType.computeIfAbsent(type, k -> new ArrayList<>());
            urlList.add(url);
        }
        return urlsPerType;
    }

    protected abstract void doRegister(Url url);

    protected abstract void doUnregister(Url url);

    protected abstract void doActivate(Url url);

    protected abstract void doDeactivate(Url url);

    protected abstract List<Url> discoverActiveProviders(Url consumerUrl);

    protected abstract void doSubscribe(Url url, ClientListener listener);

    protected abstract void doUnsubscribe(Url url, ClientListener listener);

    protected abstract void subscribeProviderListener(Url consumerUrl, ProviderListener listener);

    protected abstract void unsubscribeProviderListener(Url consumerUrl, ProviderListener listener);

    protected abstract List<Url> doDiscover(Url url);
}
