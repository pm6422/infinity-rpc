package com.luixtech.rpc.core.client.faulttolerance.impl;

import com.luixtech.rpc.core.client.sender.Sendable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.constant.ConsumerConstants;
import com.luixtech.rpc.core.constant.ServiceConstants;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import com.luixtech.rpc.core.exception.ExceptionUtils;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

import java.text.MessageFormat;
import java.util.List;

/**
 * Failover is a backup mode of operation, when the primary system exception that functions to the secondary system.
 * Failover fault tolerance strategy means that when a service node fails to call, it will continue to call other service nodes.
 */
@Slf4j
@SpiName(ConsumerConstants.FAULT_TOLERANCE_VAL_FAILOVER)
public class FailoverFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        // Select all active senders
        List<Sendable> allActiveSenders = loadBalancer.selectAllActiveSenders(request);
        int retryCount;
        if (StringUtils.isNotEmpty(request.getOption(ServiceConstants.RETRY_COUNT))) {
            retryCount = request.getIntOption(ServiceConstants.RETRY_COUNT);
        } else {
            // Get method level parameter value
            retryCount = allActiveSenders.get(0).getProviderUrl()
                    .getMethodLevelOption(request.getMethodName(), request.getMethodParameters(),
                            ServiceConstants.RETRY_COUNT, ServiceConstants.RETRY_COUNT_VAL_DEFAULT);
        }

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= retryCount; i++) {
            Sendable sender = allActiveSenders.get(i % allActiveSenders.size());
            try {
                request.setRetryNumber(i);
                // Send RPC request
                return sender.sendRequest(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= retryCount) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call {0}", sender.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to perform " + retryCount + " retries to call " + allActiveSenders.get(0).getProviderUrl() + "!");
    }
}
