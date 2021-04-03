package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.url.Url;

import java.util.List;

/**
 * Listener of provider used to handle the subscribed event
 */
public interface ServiceListener {

    void onNotify(Url consumerUrl, Url registryUrl, List<Url> providerUrls);
}