package com.luixtech.luixrpc.demoserver.service.impl;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.domain.DictItem;
import com.luixtech.luixrpc.democommon.service.DictItemService;
import com.luixtech.luixrpc.democommon.service.DictService;
import com.luixtech.luixrpc.demoserver.exception.DuplicationException;
import com.luixtech.luixrpc.demoserver.exception.DataNotFoundException;
import com.luixtech.luixrpc.demoserver.repository.DictItemRepository;
import com.luixtech.luixrpc.demoserver.repository.DictRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RpcProvider
@Slf4j
public class DictItemServiceImpl implements DictItemService {

    @Resource
    private DictRepository     dictRepository;
    @Resource
    private DictItemRepository dictItemRepository;
    @Resource
    private DictService        dictService;

    @Override
    public DictItem insert(DictItem domain) {
        // 判断dictCode是否存在
        dictRepository.findOneByDictCode(domain.getDictCode()).orElseThrow(() -> new DataNotFoundException(domain.getDictCode()));
        // 根据dictItemCode与dictCode检索记录是否存在
        List<DictItem> existingDictItems = dictItemRepository.findByDictCodeAndDictItemCode(domain.getDictCode(),
                domain.getDictItemCode());
        if (CollectionUtils.isNotEmpty(existingDictItems)) {
            throw new DuplicationException(ImmutableMap.of("dictCode", domain.getDictCode(), "dictItemCode", domain.getDictItemCode()));
        }

        Map<String, String> dictCodeDictNameMap = dictService.findDictCodeDictNameMap();
        domain.setDictName(dictCodeDictNameMap.get(domain.getDictCode()));
        dictItemRepository.save(domain);
        return domain;
    }

    @Override
    public void update(DictItem domain) {
        dictItemRepository.findById(domain.getId()).map(dictItem -> {
            Map<String, String> findDictCodeDictNameMap = dictService.findDictCodeDictNameMap();
            dictItem.setDictName(findDictCodeDictNameMap.get(domain.getDictCode()));
            dictItemRepository.save(dictItem);
            log.debug("Updated dict item: {}", domain);
            return dictItem;
        }).orElseThrow(() -> new DataNotFoundException(domain.getId()));
    }

    @Override
    public Page<DictItem> find(Pageable pageable, String dictCode, String dictItemName) {
        DictItem probe = new DictItem();
        probe.setDictCode(dictCode);
        probe.setDictItemName(dictItemName);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        return dictItemRepository.findAll(Example.of(probe, matcher), pageable);
    }
}