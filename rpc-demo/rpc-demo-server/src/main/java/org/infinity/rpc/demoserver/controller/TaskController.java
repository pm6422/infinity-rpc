package org.infinity.rpc.demoserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.demoserver.component.HttpHeaderCreator;
import org.infinity.rpc.demoserver.domain.Task;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.TaskRepository;
import org.infinity.rpc.demoserver.service.TaskService;
import org.infinity.rpc.demoserver.task.TaskExecutable;
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

import static org.infinity.rpc.demoserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing tasks.
 */
@RestController
@Slf4j
public class TaskController {

    @Resource
    private TaskRepository     taskRepository;
    @Resource
    private TaskService        taskService;
    @Resource
    private HttpHeaderCreator  httpHeaderCreator;
    @Resource
    private ApplicationContext applicationContext;

    @ApiOperation("create task")
    @PostMapping("/api/tasks")
    public ResponseEntity<Void> create(@ApiParam(value = "task", required = true) @Valid @RequestBody Task domain) {
        log.debug("REST request to create task: {}", domain);
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
        taskService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @ApiOperation("find task list")
    @GetMapping("/api/tasks")
    public ResponseEntity<List<Task>> find(Pageable pageable,
                                           @ApiParam(value = "Task name") @RequestParam(value = "name", required = false) String name,
                                           @ApiParam(value = "Bean name") @RequestParam(value = "beanName", required = false) String beanName) {
        Page<Task> tasks = taskService.find(pageable, name, beanName);
        return ResponseEntity.ok().headers(generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @ApiOperation("find task by id")
    @GetMapping("/api/tasks/{id}")
    public ResponseEntity<Task> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(task);
    }

    @ApiOperation("update task")
    @PutMapping("/api/tasks")
    public ResponseEntity<Void> update(@ApiParam(value = "new task", required = true) @Valid @RequestBody Task domain) {
        log.debug("REST request to update task: {}", domain);
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
        return ResponseEntity.ok().body(Arrays.asList(applicationContext.getBeanNamesForType(TaskExecutable.class)));
    }

    @ApiOperation("find available time units of fixed rate interval")
    @GetMapping("/api/tasks/time-units")
    public ResponseEntity<List<String>> findTimeUnits() {
        return ResponseEntity.ok().body(Task.AVAILABLE_FIXED_INTERVAL_UNIT);
    }
}
