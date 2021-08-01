package org.infinity.rpc.webcenter.service.impl;

import org.infinity.rpc.webcenter.domain.AppAuthority;
import org.infinity.rpc.webcenter.repository.AppAuthorityRepository;
import org.infinity.rpc.webcenter.service.AppAuthorityService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AppAuthorityServiceImpl implements AppAuthorityService {

    @Resource
    private AppAuthorityRepository appAuthorityRepository;

    @Override
    public Page<AppAuthority> find(Pageable pageable, String appName, String authorityName) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<AppAuthority> queryExample = Example.of(new AppAuthority(appName, authorityName), matcher);
        return appAuthorityRepository.findAll(queryExample, pageable);
    }
}