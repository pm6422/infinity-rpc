package org.infinity.luix.webcenter.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.luix.core.client.proxy.Proxy;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.core.client.stub.ConsumerStubFactory;
import org.infinity.luix.core.config.impl.ApplicationConfig;
import org.infinity.luix.core.config.impl.RegistryConfig;
import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.spring.boot.config.LuixProperties;
import org.infinity.luix.webcenter.domain.RpcApplication;
import org.infinity.luix.webcenter.repository.RpcApplicationRepository;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.infinity.luix.webcenter.service.RpcConsumerService;
import org.infinity.luix.webcenter.service.RpcProviderService;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.luix.core.server.buildin.BuildInService.METHOD_GET_APPLICATION_INFO;
import static org.infinity.luix.webcenter.domain.RpcApplication.FIELD_ACTIVE;
import static org.infinity.luix.webcenter.domain.RpcApplication.FIELD_NAME;
import static org.infinity.luix.webcenter.domain.RpcProvider.FIELD_REGISTRY_IDENTITY;
import static org.infinity.luix.webcenter.domain.RpcService.generateMd5Id;

@RpcProvider
public class RpcApplicationServiceImpl implements RpcApplicationService {
    @Resource
    private LuixProperties     luixProperties;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private MongoTemplate            mongoTemplate;
    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcProviderService       rpcProviderService;
    @Resource
    private RpcConsumerService       rpcConsumerService;

    @Override
    public void updateStatus() {
        List<RpcApplication> applications = rpcApplicationRepository.findAll();
        if (CollectionUtils.isEmpty(applications)) {
            return;
        }
        applications.forEach(domain -> {
            if (rpcProviderService.existsApplication(domain.getRegistryIdentity(), domain.getName(), true)) {
                domain.setProviding(true);
                domain.setActive(true);
            }
            if (rpcConsumerService.existsApplication(domain.getRegistryIdentity(), domain.getName(), true)) {
                domain.setConsuming(true);
                domain.setActive(true);
            }
        });
        rpcApplicationRepository.saveAll(applications);
    }

    @Override
    public Page<RpcApplication> find(Pageable pageable, String registryIdentity, String name, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(name)) {
            // Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_NAME).regex(pattern));
        }
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }

        long totalCount = mongoTemplate.count(query, RpcApplication.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcApplication.class), pageable, totalCount);
    }

    @Override
    public void inactivate(String registryIdentity, String name) {
        if (!rpcProviderService.existsApplication(registryIdentity, name, true)
                && !rpcConsumerService.existsApplication(registryIdentity, name, true)) {
            Optional<RpcApplication> application = rpcApplicationRepository
                    .findByRegistryIdentityAndName(registryIdentity, name);
            if (!application.isPresent()) {
                return;
            }
            application.get().setActive(false);
            rpcApplicationRepository.save(application.get());
        }
    }

    @Override
    public RpcApplication loadApplication(Url registryUrl, Url url) {
        RpcRegistryService rpcRegistryService = applicationContext.getBean(RpcRegistryService.class);
        RegistryConfig registryConfig = rpcRegistryService.findRegistryConfig(registryUrl.getIdentity());
        Proxy proxyFactory = Proxy.getInstance(luixProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixProperties.getApplication(), registryConfig,
                luixProperties.getAvailableProtocol(), url.getAddress(), BuildInService.class.getName(),
                10000, 2);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig) invocationHandler.invoke(METHOD_GET_APPLICATION_INFO);

        RpcApplication rpcApplication = new RpcApplication();
        BeanUtils.copyProperties(applicationConfig, rpcApplication);
        String id = generateMd5Id(rpcApplication.getName(), registryUrl.getIdentity());
        rpcApplication.setId(id);
        rpcApplication.setRegistryIdentity(registryUrl.getIdentity());
        rpcApplication.setActive(true);
        rpcApplication.setCreatedTime(Instant.now());
        rpcApplication.setModifiedTime(rpcApplication.getCreatedTime());
        return rpcApplication;
    }
}
