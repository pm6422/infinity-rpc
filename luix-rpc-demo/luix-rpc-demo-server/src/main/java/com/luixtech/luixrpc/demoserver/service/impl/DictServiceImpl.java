package com.luixtech.luixrpc.demoserver.service.impl;

import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.domain.Dict;
import com.luixtech.luixrpc.democommon.service.DictService;
import com.luixtech.luixrpc.demoserver.repository.DictRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Resource;
import java.util.Map;
import java.util.stream.Collectors;

@RpcProvider
public class DictServiceImpl implements DictService {

    @Resource
    private DictRepository dictRepository;

    @Override
    public Page<Dict> find(Pageable pageable, String dictName, Boolean enabled) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<Dict> queryExample = Example.of(new Dict(dictName, enabled), matcher);
        return dictRepository.findAll(queryExample, pageable);
    }

    @Override
    public Map<String, String> findDictCodeDictNameMap() {
        return dictRepository.findAll().stream().collect(Collectors.toMap(Dict::getDictCode, Dict::getDictName));
    }
}