package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.client.ConsumerWrapper;
import org.infinity.rpc.core.exchange.request.impl.RpcRequest;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public class ConsumerInvocationHandler<T> extends AbstractRpcConsumerInvocationHandler<T> implements InvocationHandler {

    public ConsumerInvocationHandler(ConsumerWrapper<T> wrapper) {
        consumerWrapper = wrapper;
    }

    /**
     * Call this method every time when all the methods of RPC consumer been invoked
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
            return ClassUtils.getShortNameAsProperty(ConsumerProxy.class);
        }

        RpcRequest request = new RpcRequest();
        request.setRequestId(IdGenerator.generateTimestampId());
        request.setInterfaceName(consumerWrapper.getInterfaceClass().getName());
        request.setMethodName(method.getName());
        request.setMethodArguments(args);

        boolean async = isAsyncCall(args);
        return processRequest(request, method.getReturnType(), async);
    }

    /**
     * Check whether the method is derived from {@link Object} class.
     * e.g, toString, equals, hashCode, finalize
     *
     * @param method method
     * @return true: method derived from Object class, false: otherwise
     */
    public boolean isDerivedFromObject(Method method) {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                consumerWrapper.getInterfaceClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    /**
     * It is a asynchronous request if any argument of the method is type of AsyncRequestFlag.ASYNC
     *
     * @param args method arguments
     * @return true: async call, false: sync call
     */
    private boolean isAsyncCall(Object[] args) {
        return args != null && Arrays.stream(args)
                .anyMatch(arg -> (arg instanceof AsyncRequestFlag) && (AsyncRequestFlag.ASYNC.equals(arg)));
    }
}
