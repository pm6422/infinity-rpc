package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.dto.RegistryDTO;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.infinity.rpc.democlient.service.ProviderService;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.democlient.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Api(tags = "服务发现")
@Slf4j
public class ServiceDiscoveryController {

    private final InfinityProperties infinityProperties;
    private final RegistryService    registryService;
    private final ProviderService    providerService;
    private final ApplicationService applicationService;

    public ServiceDiscoveryController(InfinityProperties infinityProperties,
                                      RegistryService registryService,
                                      ProviderService providerService,
                                      ApplicationService applicationService) {
        this.infinityProperties = infinityProperties;
        this.registryService = registryService;
        this.providerService = providerService;
        this.applicationService = applicationService;
    }

    @ApiOperation("检索所有注册中心列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("api/service-discovery/registries")
    public ResponseEntity<List<RegistryDTO>> findRegistries() {
        return ResponseEntity.ok(registryService.getRegistries());
    }

    @ApiOperation("分页检索应用列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("api/service-discovery/applications")
    public ResponseEntity<List<Application>> findApplications(
            Pageable pageable,
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl") String registryUrl,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Application> list = applicationService.find(pageable, registryUrl, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("分页检索服务提供者列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/service-discovery/providers")
    public ResponseEntity<List<Provider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl") String registryUrl,
            @ApiParam(value = "应用名称") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "接口名称") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Provider> list = providerService.find(pageable, registryUrl, application, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("启用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PutMapping("/api/service-discovery/provider/activate")
    public ResponseEntity<Void> activate(@RequestBody String providerUrl) {
        infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().activate(Url.valueOf(providerUrl)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("禁用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PutMapping("/api/service-discovery/provider/deactivate")
    public ResponseEntity<Void> deactivate(@RequestBody String providerUrl) {
        infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().deactivate(Url.valueOf(providerUrl)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
