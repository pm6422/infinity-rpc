package org.infinity.rpc.transport.netty4;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.transport.SharedObjectFactory;
import org.infinity.rpc.utilities.threadpool.DefaultThreadFactory;
import org.infinity.rpc.utilities.threadpool.StandardThreadExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class NettyChannelFactory implements SharedObjectFactory<NettyChannel> {
    private static final ExecutorService rebuildExecutorService = new StandardThreadExecutor(5, 30, 10L, TimeUnit.SECONDS, 100,
            new DefaultThreadFactory("RebuildExecutorService", true),
            new ThreadPoolExecutor.CallerRunsPolicy());
    private              NettyClient     nettyClient;
    private              String          factoryName;

    public NettyChannelFactory(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.factoryName = "NettyChannelFactory_" + nettyClient.getUrl().getHost() + "_" + nettyClient.getUrl().getPort();
    }

    @Override
    public NettyChannel buildObject() {
        return new NettyChannel(nettyClient);
    }

    @Override
    public boolean rebuildObject(NettyChannel nettyChannel, boolean async) {
        ReentrantLock lock = nettyChannel.getLock();
        if (lock.tryLock()) {
            try {
                if (!nettyChannel.isActive() && !nettyChannel.isReconnect()) {
                    nettyChannel.reconnect();
                    if (async) {
                        rebuildExecutorService.submit(new RebuildTask(nettyChannel));
                    } else {
                        nettyChannel.close();
                        nettyChannel.open();
                        log.info("rebuild channel success: " + nettyChannel.getUrl());
                    }
                }
            } catch (Exception e) {
                log.error("rebuild error: " + this.toString() + ", " + nettyChannel.getUrl(), e);
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return factoryName;
    }

    class RebuildTask implements Runnable {
        private NettyChannel channel;

        public RebuildTask(NettyChannel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                channel.getLock().lock();
                channel.close();
                channel.open();
                log.info("rebuild channel success: " + channel.getUrl());
            } catch (Exception e) {
                log.error("rebuild error: " + this.toString() + ", " + channel.getUrl(), e);
            } finally {
                channel.getLock().unlock();
            }
        }
    }
}
