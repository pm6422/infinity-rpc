package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.server.listener.ConsumerProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcConsumerRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcConsumerProcessImpl implements ConsumerProcessable {

    @Resource
    private RpcConsumerRepository    rpcConsumerRepository;
    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcApplicationService    rpcApplicationService;

    @Override
    public void process(Url registryUrl, String interfaceName, List<Url> consumerUrls) {
        if (CollectionUtils.isNotEmpty(consumerUrls)) {
            log.info("Discovered active consumers {}", consumerUrls);
            for (Url consumerUrl : consumerUrls) {
                RpcConsumer provider = RpcConsumer.of(consumerUrl, registryUrl);
                // Insert or update consumer
                rpcConsumerRepository.save(provider);

                // Insert application
                RpcApplication probe = new RpcApplication();
                probe.setName(provider.getApplication());
                probe.setRegistryIdentity(provider.getRegistryIdentity());
                // Ignore query parameter if it has a null value
                ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
                if (rpcApplicationRepository.exists(Example.of(probe, matcher))) {
                    // If application exists
                    continue;
                }

                RpcApplication rpcApplication = rpcApplicationService.remoteQueryApplication(registryUrl, consumerUrl);
                rpcApplicationRepository.save(rpcApplication);
            }
        } else {
            log.info("Discovered offline consumers of [{}]", interfaceName);

            // Update consumers to inactive
            List<RpcConsumer> list = rpcConsumerRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            list.forEach(provider -> provider.setActive(false));
            rpcConsumerRepository.saveAll(list);

            // Update application to inactive
            rpcApplicationService.inactivate(list.get(0).getApplication(), list.get(0).getRegistryIdentity());
        }
    }
}
