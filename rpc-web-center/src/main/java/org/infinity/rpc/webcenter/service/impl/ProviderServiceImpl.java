package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.domain.Provider;
import org.infinity.rpc.webcenter.repository.ProviderRepository;
import org.infinity.rpc.webcenter.service.ProviderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

import static org.infinity.rpc.webcenter.domain.Provider.*;


@Service
public class ProviderServiceImpl implements ProviderService {
    @Resource
    private MongoTemplate      mongoTemplate;
    @Resource
    private ProviderRepository providerRepository;

    @Override
    public Page<Provider> find(Pageable pageable, String registryUrl, String application, String interfaceName, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryUrl));
        if (StringUtils.isNotEmpty(application)) {
            query.addCriteria(Criteria.where(FIELD_APPLICATION).is(application));
        }
        if (StringUtils.isNotEmpty(interfaceName)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + interfaceName + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).regex(pattern));
        }
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }
        long totalCount = mongoTemplate.count(query, Provider.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, Provider.class), pageable, totalCount);
    }

    @Override
    public List<String> findDistinctApplications(String registryUrl, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryUrl));
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }
        return mongoTemplate.findDistinct(query, FIELD_APPLICATION, Provider.class, String.class);
    }
}