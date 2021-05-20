package org.infinity.rpc.core.client.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.impl.RpcInvocationException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.DESTROY_INVOKER_THREAD_POOL;

/**
 *
 */
@Slf4j
public abstract class AbstractLoadBalancer implements LoadBalancer {
    private static final int             DELAY_TIME = 1000;
    protected            List<Invokable> invokers;

    /**
     * Update new provider invokers
     *
     * @param invokers new provider invokers
     */
    @Override
    public synchronized void refresh(List<Invokable> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }
        List<Invokable> oldInvokers = this.invokers;
        // Assign new ones to provider invokers
        this.invokers = invokers;

        if (CollectionUtils.isEmpty(oldInvokers)) {
            return;
        }

        Collection<Invokable> inactiveOnes = CollectionUtils.subtract(invokers, oldInvokers);
        if (CollectionUtils.isEmpty(inactiveOnes)) {
            return;
        }
        // Destroy the inactive provider invokers
        destroyInactiveInvokers(inactiveOnes);
    }

    @Override
    public Invokable selectProviderNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.invokers)) {
            throw new RpcInvocationException("No available provider invoker for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }

        // Make a copy for thread safe purpose
        List<Invokable> invokers = new ArrayList<>(this.invokers);
        Invokable invoker = null;
        if (invokers.size() > 1) {
            invoker = doSelectNode(request);
        } else if (invokers.size() == 1 && invokers.get(0).isActive()) {
            invoker = invokers.get(0);
        }
        if (invoker == null) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        return invoker;
    }

    @Override
    public List<Invokable> selectProviderNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.invokers)) {
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        // Make a copy for thread safe purpose
        List<Invokable> invokers = new ArrayList<>(this.invokers);
        List<Invokable> selected = new ArrayList<>();
        if (invokers.size() > 1) {
            selected = doSelectNodes(request);
        } else if (invokers.size() == 1 && invokers.get(0).isActive()) {
            selected.add(invokers.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        return selected;
    }

    @Override
    public List<Invokable> getInvokers() {
        return invokers;
    }

    private void destroyInactiveInvokers(Collection<Invokable> delayDestroyInvokers) {
        // Execute once after a daley time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_INVOKER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
            for (Invokable invoker : delayDestroyInvokers) {
                try {
                    invoker.destroy();
                    log.info("Destroyed the caller with url: {}", invoker.getProviderUrl().getUri());
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the caller with url: {0}", invoker.getProviderUrl().getUri()), e);
                }
            }
        });
    }

    @Override
    public void destroy() {
        if (CollectionUtils.isNotEmpty(invokers)) {
            invokers.forEach(Invokable::destroy);
        }
    }

    /**
     * Select one provider node
     *
     * @param request request instance
     * @return selected provider invoker
     */
    protected abstract Invokable doSelectNode(Requestable request);

    /**
     * Select multiple provider nodes
     *
     * @param request request instance
     * @return selected provider invokers
     */
    protected abstract List<Invokable> doSelectNodes(Requestable request);
}
