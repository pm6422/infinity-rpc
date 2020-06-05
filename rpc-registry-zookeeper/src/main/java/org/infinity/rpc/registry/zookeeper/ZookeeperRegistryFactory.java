package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.registry.AbstractRegistryFactory;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

@ServiceName("zookeeper")
@Slf4j
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    /**
     * Create a zookeeper registry
     *
     * @param registryUrl registry URL
     * @return registry instance
     */
    @Override
    public Registry createRegistry(Url registryUrl) {
        int connectTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
        int sessionTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
        ZkClient zkClient = createZkClient(registryUrl.getParameter(Url.PARAM_ADDRESS), sessionTimeout, connectTimeout);
        return new ZookeeperRegistry(registryUrl, zkClient);
    }

    private ZkClient createZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        return new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }
}
