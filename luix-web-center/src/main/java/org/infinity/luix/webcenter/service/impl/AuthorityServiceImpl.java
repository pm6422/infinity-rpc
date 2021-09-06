package org.infinity.luix.webcenter.service.impl;

import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.repository.AuthorityRepository;
import org.infinity.luix.webcenter.service.AuthorityService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorityServiceImpl implements AuthorityService {

    @Resource
    private AuthorityRepository authorityRepository;

    @Override
    public List<String> findAllAuthorityNames(Boolean enabled) {
        return authorityRepository.findByEnabled(enabled).stream().map(Authority::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<Authority> find(Boolean enabled) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<Authority> queryExample = Example.of(new Authority(enabled), matcher);
        return authorityRepository.findAll(queryExample);
    }
}