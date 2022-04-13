package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.domain.RpcProvider;
import com.luixtech.rpc.webcenter.repository.RpcProviderRepository;
import com.luixtech.rpc.webcenter.service.RpcApplicationService;
import com.luixtech.rpc.webcenter.service.RpcServerService;
import com.luixtech.rpc.webcenter.service.RpcServiceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.luixtech.rpc.core.listener.GlobalProviderDiscoveryListener;
import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.url.Url;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcProviderProcessImpl implements GlobalProviderDiscoveryListener {

    @Resource
    private RpcProviderRepository rpcProviderRepository;
    @Resource
    private RpcServerService      rpcServerService;
    @Resource
    private RpcServiceService     rpcServiceService;
    @Resource
    private RpcApplicationService rpcApplicationService;

    @Override
    public void onNotify(Url registryUrl, String interfaceName, List<Url> providerUrls) {
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            for (Url providerUrl : providerUrls) {
                RpcProvider rpcProvider = RpcProvider.of(providerUrl, registryUrl);
                if (BuildInService.class.getName().equals(rpcProvider.getInterfaceName())) {
                    continue;
                }
                log.info("Discovered active providers: {}", providerUrl);
                // Insert or update provider
                rpcProviderRepository.save(rpcProvider);

                // Insert server
                rpcServerService.insert(registryUrl, providerUrl, rpcProvider.getAddress());

                // Insert service
                rpcServiceService.insert(registryUrl, rpcProvider.getInterfaceName());

                // Insert application
                rpcApplicationService.insert(registryUrl, providerUrl, rpcProvider.getApplication());
            }
        } else {
            // Update providers to inactive
            List<RpcProvider> list = rpcProviderRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            log.info("Discovered inactive providers of [{}]", interfaceName);

            list.forEach(provider -> provider.setActive(false));
            rpcProviderRepository.saveAll(list);
        }
    }
}
