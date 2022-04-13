package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.webcenter.config.ApplicationConstants;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.webcenter.domain.RpcConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class RpcConsumerController {

    @Resource
    private RpcConsumerService rpcConsumerService;

    @ApiOperation("find consumer list")
    @GetMapping("/api/rpc-consumers")
    @Timed
    public ResponseEntity<List<RpcConsumer>> findConsumers(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = ApplicationConstants.DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "address") @RequestParam(value = "address", required = false) String address,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcConsumer> list = rpcConsumerService.find(pageable, registryIdentity, application, address, interfaceName, active);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("get consumer options")
    @GetMapping("/api/rpc-consumers/options")
    @Timed
    public ResponseEntity<Map<String, String>> options() {
        return ResponseEntity.ok(ConsumerStub.OPTIONS);
    }
}
