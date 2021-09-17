package org.infinity.luix.demoserver.task.polling;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.demoserver.task.polling.queue.AsyncTask;
import org.infinity.luix.demoserver.task.polling.queue.DistributedMessageQueue;
import org.infinity.luix.demoserver.task.polling.queue.InMemoryAsyncTaskQueue;
import org.infinity.luix.demoserver.task.polling.queue.Message;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Indefinite iteration polling from {@link InMemoryAsyncTaskQueue}
 * used to set result to {@link org.springframework.web.context.request.async.DeferredResult} if there are completed async task
 * <p>
 * Refer to
 * https://blog.csdn.net/m0_37595562/article/details/81013909
 * https://filia-aleks.medium.com/microservice-performance-battle-spring-mvc-vs-webflux-80d39fd81bf0
 */
@Slf4j
@Component
public class ConsumeMessageTask implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this::execute).start();
    }

    private void execute() {
        while (true) {
            try {
                AsyncTask asyncTask = InMemoryAsyncTaskQueue.poll();
                if (asyncTask == null) {
                    continue;
                }

                // Get message from distributed queue
                Message message = DistributedMessageQueue.get(asyncTask.getId());
                if (message != null) {
                    // Set value to DeferredResult to complete the HTTP response if the message was found in Redis
                    ResponseEntity<String> response = ResponseEntity.ok(message.getData());
                    asyncTask.getDeferredResult().setResult(response);
                } else {
                    // Re-put in memory task queue if the message can NOT be found in Redis
                    InMemoryAsyncTaskQueue.offer(asyncTask);
                }
            } catch (Exception e) {
                log.error("Failed to consume async task queue!", e);
            }
        }
    }
}
