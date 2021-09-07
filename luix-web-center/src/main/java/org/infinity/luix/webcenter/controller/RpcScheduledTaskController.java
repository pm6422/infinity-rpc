package org.infinity.luix.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.config.ApplicationConstants;
import org.infinity.luix.webcenter.domain.RpcScheduledTask;
import org.infinity.luix.webcenter.exception.NoDataFoundException;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskRepository;
import org.infinity.luix.webcenter.service.RpcScheduledTaskService;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.config.CronTask;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.infinity.luix.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILFAST;
import static org.infinity.luix.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILOVER;


/**
 * REST controller for managing RPC scheduled tasks.
 */
@RestController
@Slf4j
public class RpcScheduledTaskController {

    @Resource
    private RpcScheduledTaskRepository rpcScheduledTaskRepository;
    @Resource
    private RpcScheduledTaskService    rpcScheduledTaskService;
    @Resource
    private HttpHeaderCreator          httpHeaderCreator;

    @ApiOperation("create scheduled task")
    @PostMapping("/api/rpc-scheduled-tasks")
    @Timed
    public ResponseEntity<Void> create(@ApiParam(value = "task", required = true) @Valid @RequestBody RpcScheduledTask domain) {
        log.debug("REST request to create scheduled task: {}", domain);
        if (domain.getStartTime() != null && domain.getStopTime() != null) {
            Validate.isTrue(domain.getStopTime().isAfter(domain.getStartTime()),
                    "The stop time must be greater than the start time");
        }
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        if (StringUtils.isNotEmpty(domain.getCronExpression())) {
            try {
                new CronTask(null, domain.getCronExpression());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal CRON expression: " + ex.getMessage());
            }
        }

        rpcScheduledTaskService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @ApiOperation("find scheduled task list")
    @GetMapping("/api/rpc-scheduled-tasks")
    @Timed
    public ResponseEntity<List<RpcScheduledTask>> find(Pageable pageable,
                                                       @ApiParam(value = "registry url identity", required = true, defaultValue = ApplicationConstants.DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
                                                       @ApiParam(value = "Task name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
                                                       @ApiParam(value = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                                       @ApiParam(value = "Form") @RequestParam(value = "form", required = false) String form,
                                                       @ApiParam(value = "Version") @RequestParam(value = "version", required = false) String version,
                                                       @ApiParam(value = "Method name") @RequestParam(value = "methodName", required = false) String methodName,
                                                       @ApiParam(value = "Method signature") @RequestParam(value = "methodSignature", required = false) String methodSignature) {
        Page<RpcScheduledTask> tasks = rpcScheduledTaskService.find(pageable, registryIdentity, name, interfaceName, form, version, methodName, methodSignature);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @ApiOperation("find scheduled task by id")
    @GetMapping("/api/rpc-scheduled-tasks/{id}")
    @Timed
    public ResponseEntity<RpcScheduledTask> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        RpcScheduledTask task = rpcScheduledTaskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(task);
    }

    @ApiOperation("update scheduled task")
    @PutMapping("/api/rpc-scheduled-tasks")
    @Timed
    public ResponseEntity<Void> update(@ApiParam(value = "new task", required = true) @Valid @RequestBody RpcScheduledTask domain) {
        log.debug("REST request to update scheduled task: {}", domain);
        if (domain.getStartTime() != null && domain.getStopTime() != null) {
            Validate.isTrue(domain.getStopTime().isAfter(domain.getStartTime()),
                    "The stop time must be greater than the start time");
        }
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        if (StringUtils.isNotEmpty(domain.getCronExpression())) {
            try {
                new CronTask(null, domain.getCronExpression());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal CRON expression: " + ex.getMessage());
            }
        }
        rpcScheduledTaskService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @ApiOperation(value = "delete scheduled task by id", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/rpc-scheduled-tasks/{id}")
    @Timed
    public ResponseEntity<Void> delete(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete scheduled task: {}", id);
        rpcScheduledTaskService.delete(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }

    @ApiOperation("Get available time units of fixed rate interval")
    @GetMapping("/api/rpc-scheduled-tasks/time-units")
    public ResponseEntity<List<String>> getTimeUnits() {
        return ResponseEntity.ok().body(RpcScheduledTask.AVAILABLE_FIXED_INTERVAL_UNIT);
    }

    @ApiOperation("Get available fault tolerances")
    @GetMapping("/api/rpc-scheduled-tasks/fault-tolerances")
    public ResponseEntity<List<String>> getFaultTolerances() {
        return ResponseEntity.ok().body(Arrays.asList(FAULT_TOLERANCE_VAL_FAILFAST, FAULT_TOLERANCE_VAL_FAILOVER));
    }
}
