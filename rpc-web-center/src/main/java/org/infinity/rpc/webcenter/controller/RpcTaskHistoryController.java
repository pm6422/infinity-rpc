package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.domain.RpcTaskHistory;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcTaskHistoryRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing task histories.
 */
@RestController
@Slf4j
public class RpcTaskHistoryController {

    @Resource
    private RpcTaskHistoryRepository rpcTaskHistoryRepository;

    @ApiOperation("find task history list")
    @GetMapping("/api/rpc-task-history/histories")
    public ResponseEntity<List<RpcTaskHistory>> find(Pageable pageable,
                                                     @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
                                                     @ApiParam(value = "Task name") @RequestParam(value = "name", required = false) String name,
                                                     @ApiParam(value = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                                     @ApiParam(value = "Form") @RequestParam(value = "form", required = false) String form,
                                                     @ApiParam(value = "Version") @RequestParam(value = "version", required = false) String version,
                                                     @ApiParam(value = "Method signature") @RequestParam(value = "methodSignature", required = false) String methodSignature) {
        RpcTaskHistory probe = new RpcTaskHistory();
        probe.setRegistryIdentity(registryIdentity);
        probe.setName(trimToNull(name));
        probe.setInterfaceName(trimToNull(interfaceName));
        probe.setForm(trimToNull(form));
        probe.setVersion(trimToNull(version));
        probe.setMethodSignature(trimToNull(methodSignature));
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Page<RpcTaskHistory> histories = rpcTaskHistoryRepository.findAll(Example.of(probe, matcher), pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(histories)).body(histories.getContent());
    }

    @ApiOperation("find task history by id")
    @GetMapping("/api/rpc-task-history/{id}")
    public ResponseEntity<RpcTaskHistory> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        RpcTaskHistory history = rpcTaskHistoryRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(history);
    }
}
