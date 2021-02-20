package org.infinity.rpc.core.exchange.server;

import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;
import java.util.Collection;

import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC;
import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC_DEFAULT_VALUE;

public abstract class AbstractServer implements Server {
    protected InetSocketAddress localAddress;
    protected InetSocketAddress remoteAddress;

    protected Url   url;
    protected Codec codec;

    protected volatile ChannelState state = ChannelState.UNINITIALIZED;


    public AbstractServer() {
    }

    public AbstractServer(Url providerUrl) {
        this.url = providerUrl;
        this.codec = Codec.getInstance(providerUrl.getOption(CODEC, CODEC_DEFAULT_VALUE));
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public Collection<Channel> getChannels() {
        throw new RpcFrameworkException(this.getClass().getName() + " getChannels() method unsupport " + url);
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        throw new RpcFrameworkException(this.getClass().getName() + " getChannel(InetSocketAddress) method unsupport " + url);
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

}
