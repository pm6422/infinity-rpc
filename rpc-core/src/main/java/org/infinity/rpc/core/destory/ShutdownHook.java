package org.infinity.rpc.core.destory;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A utility used to clean up the resources
 */
@Slf4j
@ThreadSafe
public class ShutdownHook extends Thread {
    /**
     * Lower values have higher cleanup priority which means to be closed earlier
     */
    private static final int                  DEFAULT_PRIORITY = 20;
    /**
     * Eager instance initialized while class load
     */
    private static final ShutdownHook         INSTANCE         = new ShutdownHook();
    /**
     * Resource list to be cleanup
     */
    private static final List<ClosableObject> RESOURCES        = new ArrayList<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ShutdownHook() {
    }

    public static synchronized void registerShutdownHook(Closable closable, int priority) {
        INSTANCE.RESOURCES.add(new ClosableObject(closable, priority));
        log.info("Registered the class [{}] to {}", closable.getClass(), ShutdownHook.class.getSimpleName());
    }

    /**
     * Only global resources are allowed to be register to ShutDownHook, don't register connections to it.
     *
     * @param closable
     */
    public static void registerShutdownHook(Closable closable) {
        registerShutdownHook(closable, DEFAULT_PRIORITY);
    }

    public static void runNow(boolean sync) {
        if (sync) {
            INSTANCE.run();
        } else {
            INSTANCE.start();
        }
    }

    @Override
    public void run() {
        closeAll();
    }

    private synchronized void closeAll() {
        Collections.sort(RESOURCES);
        log.info("Start to close global resource due to priority");
        for (ClosableObject resource : RESOURCES) {
            try {
                resource.closable.close();
            } catch (Exception e) {
                log.error("Failed to close " + resource.closable.getClass(), e);
            }
            log.info("Success to close " + resource.closable.getClass());
        }
        log.info("Success to close all the resource!");
        RESOURCES.clear();
    }

    @AllArgsConstructor
    private static class ClosableObject implements Comparable<ClosableObject> {
        private Closable closable;
        private int      priority;

        @Override
        public int compareTo(ClosableObject o) {
            if (this.priority > o.priority) {
                return -1;
            } else if (this.priority == o.priority) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}

