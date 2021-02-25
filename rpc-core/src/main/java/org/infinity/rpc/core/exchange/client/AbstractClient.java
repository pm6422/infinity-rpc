package org.infinity.rpc.core.exchange.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;

import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC;
import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC_VAL_DEFAULT;

@Slf4j
public abstract class AbstractClient implements Client {
    /**
     * Set default state with ChannelState.UNINITIALIZED
     */
    protected volatile ChannelState      state = ChannelState.UNINITIALIZED;
    protected          Url               providerUrl;
    protected          Codec             codec;
    protected          InetSocketAddress localAddress;
    protected          InetSocketAddress remoteAddress;

    public AbstractClient(Url providerUrl) {
        this.providerUrl = providerUrl;
        String codecName = providerUrl.getOption(CODEC, CODEC_VAL_DEFAULT);
        this.codec = Codec.getInstance(codecName);
        if (codec == null) {
            throw new RpcFrameworkException("Codec [" + codecName + "] must not be null!");
        }
        log.info("Initializing client with codec {} for url: {}", codec.getClass().getSimpleName(), providerUrl);
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}