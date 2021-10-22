package org.infinity.luix.transport.netty4.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.constant.RpcConstants;
import org.infinity.luix.core.exception.ExceptionUtils;
import org.infinity.luix.core.exception.RpcAbstractException;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.exchange.client.AbstractPooledClient;
import org.infinity.luix.core.exchange.client.SharedObjectFactory;
import org.infinity.luix.core.exchange.constants.ChannelState;
import org.infinity.luix.core.server.response.FutureResponse;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.server.response.impl.RpcResponse;
import org.infinity.luix.core.thread.ScheduledThreadPool;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.transport.netty4.NettyDecoder;
import org.infinity.luix.transport.netty4.NettyEncoder;
import org.infinity.luix.transport.netty4.NettyServerClientHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.infinity.luix.core.constant.ProtocolConstants.*;
import static org.infinity.luix.core.constant.RegistryConstants.CONNECT_TIMEOUT;
import static org.infinity.luix.core.constant.RegistryConstants.CONNECT_TIMEOUT_VAL_DEFAULT;
import static org.infinity.luix.core.thread.ScheduledThreadPool.DESTROY_NETTY_TIMEOUT_INTERVAL;
import static org.infinity.luix.core.thread.ScheduledThreadPool.DESTROY_NETTY_TIMEOUT_TASK_THREAD_POOL;

/**
 * toto: implements StatisticCallback
 */
@Slf4j
public class NettyClient extends AbstractPooledClient {
    private static final NioEventLoopGroup         NIO_EVENT_LOOP_GROUP  = new NioEventLoopGroup();
    /**
     * Async response used to handle async request
     */
    private final        Map<Long, FutureResponse> requestId2ResponseMap = new ConcurrentHashMap<>();
    /**
     * Invocation error count
     */
    private final        AtomicLong                errorCount            = new AtomicLong(0);
    private final        ScheduledFuture<?>        timeoutFuture;
    private final        int                       maxClientFailedConn;
    private              Bootstrap                 bootstrap;

    public NettyClient(Url providerUrl) {
        super(providerUrl);
        maxClientFailedConn = providerUrl.getIntOption(MAX_CLIENT_FAILED_CONN, MAX_CLIENT_FAILED_CONN_VAL_DEFAULT);
        timeoutFuture = ScheduledThreadPool.schedulePeriodicalTask(DESTROY_NETTY_TIMEOUT_TASK_THREAD_POOL,
                DESTROY_NETTY_TIMEOUT_INTERVAL, this::recycleTimeoutTask);
    }

    private void recycleTimeoutTask() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Long, FutureResponse> request2ResponseEntry : requestId2ResponseMap.entrySet()) {
            try {
                FutureResponse futureResponse = request2ResponseEntry.getValue();
                if (futureResponse.getCreatedTime() + futureResponse.getTimeout() < currentTime) {
                    // If timeout, remove response and then cancel
                    removeResponse(request2ResponseEntry.getKey());
                    futureResponse.cancel();
                }
            } catch (Exception e) {
                log.error("Failed to recycle the request callback for uri [" + providerUrl.getUri() + "]", e);
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected SharedObjectFactory createChannelFactory() {
        return new NettyChannelFactory(this);
    }

    @Override
    public Responseable request(Requestable request) {
        if (!isActive()) {
            throw new RpcFrameworkException("Client status is inactive for url [" + providerUrl.getUri()
                    + "] and request " + request);
        }
        return doRequest(request);
    }

    private Responseable doRequest(Requestable request) {
        Responseable response;
        try {
            Channel channel = getChannel();
            RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_CONNECTION);

            if (channel == null) {
                log.error("No channel found for request {}", request);
                return null;
            }
            // All requests are handled asynchronously and return type always be RpcFutureResponse
            response = channel.request(request);
        } catch (Exception e) {
            if (ExceptionUtils.isBizException(e)) {
                throw (RpcAbstractException) e;
            } else {
                throw new RpcFrameworkException("Failed to process request " + request, e);
            }
        }

        // Return RpcFutureResponse directly or convert RpcFutureResponse to RpcResponse
        response = asyncResponse(response, request.isAsync());
        return response;
    }

    /**
     * @param response response
     * @param async    async flag
     * @return response
     */
    private Responseable asyncResponse(Responseable response, boolean async) {
        // If it is asynchronous call, return RpcFutureResponse directly
        // If it is synchronous call, firstly it takes time to convert RpcFutureResponse to RpcResponse, and then return it.
        return async ? response : RpcResponse.of(response);
    }

    @Override
    public synchronized boolean open() {
        if (isActive()) {
            return true;
        }

        bootstrap = new Bootstrap();
        int timeout = getProviderUrl().getIntOption(CONNECT_TIMEOUT, CONNECT_TIMEOUT_VAL_DEFAULT);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // 最大响应包限制
        final int maxContentLength = providerUrl.getIntOption(MAX_CONTENT_LENGTH, MAX_CONTENT_LENGTH_VAL_DEFAULT);
        bootstrap.group(NIO_EVENT_LOOP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(NettyEncoder.ENCODER, new NettyEncoder());
                        pipeline.addLast(NettyDecoder.DECODER, new NettyDecoder(codec, NettyClient.this, maxContentLength));
                        pipeline.addLast(NettyServerClientHandler.HANDLER, createServerClientHandler());
                    }
                });

        // 初始化连接池
        initPool();

        log.info("Opened the netty client for url [{}]", providerUrl);

//         注册统计回调
//        StatsUtil.registryStatisticCallback(this);
//         设置可用状态

        state = ChannelState.ACTIVE;
        return true;
    }

    private NettyServerClientHandler createServerClientHandler() {
        return new NettyServerClientHandler(NettyClient.this, (channel, message) -> {
            Responseable response = (Responseable) message;
            FutureResponse futureResponse = NettyClient.this.removeResponse(response.getRequestId());
            if (futureResponse == null) {
                log.warn("No response found with request ID: [{}]", response.getRequestId());
                return null;
            }
            if (response.getException() != null) {
                futureResponse.onFailure(response);
            } else {
                futureResponse.onSuccess(response);
            }
            return null;
        });
    }

    @Override
    public synchronized void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {
        if (state.isClosed()) {
            return;
        }

        try {
            cleanup();
            if (state.isCreated()) {
                log.info("Current status is uninitialized for uri [{}]", providerUrl.getUri());
                return;
            }

            // 设置close状态
            state = ChannelState.CLOSED;
            log.info("Closed netty client for uri [{}]", providerUrl.getUri());
        } catch (Exception e) {
            log.error("Failed to close netty client for uri [" + providerUrl.getUri() + "]", e);
        }
    }

    public void cleanup() {
        // 取消定期的回收任务
        timeoutFuture.cancel(true);
        // 清空callback
        requestId2ResponseMap.clear();
        // 关闭client持有的channel
        closeAllChannels();
        // 解除统计回调的注册
//        StatsUtil.unRegistryStatisticCallback(this);
    }

    @Override
    public ChannelState getState() {
        return state;
    }

    @Override
    public boolean isClosed() {
        return state.isClosed();
    }

    @Override
    public boolean isActive() {
        return state.isActive();
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public FutureResponse removeResponse(long requestId) {
        return requestId2ResponseMap.remove(requestId);
    }

    /**
     * 增加调用失败的次数
     *
     * <pre>
     * 如果连续失败的次数 >= maxClientConnection，那么把client设置成不可用状态
     * </pre>
     */
    void incrErrorCount() {
        long count = errorCount.incrementAndGet();

        // 如果节点是可用状态，同时当前连续失败的次数超过允许的最大失败连接数，那么把该节点标示为不可用
        if (count >= maxClientFailedConn && state.isActive()) {
            synchronized (this) {
                count = errorCount.longValue();

                if (count >= maxClientFailedConn && state.isActive()) {
                    log.error("Changed current state of netty client to inactive for uri [{}]", providerUrl.getUri());
                    state = ChannelState.INACTIVE;
                }
            }
        }
    }

    /**
     * Reset invocation error count and set state to 'ACTIVE'
     */
    public void resetInvocationError() {
        errorCount.set(0);

        if (state.isActive()) {
            return;
        }

        synchronized (this) {
            if (state.isActive()) {
                return;
            }
            // 如果节点是inactive才进行设置，而如果是close或者uninitialized，那么直接忽略
            if (state.isInactive()) {
                long count = errorCount.longValue();

                // 过程中有其他并发更新errorCount的，因此这里需要进行一次判断
                if (count < maxClientFailedConn) {
                    state = ChannelState.ACTIVE;
                    log.info("Recovered the state of netty client to active for url [{}]", providerUrl.getUri());
                }
            }
        }
    }


    /**
     * Register response associated with request ID and limit the concurrent requests
     *
     * @param requestId      request ID
     * @param futureResponse response future
     */
    public void registerResponse(long requestId, FutureResponse futureResponse) {
        if (this.requestId2ResponseMap.size() >= RpcConstants.NETTY_CLIENT_MAX_REQUEST) {
            throw new RpcFrameworkException("Discarded the request [" + requestId + "] " +
                    "and url [" + providerUrl.getUri() + "] for exceeding max request limit!");
        }
        this.requestId2ResponseMap.put(requestId, futureResponse);
    }

    @Override
    public void checkHealth(Requestable request) {
        try {
            log.info("Checking health for url [{}]", providerUrl.getUri());
            doRequest(request);
        } catch (Exception e) {
            log.error("Failed to check health for url [" + providerUrl.getUri() + "]!", e);
        }
    }

    @Override
    public String toString() {
        return NettyClient.class.getSimpleName().concat(":").concat(getProviderUrl().getPath());
    }
}
