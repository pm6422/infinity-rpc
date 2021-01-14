package org.infinity.rpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.server.exporter.Exportable;
import org.infinity.rpc.core.config.spring.server.exporter.impl.DefaultRpcExporter;
import org.infinity.rpc.core.config.spring.server.messagehandler.impl.ProviderMessageRouter;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.protocol.AbstractProtocol;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServiceName("infinity")
@Slf4j
public class InfinityProtocol extends AbstractProtocol {

    /**
     * 多个service可能在相同端口进行服务暴露，因此来自同个端口的请求需要进行路由以找到相应的服务，同时不在该端口暴露的服务不应该被找到
     */
    private final Map<String, ProviderMessageRouter> ipPort2RequestRouter = new ConcurrentHashMap<>();

    @Override
    protected <T> Exportable<T> createExporter(ProviderWrapper<T> provider) {
        return new DefaultRpcExporter<>(provider, this.ipPort2RequestRouter, this.exporterMap);
    }
}
