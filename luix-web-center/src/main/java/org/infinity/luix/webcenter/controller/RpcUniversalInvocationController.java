package org.infinity.luix.webcenter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.dto.MethodInvocation;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.infinity.luix.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.luix.core.client.proxy.Proxy;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.spring.boot.config.LuixProperties;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * REST controller for RPC universal calling.
 */
@RestController
@Slf4j
public class RpcUniversalInvocationController {

    @Resource
    private LuixProperties     luixProperties;
    @Resource
    private RpcRegistryService rpcRegistryService;

    @ApiOperation("direct address invocation")
    @PostMapping("/api/rpc-invocations/invoke")
    public String invoke(@ApiParam(value = "methodInvocation", required = true)
                         @Valid @RequestBody MethodInvocation methodInvocation) throws JsonProcessingException {
        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(methodInvocation.getRegistryIdentity(),
                methodInvocation.getProviderUrl(), methodInvocation.getInterfaceName(), methodInvocation.getAttributes());
        Proxy proxyFactory = Proxy.getInstance(luixProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return new ObjectMapper().writeValueAsString(result);
    }

    @ApiOperation("discover address invocation by file")
    @PostMapping("/api/rpc-invocations/invoke-by-file")
    public String invokeByFile(@ApiParam(value = "file", required = true) @RequestPart MultipartFile file) throws IOException {
        String input = StreamUtils.copyToString(file.getInputStream(), Charset.defaultCharset());
        MethodInvocation methodInvocation = new ObjectMapper().readValue(input, MethodInvocation.class);
        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(methodInvocation.getRegistryIdentity(),
                methodInvocation.getProviderUrl(), methodInvocation.getInterfaceName(), methodInvocation.getAttributes());
        Proxy proxyFactory = Proxy.getInstance(luixProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return new ObjectMapper().writeValueAsString(result);
    }
}
