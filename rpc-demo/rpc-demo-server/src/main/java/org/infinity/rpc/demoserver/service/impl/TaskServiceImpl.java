package org.infinity.rpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.demoserver.domain.Task;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.TaskHistoryRepository;
import org.infinity.rpc.demoserver.repository.TaskLockRepository;
import org.infinity.rpc.demoserver.repository.TaskRepository;
import org.infinity.rpc.demoserver.service.TaskService;
import org.infinity.rpc.demoserver.task.CronTaskRegistrar;
import org.infinity.rpc.demoserver.task.RunnableTask;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService, ApplicationRunner {
    @Resource
    private TaskRepository        taskRepository;
    @Resource
    private TaskHistoryRepository taskHistoryRepository;
    @Resource
    private TaskLockRepository    taskLockRepository;
    @Resource
    private CronTaskRegistrar     cronTaskRegistrar;

    @Override
    public void refresh() throws Exception {
        cronTaskRegistrar.destroy();
        run(null);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Timed task with normal state in initial load database
        List<Task> enabledTasks = taskRepository.findByEnabledIsTrue();
        if (CollectionUtils.isEmpty(enabledTasks)) {
            log.info("No tasks to execute!");
            return;
        }

        for (Task task : enabledTasks) {
            addCronTask(task);
        }
        log.info("Loaded all tasks");
    }

    @Override
    public Task insert(Task domain) {
        domain.setName("T" + IdGenerator.generateShortId());
        Task savedOne = taskRepository.insert(domain);
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            addCronTask(savedOne);
        }
        return savedOne;
    }

    @Override
    public void update(Task domain) {
        Task existingOne = taskRepository.findById(domain.getId()).orElseThrow(() -> new NoDataFoundException(domain.getId()));
        Task savedOne = taskRepository.save(domain);

        // Remove before adding
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            removeCronTask(existingOne);
        }

        // Add a new one
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            addCronTask(savedOne);
        }
    }

    @Override
    public void delete(String id) {
        Task existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        taskRepository.deleteById(id);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            removeCronTask(existingOne);
        }
    }

    @Override
    public void startOrStop(String id) {
        Task existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            addCronTask(existingOne);
        } else {
            removeCronTask(existingOne);
        }
    }

    @Override
    public Page<Task> find(Pageable pageable, String name, String beanName, String methodName) {
        Task probe = new Task();
        probe.setName(name);
        probe.setBeanName(beanName);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        return taskRepository.findAll(Example.of(probe, matcher), pageable);
    }

    private void addCronTask(Task task) {
        RunnableTask runnableTask = RunnableTask.builder()
                .taskHistoryRepository(taskHistoryRepository)
                .taskLockRepository(taskLockRepository)
                .name(task.getName())
                .beanName(task.getBeanName())
                .argumentsJson(task.getArgumentsJson())
                .cronExpression(task.getCronExpression())
                .allHostsRun(task.isAllHostsRun())
                .build();
        cronTaskRegistrar.addCronTask(runnableTask, task.getCronExpression());
    }

    private void removeCronTask(Task task) {
        RunnableTask runnableTask = RunnableTask.builder()
                .taskHistoryRepository(taskHistoryRepository)
                .taskLockRepository(taskLockRepository)
                .name(task.getName())
                .beanName(task.getBeanName())
                .argumentsJson(task.getArgumentsJson())
                .cronExpression(task.getCronExpression())
                .allHostsRun(task.isAllHostsRun())
                .build();
        cronTaskRegistrar.removeCronTask(runnableTask);
    }
}
