package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.stub.MethodMeta;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.dto.OptionMetaDTO;
import org.infinity.rpc.webcenter.dto.OptionsDTO;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.server.buildin.BuildInService.*;
import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcProviderController {

    @Resource
    private InfinityProperties    infinityProperties;
    @Resource
    private RpcRegistryService    rpcRegistryService;
    @Resource
    private RpcProviderService    rpcProviderService;
    @Resource
    private RpcProviderRepository rpcProviderRepository;
    @Resource
    private HttpHeaderCreator     httpHeaderCreator;

    @ApiOperation("find provider by ID")
    @GetMapping("/api/rpc-provider/{id}")
    public ResponseEntity<RpcProvider> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcProvider domain = rpcProviderRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find provider list")
    @GetMapping("/api/rpc-provider/providers")
    public ResponseEntity<List<RpcProvider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcProvider> list = rpcProviderService.find(pageable, registryIdentity, application, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("find all methods of provider")
    @GetMapping("/api/rpc-provider/methods")
    public ResponseEntity<List<MethodMeta>> findMethods(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        // Use specified provider url
        UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
        @SuppressWarnings({"unchecked"})
        List<MethodMeta> result = (List<MethodMeta>) invocationHandler.invoke(METHOD_GET_METHODS,
                new String[]{String.class.getName(), String.class.getName(), String.class.getName()},
                new Object[]{providerUrl.getPath(), providerUrl.getForm(), providerUrl.getVersion()});
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation(value = "check health of provider", notes = "There is no service discovery in the direct connection mode, even the inactive provider can be called successfully")
    @GetMapping("/api/rpc-provider/health")
    public ResponseEntity<String> health(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        // Use specified provider url
        UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
        String result;
        try {
            result = (String) invocationHandler.invoke(METHOD_CHECK_HEALTH,
                    new String[]{String.class.getName(), String.class.getName(), String.class.getName()},
                    new Object[]{providerUrl.getPath(), providerUrl.getForm(), providerUrl.getVersion()});
        } catch (Exception ex) {
            result = ex.getMessage();
        }
        return ResponseEntity.ok().body(result);
    }

    private UniversalInvocationHandler createBuildInInvocationHandler(String registryIdentity, Url providerUrl) {
        ConsumerStub<?> consumerStub = ConsumerStub.create(BuildInService.class.getName(),
                infinityProperties.getApplication(), rpcRegistryService.findRegistryConfig(registryIdentity),
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, providerUrl.getAddress());
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        return proxyFactory.createUniversalInvocationHandler(consumerStub);
    }

    @ApiOperation("activate provider")
    @GetMapping("/api/rpc-provider/activate")
    public void activate(
            @ApiParam(value = "registry url identity", defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        control(registryIdentity, providerUrl, METHOD_ACTIVATE);
    }

    @ApiOperation("deactivate provider")
    @GetMapping("/api/rpc-provider/deactivate")
    public void deactivate(
            @ApiParam(value = "registry url identity", defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        control(registryIdentity, providerUrl, METHOD_DEACTIVATE);
    }

    private void control(String registryIdentity, Url providerUrl, String methodName) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(registry -> {
                String identity = registry.getRegistryImpl().getRegistryUrl().getIdentity();
                // Use specified provider url
                UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(identity, providerUrl);
                invocationHandler.invoke(methodName,
                        new String[]{String.class.getName(), String.class.getName(), String.class.getName()},
                        new Object[]{providerUrl.getPath(), providerUrl.getForm(), providerUrl.getVersion()});
            });
        } else {
            // Use specified provider url
            UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
            invocationHandler.invoke(methodName,
                    new String[]{String.class.getName(), String.class.getName(), String.class.getName()},
                    new Object[]{providerUrl.getPath(), providerUrl.getForm(), providerUrl.getVersion()});
        }
    }

    @ApiOperation("get provider options")
    @GetMapping("/api/rpc-provider/options")
    public List<OptionMetaDTO> options(
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        Map<String, String> options = providerUrl.getOptions();

        List<OptionMetaDTO> all = ProviderStub.OPTIONS.stream().map(OptionMetaDTO::of).collect(Collectors.toList());
        all.forEach(dto -> {
            if (options.containsKey(dto.getName())) {
                dto.setValue(options.get(dto.getName()));
            }
        });
        return all;
    }

    @ApiOperation("Save options")
    @PutMapping("/api/rpc-provider/options")
    public ResponseEntity<Void> saveOptions(@ApiParam(value = "optionsDTO", required = true)
                                            @Valid @RequestBody OptionsDTO optionsDTO) {
        Url providerUrl = Url.valueOf(optionsDTO.getUrl());
        for (OptionMetaDTO next : optionsDTO.getOptions()) {
            if (next.getDefaultValue().equals(next.getValue())) {
                providerUrl.getOptions().remove(next.getName());
            } else {
                providerUrl.addOption(next.getName(), next.getValue());
            }
        }

        //todo
        reregister(optionsDTO.getRegistryIdentity(), providerUrl);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    private void reregister(String registryIdentity, Url providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(registry -> {
                String identity = registry.getRegistryImpl().getRegistryUrl().getIdentity();
                // todo: change to broadcasting
                // Use specified provider url
                UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(identity, providerUrl);
                invocationHandler.invoke(METHOD_REREGISTER,
                        new String[]{String.class.getName()},
                        new Object[]{providerUrl.toFullStr()});
            });
        } else {
            // todo: change to broadcasting
            // Use specified provider url
            UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
            invocationHandler.invoke(METHOD_REREGISTER,
                    new String[]{String.class.getName()},
                    new Object[]{providerUrl.toFullStr()});
        }
    }
}
