package org.infinity.rpc.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.common.RpcDecoder;
import org.infinity.rpc.common.RpcEncoder;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.springframework.aop.support.AopUtils;

import java.util.Map;

/**
 * RPC服务器类，使用Spring
 */
@Slf4j
public class RpcServer {
    // key: serviceInterfaceName, value: serviceImpl
    private Map<String, Object> rpcProviderMap;
    private ZkRpcServerRegistry zkRpcServerRegistry;
    private String              serverAddress;
    private String              serverIp;
    private int                 serverPort;

    public RpcServer(String serverAddress, ZkRpcServerRegistry rpcServerRegistry, Map<String, Object> rpcProviderMap) {
        log.info("Starting RPC server on [{}]", serverAddress);
        this.serverAddress = serverAddress;
        String[] ipAndPortParts = serverAddress.split(":");
        serverIp = ipAndPortParts[0];
        serverPort = Integer.valueOf(ipAndPortParts[1]);
        this.zkRpcServerRegistry = rpcServerRegistry;
        this.rpcProviderMap = rpcProviderMap;
    }

    private Class getTargetClass(Object bean) {
        if (isProxyBean(bean)) {
            return AopUtils.getTargetClass(bean);
        }
        return bean.getClass();
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }

    /**
     * Start netty server
     */
    public void startNettyServer() {
        // 创建服务端的通信对象
        ServerBootstrap server = new ServerBootstrap();
        // 创建异步通信的事件组 用于建立TCP连接的
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // 创建异步通信的事件组，用于处理Channel(通道)的I/O事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 开始设置server的相关参数
            server.group(bossGroup, workerGroup)
                    // 启动异步ServerSocket
                    .channel(NioServerSocketChannel.class)
                    // 初始化通道信息
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcDecoder(RpcRequest.class))//1.解码请求对象
                                    .addLast(new RpcEncoder(RpcResponse.class))//2.编码响应对象
                                    .addLast(new RpcServerHandler(rpcProviderMap));//3.请求处理
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ;
            // 开启异步通信服务
            ChannelFuture future = server.bind(serverIp, serverPort).sync();

            // Register RPC server on registry
            this.registerRpcServer();
            log.info("Started RPC server on [{}]", serverAddress);
            // 线程阻塞在此，程序暂停执行，等待通信完成
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Failed to start netty", e.getMessage());
        } finally {
            // 优雅的关闭socket
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Register RPC server on registry
     *
     * @throws Exception
     */
    private void registerRpcServer() throws Exception {
        log.info("Registering RPC server address [{}] on registry", serverAddress);
        zkRpcServerRegistry.createRpcServerNode(serverAddress);
        zkRpcServerRegistry.startWatchNode();
        log.info("Registered RPC server address [{}] on registry", serverAddress);
    }
}
