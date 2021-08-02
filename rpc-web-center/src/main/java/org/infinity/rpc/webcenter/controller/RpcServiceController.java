package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcServiceController {

    @Resource
    private RpcServiceService     rpcServiceService;
    @Resource
    private RpcServiceRepository  rpcServiceRepository;
    @Resource
    private RpcProviderService    rpcProviderService;
    @Resource
    private RpcConsumerService    rpcConsumerService;
    @Resource
    private RpcProviderController rpcProviderController;

    @ApiOperation("find service by ID")
    @GetMapping("/api/rpc-service/{id}")
    public ResponseEntity<RpcService> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        if (rpcProviderService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
            domain.setProviding(true);
        }
        if (rpcConsumerService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
        }
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find service")
    @GetMapping("/api/rpc-service")
    public ResponseEntity<RpcService> find(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        String id = DigestUtils.md5DigestAsHex((interfaceName + "@" + registryIdentity).getBytes());
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new NoDataFoundException(interfaceName));
        if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setProviding(true);
        }
        if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
        }
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find service list")
    @GetMapping("/api/rpc-service/services")
    public ResponseEntity<List<RpcService>> findRpcServices(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        Page<RpcService> results = rpcServiceService.find(pageable, registryIdentity, interfaceName);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setProviding(true);
                }
                if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setConsuming(true);
                }
                if (rpcProviderService.existsApplicationService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setActive(true);
                } else {
                    domain.setActive(false);
                }
            });
        }
        return ResponseEntity.ok().headers(generatePageHeaders(results)).body(results.getContent());
    }

    @ApiOperation("activate service")
    @GetMapping("/api/rpc-service/activate")
    public ResponseEntity<Void> activate(
            @ApiParam(value = "registry url identity", defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        rpcProviderService.find(registryIdentity, interfaceName, false)
                .forEach(provider -> rpcProviderController.activate(registryIdentity, provider.getUrl()));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("deactivate service")
    @GetMapping("/api/rpc-service/deactivate")
    public ResponseEntity<Void> deactivate(
            @ApiParam(value = "registry url identity", defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        rpcProviderService.find(registryIdentity, interfaceName, true)
                .forEach(provider -> rpcProviderController.deactivate(registryIdentity, provider.getUrl()));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
