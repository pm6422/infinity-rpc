package org.infinity.rpc.core.exchange.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.lang.MathUtils;
import org.infinity.rpc.utilities.threadpool.DefaultThreadFactory;
import org.infinity.rpc.utilities.threadpool.StandardThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;

@Slf4j
public abstract class AbstractSharedPoolClient extends AbstractClient {

    private static final ThreadPoolExecutor           THREAD_POOL = new StandardThreadExecutor(1, 300,
            20000, new DefaultThreadFactory(AbstractSharedPoolClient.class.getSimpleName(), true));
    private final        AtomicInteger                idx         = new AtomicInteger();
    /**
     * Object factory used to build channel
     */
    private              SharedObjectFactory<Channel> factory;
    private final        int                          channelSize;
    private              List<Channel>                channels;

    public AbstractSharedPoolClient(Url providerUrl) {
        super(providerUrl);
        channelSize = providerUrl.getIntOption(MIN_CLIENT_CONN, MIN_CLIENT_CONN_DEFAULT_VALUE);
    }

    protected void initPool() {
        factory = createChannelFactory();
        channels = new ArrayList<>(channelSize);
        IntStream.range(0, channelSize).forEach(x -> channels.add(factory.buildObject()));
        initConnections(providerUrl.getBooleanOption(ASYNC_INIT_CONN, ASYNC_INIT_CONN_DEFAULT_VALUE));
    }

    protected void initConnections(boolean async) {
        if (async) {
            THREAD_POOL.execute(this::createConnections);
        } else {
            createConnections();
        }
    }

    private void createConnections() {
        for (Channel channel : channels) {
            try {
                channel.open();
            } catch (Exception e) {
                log.error("create connect Error: url=" + providerUrl.getUri(), e);
            }
        }
    }

    protected Channel getChannel() {
        int index = MathUtils.getNonNegativeRange24bit(idx.getAndIncrement());
        Channel channel;

        for (int i = index; i < channelSize + 1 + index; i++) {
            channel = channels.get(i % channelSize);
            if (!channel.isActive()) {
                factory.rebuildObject(channel, i != channelSize + 1);
            }
            if (channel.isActive()) {
                return channel;
            }
        }

        String errorMsg = this.getClass().getSimpleName() + " getChannel Error: url=" + providerUrl.getUri();
        log.error(errorMsg);
        throw new RpcServiceException(errorMsg);
    }

    protected void closeAllChannels() {
        channels.forEach(Channel::close);
    }

    /**
     * Create channel factory
     *
     * @return {@link SharedObjectFactory} instance
     */
    protected abstract SharedObjectFactory<Channel> createChannelFactory();
}
