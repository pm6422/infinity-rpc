package com.luixtech.luixrpc.transport.netty4.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.client.request.Requestable;
import com.luixtech.luixrpc.core.exception.TransportException;
import com.luixtech.luixrpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.luixrpc.core.exchange.callback.StatisticCallback;
import com.luixtech.luixrpc.core.exchange.constants.ChannelState;
import com.luixtech.luixrpc.core.exchange.server.AbstractServer;
import com.luixtech.luixrpc.core.server.handler.InvocationHandleable;
import com.luixtech.luixrpc.core.server.response.Responseable;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.transport.netty4.NettyDecoder;
import com.luixtech.luixrpc.transport.netty4.NettyEncoder;
import com.luixtech.luixrpc.transport.netty4.NettyServerClientHandler;
import com.luixtech.luixrpc.utilities.threadpool.NamedThreadFactory;
import com.luixtech.luixrpc.utilities.threadpool.NetworkThreadPoolExecutor;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static com.luixtech.luixrpc.core.constant.ProtocolConstants.*;

@Slf4j
public class NettyServer extends AbstractServer implements StatisticCallback {
    protected NettyServerChannelManager channelManager;
    private   EventLoopGroup            bossGroup;
    private   EventLoopGroup            workerGroup;
    private   Channel                   serverChannel;
    private InvocationHandleable      handler;
    private NetworkThreadPoolExecutor networkThreadPoolExecutor;

    private AtomicInteger rejectCounter = new AtomicInteger(0);

    public AtomicInteger getRejectCounter() {
        return rejectCounter;
    }

    public NettyServer(Url providerUrl, InvocationHandleable handler) {
        super(providerUrl);
        this.handler = handler;
    }

    @Override
    public boolean isBound() {
        return serverChannel != null && serverChannel.isActive();
    }

    @Override
    public Responseable request(Requestable request) throws TransportException {
        throw new RpcFrameworkException("NettyServer request(Request request) method not support: url: " + providerUrl);
    }

    @Override
    public boolean open() {
        if (isActive()) {
            log.warn("Netty server channel already be opened for url [{}]", providerUrl);
            return state.isActive();
        }
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
        }

        log.info("NettyServer ServerChannel start Open: url=" + providerUrl);
        int maxContentLength = providerUrl.getIntOption(MAX_CONTENT_LENGTH, MAX_CONTENT_LENGTH_VAL_DEFAULT);
        int maxServerConn = providerUrl.getIntOption(MAX_SERVER_CONN, MAX_SERVER_CONN_VAL_DEFAULT);
        int workerQueueSize = providerUrl.getIntOption(WORK_QUEUE_SIZE, WORK_QUEUE_SIZE_VAL_DEFAULT);
        boolean shareChannel = providerUrl.getBooleanOption(SHARED_SERVER, SHARED_SERVER_VAL_DEFAULT);

        int minWorkerThread, maxWorkerThread;

        if (shareChannel) {
            minWorkerThread = providerUrl.getIntOption(MIN_THREAD, MIN_THREAD_SHARED_CHANNEL);
            maxWorkerThread = providerUrl.getIntOption(MAX_THREAD, MAX_THREAD_SHARED_CHANNEL);
        } else {
            minWorkerThread = providerUrl.getIntOption(MIN_THREAD, MIN_THREAD_VAL_DEFAULT);
            maxWorkerThread = providerUrl.getIntOption(MAX_THREAD, MAX_THREAD_VAL_DEFAULT);
        }

        networkThreadPoolExecutor = (networkThreadPoolExecutor != null && !networkThreadPoolExecutor.isShutdown()) ? networkThreadPoolExecutor
                : new NetworkThreadPoolExecutor(minWorkerThread, maxWorkerThread, workerQueueSize, new NamedThreadFactory("NettyServer-" + providerUrl.getAddress(), true));
        networkThreadPoolExecutor.prestartAllCoreThreads();

        channelManager = new NettyServerChannelManager(maxServerConn);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(NettyEncoder.ENCODER, new NettyEncoder());
                        pipeline.addLast(NettyDecoder.DECODER, new NettyDecoder(codec, NettyServer.this, maxContentLength));
                        pipeline.addLast(NettyServerClientHandler.HANDLER, createServerClientHandler());
                        pipeline.addLast(NettyServerChannelManager.CHANNEL_MANAGER, channelManager);
                    }
                });
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(providerUrl.getPort()));
        channelFuture.syncUninterruptibly();
        serverChannel = channelFuture.channel();
        state = ChannelState.ACTIVE;
//        StatsUtils.registryStatisticCallback(this);
        log.info("NettyServer ServerChannel finish Open: url=" + providerUrl);
        log.info("Started netty server with port [{}]", providerUrl.getPort());
        return state.isActive();
    }

    private NettyServerClientHandler createServerClientHandler() {
        return new NettyServerClientHandler(NettyServer.this, NettyServer.this.handler, networkThreadPoolExecutor);
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
                log.info("NettyServer close fail: state={}, url={}", state.value, providerUrl.getUri());
                return;
            }

            // 设置close状态
            state = ChannelState.CLOSED;
            log.info("NettyServer close Success: url={}", providerUrl.getUri());
        } catch (Exception e) {
            log.error("NettyServer close Error: url=" + providerUrl.getUri(), e);
        }
    }

    @Override
    public ChannelState getState() {
        return state;
    }

    public void cleanup() {
        // close listen socket
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        // close all clients's channel
        if (channelManager != null) {
            channelManager.close();
        }
        // shutdown the threadPool
        if (networkThreadPoolExecutor != null) {
            networkThreadPoolExecutor.shutdownNow();
        }
        // 取消统计回调的注册
//        StatsUtil.unRegistryStatisticCallback(this);
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

    @Override
    public String statisticCallback() {
        return String.format("identity: %s connectionCount: %s taskCount: %s queueCount: %s maxThreadCount: %s maxTaskCount: %s executorRejectCount: %s",
                providerUrl.getIdentity(), channelManager.getChannels().size(), networkThreadPoolExecutor.getSubmittedTasksCount(),
                networkThreadPoolExecutor.getQueue().size(), networkThreadPoolExecutor.getMaximumPoolSize(),
                networkThreadPoolExecutor.getMaxSubmittedTasksCount(), rejectCounter.getAndSet(0));
    }
}
