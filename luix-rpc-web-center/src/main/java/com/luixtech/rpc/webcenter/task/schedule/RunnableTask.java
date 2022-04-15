package com.luixtech.rpc.webcenter.task.schedule;

import com.luixtech.rpc.webcenter.domain.RpcScheduledTask;
import com.luixtech.rpc.webcenter.domain.RpcScheduledTaskHistory;
import com.luixtech.rpc.webcenter.domain.RpcScheduledTaskLock;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskHistoryRepository;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskLockRepository;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StopWatch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.luixtech.rpc.core.constant.ServiceConstants.*;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static com.luixtech.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE;

@Slf4j
@Builder
public class RunnableTask implements Runnable {

    private static final int SECOND = 1_000;
    private static final int MINUTE = 60_000;

    private final RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;
    private final RpcScheduledTaskLockRepository    rpcScheduledTaskLockRepository;
    private final RpcScheduledTask                  rpcScheduledTask;

    private final transient RpcRegistryService rpcRegistryService;
    private final transient Proxy              proxyFactory;
    private final           String             name;
    private final           String             registryIdentity;
    private final           String             interfaceName;
    private final           String             form;
    private final           String             version;
    private final           Integer            requestTimeout;
    private final           Integer            retryCount;
    private final           String             faultTolerance;

    @Override
    public void run() {
        if (rpcScheduledTask.getStartTime() == null && rpcScheduledTask.getInitialDelay() != null) {
            Instant delayInstant = rpcScheduledTask.getCreatedTime().plusMillis(calculateMilliSeconds(rpcScheduledTask));
            rpcScheduledTask.setStartTime(delayInstant);
        }
        Instant now = Instant.now();
        if (rpcScheduledTask.getStartTime() != null && now.isBefore(rpcScheduledTask.getStartTime())) {
            log.debug("It's not time to start yet for scheduled task: [{}]", rpcScheduledTask.getName());
            return;
        }
        if (rpcScheduledTask.getStopTime() != null && now.isAfter(rpcScheduledTask.getStopTime())) {
            log.debug("It's past the stop time for scheduled task: [{}]", rpcScheduledTask.getName());
            return;
        }
        // Single host execution mode
//        if (rpcScheduledTaskLockRepository.findByName(rpcScheduledTask.getName()).isPresent()) {
//            log.warn("Skip to execute scheduled task for the address: {}", NetworkUtils.INTRANET_IP);
//            return;
//        }
        // This distributed lock used to control that only one node executes the task at the same time
        RpcScheduledTaskLock scheduledTaskLock = new RpcScheduledTaskLock();
        scheduledTaskLock.setName(rpcScheduledTask.getName());
        // Set expiry time with 10 seconds for the lock
        scheduledTaskLock.setExpiryTime(Instant.now().plus(10, ChronoUnit.SECONDS));
        rpcScheduledTaskLockRepository.save(scheduledTaskLock);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        RpcScheduledTaskHistory scheduledTaskHistory = new RpcScheduledTaskHistory();
        BeanUtils.copyProperties(rpcScheduledTask, scheduledTaskHistory);
        scheduledTaskHistory.setId(null);

        // Automatically delete records after 7 days
        scheduledTaskHistory.setExpiryTime(Instant.now().plus(7, ChronoUnit.DAYS));

        try {
            // Execute task
            executeTask();
            scheduledTaskHistory.setSuccess(true);
        } catch (Exception ex) {
            scheduledTaskHistory.setSuccess(false);
            scheduledTaskHistory.setReason(ex.getMessage());
            log.error(String.format("Failed to execute scheduled task %s.%s(%s)", interfaceName,
                    rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson()), ex);
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            if (elapsed < SECOND) {
                log.info("Executed scheduled task {}.{}({}) in {}ms",
                        interfaceName, rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson(), elapsed);
            } else if (elapsed < MINUTE) {
                log.info("Executed scheduled task {}.{}({}) in {}s",
                        interfaceName, rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson(), elapsed / 1000);
            } else {
                log.info("Executed scheduled task {}.{}({}) in {}m",
                        interfaceName, rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson(), elapsed / (1000 * 60));
            }

            scheduledTaskHistory.setElapsed(elapsed);
            // Save task history
            rpcScheduledTaskHistoryRepository.save(scheduledTaskHistory);
        }
    }

    private void executeTask() {
        ConsumerStub<?> consumerStub = getConsumerStub(rpcRegistryService, registryIdentity,
                interfaceName, form, version, requestTimeout, retryCount, faultTolerance);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        invocationHandler.invoke(rpcScheduledTask.getMethodName(), rpcScheduledTask.getMethodParamTypes(), null);
    }

    private static ConsumerStub<?> getConsumerStub(RpcRegistryService rpcRegistryService,
                                                   String registryIdentity, String interfaceName,
                                                   String form, String version,
                                                   Integer requestTimeout, Integer retryCount, String faultTolerance) {
        // Select one of node to execute
        Map<String, String> attributes = new HashMap<>(3);
        attributes.put(FORM, form);
        attributes.put(VERSION, version);
        if (requestTimeout != null) {
            attributes.put(REQUEST_TIMEOUT, requestTimeout.toString());
        }
        if (retryCount != null) {
            attributes.put(RETRY_COUNT, retryCount.toString());
        }
        if (StringUtils.isNotEmpty(faultTolerance)) {
            attributes.put(FAULT_TOLERANCE, faultTolerance);
        }
        return rpcRegistryService.getConsumerStub(registryIdentity, null, interfaceName, attributes);
    }

    private long calculateMilliSeconds(RpcScheduledTask scheduledTask) {
        long oneSecond = 1_000;
        if (RpcScheduledTask.UNIT_SECONDS.equals(scheduledTask.getInitialDelayUnit())) {
            return oneSecond * scheduledTask.getInitialDelay();
        } else if (RpcScheduledTask.UNIT_MINUTES.equals(scheduledTask.getInitialDelayUnit())) {
            return oneSecond * 60 * scheduledTask.getInitialDelay();
        } else if (RpcScheduledTask.UNIT_HOURS.equals(scheduledTask.getInitialDelayUnit())) {
            return oneSecond * 60 * 60 * scheduledTask.getInitialDelay();
        }
        throw new IllegalStateException("Illegal initial delay time unit!");
    }
}