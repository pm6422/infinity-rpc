package org.infinity.rpc.webcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.RpcTask;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcTaskRepository;
import org.infinity.rpc.webcenter.service.RpcTaskService;
import org.infinity.rpc.webcenter.task.Taskable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;


/**
 * REST controller for managing tasks.
 */
@RestController
@Slf4j
public class RpcTaskController {

    @Resource
    private RpcTaskRepository  taskRepository;
    @Resource
    private RpcTaskService     taskService;
    @Resource
    private HttpHeaderCreator  httpHeaderCreator;
    @Resource
    private ApplicationContext applicationContext;

    @ApiOperation("create task")
    @PostMapping("/api/tasks")
    public ResponseEntity<Void> create(@ApiParam(value = "task", required = true) @Valid @RequestBody RpcTask domain) {
        log.debug("REST request to create task: {}", domain);
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        taskService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @ApiOperation("find task list")
    @GetMapping("/api/tasks")
    public ResponseEntity<List<RpcTask>> find(Pageable pageable,
                                              @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
                                              @ApiParam(value = "Task name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
                                              @ApiParam(value = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                              @ApiParam(value = "Form") @RequestParam(value = "form", required = false) String form,
                                              @ApiParam(value = "Version") @RequestParam(value = "version", required = false) String version,
                                              @ApiParam(value = "Method name") @RequestParam(value = "methodName", required = false) String methodName) {
        Page<RpcTask> tasks = taskService.find(pageable, registryIdentity, name, interfaceName, form, version, methodName);
        return ResponseEntity.ok().headers(generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @ApiOperation("find task by id")
    @GetMapping("/api/tasks/{id}")
    public ResponseEntity<RpcTask> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        RpcTask task = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(task);
    }

    @ApiOperation("update task")
    @PutMapping("/api/tasks")
    public ResponseEntity<Void> update(@ApiParam(value = "new task", required = true) @Valid @RequestBody RpcTask domain) {
        log.debug("REST request to update task: {}", domain);
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        taskService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @ApiOperation(value = "delete task by id", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/tasks/{id}")
    public ResponseEntity<Void> delete(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete task: {}", id);
        taskService.delete(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }

    @ApiOperation("find task bean names")
    @GetMapping("/api/tasks/beans")
    public ResponseEntity<List<String>> findBeans() {
        return ResponseEntity.ok().body(Arrays.asList(applicationContext.getBeanNamesForType(Taskable.class)));
    }
}
