package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.entity.ProviderInfo;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@RestController
@Api(tags = "服务发现")
@Slf4j
public class ServiceDiscoveryController {
    @Resource
    private       InfinityProperties    infinityProperties;
    private final List<RegistryService> registryServices;

    public ServiceDiscoveryController(List<RegistryService> registryServices) {
        this.registryServices = registryServices;
    }

    @ApiOperation("获取所有应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/apps")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<ApplicationExtConfig>> findApps() {
        List<ApplicationExtConfig> applications = registryServices.get(0).getAllApplications();
        return ResponseEntity.ok(applications);
    }

    @ApiOperation("获取所有服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/providers")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<ProviderInfo>> findProviders() {
        List<ProviderInfo> providers = new ArrayList<>();
        Map<String, Map<String, List<AddressInfo>>> nodeMap = registryServices.get(0).getAllNodes("provider");
        if (MapUtils.isNotEmpty(nodeMap)) {
            for (Map.Entry<String, Map<String, List<AddressInfo>>> entry : nodeMap.entrySet()) {
                List<AddressInfo> activeProviders = entry.getValue().get("active");
                List<AddressInfo> inactiveProviders = entry.getValue().get("inactive");
                providers.add(ProviderInfo.of(entry.getKey(), activeProviders, inactiveProviders));
            }
        }
        return ResponseEntity.ok().body(providers);
    }

    /**
     * Get provider node
     *
     * @param statusNode statusNode
     * @return address info list
     */
    @GetMapping("api/service-discovery/{providerName}/{statusNode}/nodes")
    public ResponseEntity<List<AddressInfo>> getProviderNode(@PathVariable(value = "providerName") String providerName,
                                                             @PathVariable(value = "statusNode") String statusNode) {
        List<AddressInfo> nodes = registryServices.get(0).getNodes("provider", providerName, statusNode);
        return ResponseEntity.ok().body(nodes);
    }

    @PostMapping("/api/service-discovery/deactivate")
    public ResponseEntity<Void> deactivate(@RequestBody String url) {
        infinityProperties.getRegistryList().forEach(registryConfig -> {
            registryConfig.getRegistryImpl().deactivate(Url.valueOf(url));
        });
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/api/service-discovery/activate")
    public ResponseEntity<Void> activate(@RequestBody String url) {
        infinityProperties.getRegistryList().forEach(registryConfig -> {
            registryConfig.getRegistryImpl().activate(Url.valueOf(url));
        });
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Get all the groups
     *
     * @return groups
     */
//    @GetMapping("api/service-discovery/groups")
//    public ResponseEntity<List<String>> getAllGroups() {
//        List<String> result = registryService.getGroups();
//        return ResponseEntity.ok().body(result);
//    }

//    /**
//     * Get providers
//     *
//     * @param group group
//     * @return providers
//     */
//    @GetMapping("api/service-discovery/{group}/providers")
//    public ResponseEntity<List<String>> getProvidersByGroup(@PathVariable(value = "group", required = true) String group) {
//        List<String> providers = registryService.getProvidersByGroup(group);
//        return ResponseEntity.ok().body(providers);
//    }
}
