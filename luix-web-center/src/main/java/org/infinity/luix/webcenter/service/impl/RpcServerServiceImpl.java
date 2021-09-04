package org.infinity.luix.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.webcenter.domain.RpcServer;
import org.infinity.luix.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.luix.core.client.proxy.Proxy;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.core.client.stub.ConsumerStubFactory;
import org.infinity.luix.core.config.impl.RegistryConfig;
import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.server.buildin.ServerInfo;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.spring.boot.config.InfinityProperties;
import org.infinity.luix.webcenter.repository.RpcServerRepository;
import org.infinity.luix.webcenter.service.RpcConsumerService;
import org.infinity.luix.webcenter.service.RpcProviderService;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.infinity.luix.webcenter.service.RpcServerService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Pattern;

import static org.infinity.luix.core.server.buildin.BuildInService.METHOD_GET_SERVER_INFO;
import static org.infinity.luix.webcenter.domain.RpcService.generateMd5Id;

@Service
public class RpcServerServiceImpl implements RpcServerService {

    @Resource
    private InfinityProperties  infinityProperties;
    @Resource
    private ApplicationContext  applicationContext;
    @Resource
    private MongoTemplate       mongoTemplate;
    @Resource
    private RpcServerRepository rpcServerRepository;
    @Resource
    private RpcProviderService  rpcProviderService;
    @Resource
    private RpcConsumerService  rpcConsumerService;

    @Override
    public boolean exists(String registryIdentity, String address) {
        return rpcServerRepository.existsByRegistryIdentityAndAddress(registryIdentity, address);
    }

    @Override
    public Page<RpcServer> find(Pageable pageable, String registryIdentity, String address) {
        Query query = Query.query(Criteria.where(RpcServer.FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(address)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + address + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(RpcServer.FIELD_ADDRESS).regex(pattern));
        }
        long totalCount = mongoTemplate.count(query, RpcServer.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcServer.class), pageable, totalCount);
    }

    @Override
    public void inactivate(String registryIdentity, String address) {
        if (!rpcProviderService.existsService(registryIdentity, address, true)
                && !rpcConsumerService.existsService(registryIdentity, address, true)) {
            RpcServer rpcServer = rpcServerRepository.findByRegistryIdentityAndAddress(registryIdentity, address);
            if (rpcServer == null) {
                return;
            }
            rpcServer.setActive(false);
            rpcServerRepository.save(rpcServer);
        }
    }

    @Override
    public RpcServer loadServer(Url registryUrl, Url url) {
        return loadServer(registryUrl.getIdentity(), url.getAddress());
    }

    @Override
    public RpcServer loadServer(String registryIdentity, String address) {
        RpcRegistryService rpcRegistryService = applicationContext.getBean(RpcRegistryService.class);
        RegistryConfig registryConfig = rpcRegistryService.findRegistryConfig(registryIdentity);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(infinityProperties.getApplication(), registryConfig,
                infinityProperties.getAvailableProtocol(), address, BuildInService.class.getName(),
                10000, 2);

        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ServerInfo serverInfo = (ServerInfo) invocationHandler.invoke(METHOD_GET_SERVER_INFO);

        RpcServer rpcServer = new RpcServer();
        BeanUtils.copyProperties(serverInfo, rpcServer);
        String id = generateMd5Id(address, registryIdentity);
        rpcServer.setId(id);
        rpcServer.setRegistryIdentity(registryIdentity);
        rpcServer.setAddress(address);
        rpcServer.setActive(true);
        return rpcServer;
    }
}
