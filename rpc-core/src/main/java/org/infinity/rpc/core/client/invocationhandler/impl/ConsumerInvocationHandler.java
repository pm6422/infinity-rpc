package org.infinity.rpc.core.client.invocationhandler.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.infinity.rpc.core.client.invocationhandler.AbstractConsumerInvocationHandler;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.impl.JdkProxyFactory;
import org.infinity.rpc.core.client.request.impl.RpcRequest;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.server.response.FutureResponse;
import org.infinity.rpc.utilities.id.IdGenerator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.core.utils.MethodParameterUtils.*;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public class ConsumerInvocationHandler<T> extends AbstractConsumerInvocationHandler<T>
        implements InvocationHandler, UniversalInvocationHandler {

    public ConsumerInvocationHandler(ConsumerStub<T> consumerStub) {
        super.consumerStub = consumerStub;
    }

    /**
     * Invoke this method every time when all the methods of RPC consumer been invoked
     *
     * @param proxy  consumer proxy instance
     * @param method consumer method
     * @param args   consumer method arguments
     * @return RPC invocation result
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (isDerivedFromObject(method)) {
            // IDE may call the Object.toString() method if you set some break pointers.
            return JdkProxyFactory.class.getSimpleName();
        }

        // Create a new RpcRequest for each request
        RpcRequest request = new RpcRequest(IdGenerator.generateTimestampId(),
                consumerStub.getInterfaceName(),
                method.getName(),
                getMethodParameters(method),
                isAsyncMethod(method));

        // Set method arguments
        request.setMethodArguments(args);

        // Set some options
        request.addOption(GROUP, consumerStub.getGroup());
        request.addOption(VERSION, consumerStub.getVersion());
        request.addOption(CHECK_HEALTH_FACTORY, consumerStub.getCheckHealthFactory());
        request.addOption(REQUEST_TIMEOUT, consumerStub.getRequestTimeout(), REQUEST_TIMEOUT_VAL_DEFAULT);
        request.addOption(MAX_RETRIES, consumerStub.getMaxRetries(), MAX_RETRIES_VAL_DEFAULT);
        request.addOption(MAX_PAYLOAD, consumerStub.getMaxPayload(), MAX_PAYLOAD_VAL_DEFAULT);

        return processRequest(request, method.getReturnType());
    }

    /**
     * It is a asynchronous method calling if the return type of method is type of {@link FutureResponse}
     *
     * @param method method
     * @return true: async call, false: sync call
     */
    private boolean isAsyncMethod(Method method) {
        return method.getReturnType().equals(FutureResponse.class);
    }

    @Override
    public Object invoke(String methodName, String[] methodParamTypes, Object[] args, Map<String, String> options) {
        // Create a new RpcRequest for each request
        RpcRequest request = new RpcRequest(IdGenerator.generateTimestampId(),
                consumerStub.getInterfaceName(),
                methodName,
                ArrayUtils.isEmpty(methodParamTypes) ? VOID : String.join(PARAM_TYPE_STR_DELIMITER, methodParamTypes),
                false);

        // Set method arguments
        request.setMethodArguments(args);

        // Set some options
        request.setOptions(options);
        return processRequest(request, Object.class);
    }
}
