package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.demoserver.domain.TaskHistory;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.TaskHistoryRepository;
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

import static org.infinity.rpc.demoserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing task histories.
 */
@RestController
@Slf4j
public class TaskHistoryController {

    @Resource
    private TaskHistoryRepository taskHistoryRepository;

    @ApiOperation("find task history list")
    @GetMapping("/api/task-histories")
    public ResponseEntity<List<TaskHistory>> find(Pageable pageable,
                                                  @ApiParam(value = "Task name") @RequestParam(value = "name", required = false) String name) {
        TaskHistory probe = new TaskHistory();
        probe.setName(name);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Page<TaskHistory> histories = taskHistoryRepository.findAll(Example.of(probe, matcher), pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(histories)).body(histories.getContent());
    }

    @ApiOperation("find task history by id")
    @GetMapping("/api/task-histories/{id}")
    public ResponseEntity<TaskHistory> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        TaskHistory history = taskHistoryRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(history);
    }
}
