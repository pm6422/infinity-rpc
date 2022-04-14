package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.luixtech.rpc.webcenter.dto.RpcRegistryDTO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class RpcRegistryController {

    @Resource
    private RpcRegistryService rpcRegistryService;

    @ApiOperation("find all registries")
    @GetMapping("open-api/rpc-registries")
    @Timed
    public ResponseEntity<List<RpcRegistryDTO>> findRegistries() {
        return ResponseEntity.ok(rpcRegistryService.getRegistries());
    }
}