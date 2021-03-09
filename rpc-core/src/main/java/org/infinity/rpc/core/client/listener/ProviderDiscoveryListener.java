package org.infinity.rpc.core.client.listener;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.cluster.ProviderCluster;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.EventSubscriber;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;

import java.util.List;

/**
 * todo: see ClusterSupport
 * Listener used to subscribe providers change event,
 * method {@link ProviderDiscoveryListener#onNotify(Url, List)} will be invoked if providers change event occurs.
 *
 */
@Slf4j
@ThreadSafe
public class ProviderDiscoveryListener extends ProviderNotifyListener {
    private Url clientUrl;

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderDiscoveryListener() {
        super();
    }

    /**
     * Pass provider cluster to listener, listener will update provider cluster after provider urls changed
     *
     * @param providerCluster provider cluster
     * @param interfaceName   The interface class name of the consumer
     * @param clientUrl       client url
     * @return listener listener
     */
    public static  ProviderDiscoveryListener of(ProviderCluster providerCluster, String interfaceName, Url clientUrl) {
        ProviderDiscoveryListener listener = new ProviderDiscoveryListener();
        listener.providerCluster = providerCluster;
        listener.interfaceName = interfaceName;
        listener.clientUrl = clientUrl;
        listener.protocol = Protocol.getInstance(clientUrl.getProtocol());
        return listener;
    }

    /**
     * IMPORTANT: Subscribe this client listener to all the registries
     * So when providers change event occurs, it can invoke onNotify() method.
     *
     * @param registryUrls registry urls
     */
    @EventSubscriber("providersDiscoveryEvent")
    public void subscribe(List<Url> registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            // Bind this listener to the client
            registry.subscribe(clientUrl, this);
        }
    }

    @Override
    public String toString() {
        return ProviderDiscoveryListener.class.getSimpleName().concat(":").concat(interfaceName);
    }
}
