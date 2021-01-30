package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democommon.service.AppService;
import org.infinity.rpc.spring.boot.bean.name.ProviderStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

@RestController
@Api(tags = "测试")
@Slf4j
public class TestController {
    private final InfinityProperties infinityProperties;
    private final ApplicationContext applicationContext;
    private final Environment        env;

    public TestController(InfinityProperties infinityProperties,
                          ApplicationContext applicationContext,
                          Environment env) {
        this.infinityProperties = infinityProperties;
        this.applicationContext = applicationContext;
        this.env = env;
    }


    @ApiOperation("测试注册provider")
    @GetMapping("/open-api/test/register-provider")
    public void registerProvider() {
        Url providerUrl = Url.of(
                infinityProperties.getAvailableProtocol().getName(),
                "192.168.0.1",
                infinityProperties.getAvailableProtocol().getPort(),
                AppService.class.getName());

        // Assign values to parameters
        providerUrl.addOption(CHECK_HEALTH, String.valueOf(CHECK_HEALTH_DEFAULT_VALUE));
        providerUrl.addOption(Url.PARAM_APP, infinityProperties.getApplication().getName());

        infinityProperties.getRegistryList().forEach(registryConfig -> {
            registryConfig.getRegistryImpl().register(providerUrl);
        });
    }

    @ApiOperation("测试获取AppService provider stub")
    @GetMapping("/open-api/test/app-service-provider-stub")
    public Object testGetAppServiceProviderStub() {
        String name = ProviderStubBeanNameBuilder
                .builder(AppService.class.getName(), env)
                .group(GROUP_DEFAULT_VALUE)
                .version(VERSION_DEFAULT_VALUE)
                .build();
        return applicationContext.getBean(name);
    }

    @ApiOperation("测试获取AppService provider")
    @GetMapping("/open-api/test/app-service-provider")
    public void testGetAppServiceProvider() {
        Object bean = applicationContext.getBean(AppService.class);
        log.info(bean.toString());
    }
}
