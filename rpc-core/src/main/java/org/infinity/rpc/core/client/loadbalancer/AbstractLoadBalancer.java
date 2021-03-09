package org.infinity.rpc.core.client.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.request.Importable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.RpcInvocationException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.DESTROY_CALLER_THREAD_POOL;

/**
 *
 */
@Slf4j
public abstract class AbstractLoadBalancer implements LoadBalancer {
    private static final int              DELAY_TIME = 1000;
    protected            List<Importable> importers;

    @Override
    public void refresh(List<Importable> newImporters) {
        List<Importable> oldImporters = importers;
        // Assign new ones to provider callers
        importers = newImporters;

        if (CollectionUtils.isEmpty(oldImporters)) {
            return;
        }

        Collection<Importable> inactiveOnes = CollectionUtils.subtract(newImporters, oldImporters);
        if (CollectionUtils.isEmpty(inactiveOnes)) {
            return;
        }
        // Destroy the inactive provider callers
        destroyInactiveProviderCallers(inactiveOnes);
    }

    @Override
    public Importable selectProviderNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.importers)) {
            throw new RpcInvocationException("No available provider caller for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }

        // Make a copy for thread safe purpose
        List<Importable> importers = new ArrayList<>(this.importers);
        Importable importer = null;
        if (importers.size() > 1) {
            importer = doSelectNode(request);
        } else if (importers.size() == 1 && importers.get(0).isActive()) {
            importer = importers.get(0);
        }
        if (importer == null) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider caller for now, " +
                    "please check whether the server is ok!");
        }
        return importer;
    }

    @Override
    public List<Importable> selectProviderNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.importers)) {
            throw new RpcInvocationException("No active provider caller for now, " +
                    "please check whether the server is ok!");
        }
        // Make a copy for thread safe purpose
        List<Importable> importers = new ArrayList<>(this.importers);
        List<Importable> selected = new ArrayList<>();
        if (importers.size() > 1) {
            selected = doSelectNodes(request);
        } else if (importers.size() == 1 && importers.get(0).isActive()) {
            selected.add(importers.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider caller for now, " +
                    "please check whether the server is ok!");
        }
        return selected;
    }

    @Override
    public List<Importable> getProviderCallers() {
        return importers;
    }

    private void destroyInactiveProviderCallers(Collection<Importable> delayDestroyImporters) {
        // Execute once after a daley time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_CALLER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
            for (Importable importer : delayDestroyImporters) {
                try {
                    importer.destroy();
                    log.info("Destroyed the caller with url: {}", importer.getProviderUrl().getUri());
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the caller with url: {0}", importer.getProviderUrl().getUri()), e);
                }
            }
        });
    }

    @Override
    public void destroy() {
        importers.forEach(Importable::destroy);
    }

    /**
     * Select one provider node
     *
     * @param request request instance
     * @return selected provider caller
     */
    protected abstract Importable doSelectNode(Requestable request);

    /**
     * Select multiple provider nodes
     *
     * @param request request instance
     * @return selected provider callers
     */
    protected abstract List<Importable> doSelectNodes(Requestable request);
}
