package org.infinity.luix.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.demoserver.service.AsyncTaskTestService;
import org.infinity.luix.demoserver.task.polling.queue.DistributedMessageQueue;
import org.infinity.luix.demoserver.task.polling.queue.Message;
import org.infinity.luix.demoserver.utils.TraceIdUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AsyncTaskTestServiceImpl implements AsyncTaskTestService {

    @Override
    public String sendMessage() {
        try {
            TimeUnit.SECONDS.sleep(5);
            return "Task " + TraceIdUtils.getTraceId() + " finished";
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }

    @Override
    @Async
    public void sendMessage(Message message) {
        try {
            log.info("Sending message {}", message);
            // stimulate consuming task running
            TimeUnit.SECONDS.sleep(5);
            DistributedMessageQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
