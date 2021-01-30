package org.infinity.rpc.core.exchange.cluster.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.faulttolerance.FaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.DESTROY_CALLER_THREAD_POOL;

/**
 * todo: ClusterSpi
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("default")
public class DefaultProviderCluster<T> implements ProviderCluster<T> {
    private static final int                       DELAY_TIME = 1000;
    private              boolean                   active     = false;
    private              Class<T>                  interfaceClass;
    private              String                    protocol;
    private              FaultToleranceStrategy<T> faultToleranceStrategy;
    private              LoadBalancer<T>           loadBalancer;
    private              List<ProviderCaller<T>>   providerCallers;

    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setLoadBalancer(@NonNull LoadBalancer<T> loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public LoadBalancer<T> getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void setFaultToleranceStrategy(@NonNull FaultToleranceStrategy<T> faultToleranceStrategy) {
        this.faultToleranceStrategy = faultToleranceStrategy;
    }

    @Override
    public FaultToleranceStrategy<T> getFaultToleranceStrategy() {
        return faultToleranceStrategy;
    }

    @Override
    public List<ProviderCaller<T>> getProviderCallers() {
        return providerCallers;
    }

    @Override
    public void init() {
        // todo: remove this statement
        refresh(providerCallers);
        active = true;
    }

    /**
     * Update new provider callers of load balancer
     *
     * @param newProviderCallers new provider callers
     */
    @Override
    public synchronized void refresh(List<ProviderCaller<T>> newProviderCallers) {
        if (CollectionUtils.isEmpty(newProviderCallers)) {
            return;
        }
        // Set new provider callers to load balancer
        loadBalancer.refresh(newProviderCallers);

        List<ProviderCaller<T>> oldProviderCallers = providerCallers;
        // Assign new ones to provider callers
        providerCallers = newProviderCallers;

        if (CollectionUtils.isEmpty(oldProviderCallers)) {
            return;
        }

        Collection<ProviderCaller<T>> inactiveOnes = CollectionUtils.subtract(newProviderCallers, oldProviderCallers);
        if (CollectionUtils.isEmpty(inactiveOnes)) {
            return;
        }
        // Destroy the inactive provider callers
        destroyInactiveProviderCallers(inactiveOnes);
    }

    private void destroyInactiveProviderCallers(Collection<ProviderCaller<T>> delayDestroyProviderCallers) {
        // Execute once after a daley time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_CALLER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
            for (ProviderCaller<?> providerCaller : delayDestroyProviderCallers) {
                try {
                    providerCaller.destroy();
                    log.info("Destroyed the caller with url: {}", providerCaller.getProviderUrl().getUri());
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the caller with url: {0}", providerCaller.getProviderUrl().getUri()), e);
                }
            }
        });
    }

    @Override
    public void destroy() {
        active = false;
        for (ProviderCaller<T> providerCaller : this.providerCallers) {
            providerCaller.destroy();
        }
    }

    @Override
    public Responseable call(Requestable request) {
        if (active) {
            try {
                return faultToleranceStrategy.call(loadBalancer, request);
            } catch (Exception e) {
                return handleError(request, e);
            }
        }
        return handleError(request, new RpcServiceException(RpcErrorMsgConstant.SERVICE_NOT_FOUND));
    }

    private Responseable handleError(Requestable request, Exception cause) {
        if (ExceptionUtils.isBizException(cause)) {
            // Throw the exception if it is business one
            throw (RuntimeException) cause;
        }

        boolean parameter = faultToleranceStrategy.getClientUrl()
                .getBooleanOption(Url.PARAM_THROW_EXCEPTION, Url.PARAM_THROW_EXCEPTION_DEFAULT_VALUE);
        if (parameter) {
            if (cause instanceof RpcAbstractException) {
                throw (RpcAbstractException) cause;
            } else {
                throw new RpcServiceException("Failed to call the request!", cause);
            }
        }
        return RpcResponse.error(request, cause);
    }

    @Override
    public String toString() {
        if (interfaceClass == null) {
            return DefaultProviderCluster.class.getSimpleName();
        }
        return DefaultProviderCluster.class.getSimpleName().concat(":").concat(interfaceClass.getName());
    }
}
