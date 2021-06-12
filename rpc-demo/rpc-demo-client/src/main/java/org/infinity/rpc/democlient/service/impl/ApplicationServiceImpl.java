package org.infinity.rpc.democlient.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Consumer;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.repository.ApplicationRepository;
import org.infinity.rpc.democlient.repository.ConsumerRepository;
import org.infinity.rpc.democlient.repository.ProviderRepository;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.config.impl.ProviderConfig.METHOD_APPLICATION_META;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
import static org.infinity.rpc.democlient.domain.Application.*;
import static org.infinity.rpc.democlient.domain.Provider.FIELD_REGISTRY_IDENTITY;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Resource
    private InfinityProperties    infinityProperties;
    @Resource
    private ApplicationContext    applicationContext;
    @Resource
    private MongoTemplate         mongoTemplate;
    @Resource
    private ProviderRepository    providerRepository;
    @Resource
    private ConsumerRepository    consumerRepository;
    @Resource
    private ApplicationRepository applicationRepository;

    @Override
    public Page<Application> find(Pageable pageable, String registryUrl, String name, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryUrl));
        if (StringUtils.isNotEmpty(name)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_NAME).regex(pattern));
        }
        if (active != null) {
            if (Boolean.TRUE.equals(active)) {
                // or criteria
                Criteria criteria = new Criteria().orOperator(
                        Criteria.where(FIELD_ACTIVE_PROVIDER).is(true),
                        Criteria.where(FIELD_ACTIVE_CONSUMER).is(true));
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where(FIELD_ACTIVE_PROVIDER).is(false));
                query.addCriteria(Criteria.where(FIELD_ACTIVE_CONSUMER).is(false));
            }
        }

        long totalCount = mongoTemplate.count(query, Application.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, Application.class), pageable, totalCount);
    }

    @Override
    public Application remoteQueryApplication(Url registryUrl, Url url) {
        Application application = new Application();
        RegistryService registryService = applicationContext.getBean(RegistryService.class);
        Url copy = url.copy();
        int timeout = 100000;
        copy.addOption(REQUEST_TIMEOUT, String.valueOf(timeout));
        ConsumerStub<?> consumerStub = registryService.getConsumerStub(registryUrl.getIdentity(), copy);
        consumerStub.setRequestTimeout(timeout);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig)
                invocationHandler.invoke(METHOD_APPLICATION_META, null, null);
        BeanUtils.copyProperties(applicationConfig, application);
        application.setRegistryIdentity(registryUrl.getIdentity());
        application.setActiveProvider(true);
        application.setActiveConsumer(false);
        return application;
    }

    @Override
    public void inactivate(String applicationName, String registryIdentity) {
        Provider providerProbe = new Provider();
        providerProbe.setApplication(applicationName);
        providerProbe.setRegistryIdentity(registryIdentity);
        providerProbe.setActive(true);

        Consumer consumerProbe = new Consumer();
        consumerProbe.setApplication(applicationName);
        consumerProbe.setRegistryIdentity(registryIdentity);
        consumerProbe.setActive(true);

        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        if (!providerRepository.exists(Example.of(providerProbe, matcher)) &&
                !consumerRepository.exists(Example.of(consumerProbe, matcher))) {
            Optional<Application> application = applicationRepository.findByNameAndRegistryIdentity(applicationName,
                    registryIdentity);
            if (!application.isPresent()) {
                return;
            }
            application.get().setActiveProvider(false);
            applicationRepository.save(application.get());
        }
    }
}
