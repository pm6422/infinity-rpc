package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.url.Url;

/**
 * Listener of command used to handle the subscribed event
 */
public interface CommandListener {

    void onNotify(Url consumerUrl, String commandString);
}