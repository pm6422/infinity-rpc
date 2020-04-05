package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.server.RpcServer;
import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableConfigurationProperties({InfinityRpcProperties.class})
@Configuration
public class RpcServerAutoConfiguration {

    @Autowired
    private InfinityRpcProperties infinityRpcProperties;

    @Bean
    public ZkRpcServerRegistry rpcServiceRegistry() {
        return new ZkRpcServerRegistry(infinityRpcProperties.getRegistry().getAddress());
    }

    @Bean
    public RpcServer rpcServer() {
        return new RpcServer(infinityRpcProperties.getProtocol().getPort(), rpcServiceRegistry());
    }
}
