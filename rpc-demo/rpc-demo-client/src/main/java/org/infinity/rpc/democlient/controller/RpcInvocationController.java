package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.client.stub.UniversalMethodInvocation;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * REST controller for RPC calling.
 */
@RestController
@Api(tags = "RPC调用")
@Slf4j
public class RpcInvocationController {

    @Resource
    private InfinityProperties infinityProperties;

    @ApiOperation("通用调用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功调用")})
    @PostMapping("/api/rpc/universal-invocation")
    public ResponseEntity<Object> universalInvoke(@ApiParam(value = "调用参数", required = true) @RequestBody UniversalMethodInvocation req) {
        ConsumerStub<?> consumerStub = getConsumerStub(req);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = universalInvocationHandler.invoke(req.getMethodName(), req.getMethodParamTypes(), req.getArgs());
        return ResponseEntity.ok().body(result);
    }

    private ConsumerStub<?> getConsumerStub(UniversalMethodInvocation req) {
        Map<String, Object> attributesMap = new HashMap<>(0);
        if (MapUtils.isNotEmpty(req.getAttributes())) {
            for (Map.Entry<String, String> entry : req.getAttributes().entrySet()) {
                attributesMap.put(entry.getKey(), entry.getValue());
            }
        }
        String beanName = ConsumerStub.buildConsumerStubBeanName(req.getInterfaceName(), attributesMap);
        if (ConsumerStubHolder.getInstance().getStubs().containsKey(beanName)) {
            return ConsumerStubHolder.getInstance().getStubs().get(beanName);
        }

        Integer requestTimeout = null;
        if (req.getAttributes().containsKey(REQUEST_TIMEOUT)) {
            requestTimeout = Integer.parseInt(req.getAttributes().get(REQUEST_TIMEOUT));
        }
        Integer maxRetries = null;
        if (req.getAttributes().containsKey(MAX_RETRIES)) {
            maxRetries = Integer.parseInt(req.getAttributes().get(MAX_RETRIES));
        }
        ConsumerStub<?> consumerStub = ConsumerStub.create(req.getInterfaceName(), infinityProperties.getApplication(),
                infinityProperties.getRegistry(), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, req.getAttributes().get(FORM), req.getAttributes().get(VERSION), requestTimeout, maxRetries);
        ConsumerStubHolder.getInstance().addStub(beanName, consumerStub);
        return consumerStub;
    }
}
