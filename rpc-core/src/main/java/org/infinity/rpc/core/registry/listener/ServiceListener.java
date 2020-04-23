package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.registry.Url;

import java.util.List;


public interface ServiceListener {

    void notifyService(Url refUrl, Url registryUrl, List<Url> urls);

}