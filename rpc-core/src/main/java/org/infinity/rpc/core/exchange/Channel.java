package org.infinity.rpc.core.exchange;

import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.impl.TransportException;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;

public interface Channel {

    /**
     * Send request
     *
     * @param request request object
     * @return response object
     * @throws TransportException if exception occurs
     */
    Responseable request(Requestable request) throws TransportException;

    /**
     * Open the channel
     *
     * @return {@code true} if it was opened and {@code false} otherwise
     */
    boolean open();

    /**
     * Close the channel
     */
    void close();

    /**
     * Close the channel with a timeout
     *
     * @param timeout timeout in @todo
     */
    void close(int timeout);

    /**
     * Get local socket address
     *
     * @return local address
     */
    InetSocketAddress getLocalAddress();

    /**
     * Get remote socket address
     *
     * @return remote address
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Get state
     *
     * @return state
     */
    ChannelState getState();

    /**
     * Check whether it is closed or not
     *
     * @return {@code true} if it was closed and {@code false} otherwise
     */
    boolean isClosed();

    /**
     * Check node availability status
     *
     * @return {@code true} if it was active and {@code false} otherwise
     */
    boolean isActive();

    /**
     * Get the provider url
     *
     * @return url provider url
     */
    Url getProviderUrl();
}