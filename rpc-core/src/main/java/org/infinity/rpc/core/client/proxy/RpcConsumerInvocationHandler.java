package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.request.impl.RpcRequest;
import org.infinity.rpc.utilities.id.IdGenerator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class RpcConsumerInvocationHandler<T> extends AbstractRpcConsumerInvocationHandler<T> implements InvocationHandler {

    public RpcConsumerInvocationHandler(Class<T> interfaceClass, List<Cluster<T>> clusters) {
        super.interfaceClass = interfaceClass;
        super.interfaceName = interfaceClass.getName();
        super.clusters = clusters;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isLocalMethod(method)) {

        }

        RpcRequest request = RpcRequest.builder()
                .requestId(IdGenerator.generateTimestampId())
                .interfaceName(interfaceName)
                .methodName(method.getName())
                .methodArguments(args)
                .build();

        boolean async = false;
        Class returnType = getRealReturnType(async, this.clazz, method, method.getName());
        return invokeRequest(request, returnType, async);
    }

    /**
     * toString,equals,hashCode,finalize等接口未声明的方法不进行远程调用
     *
     * @param method
     * @return
     */
    public boolean isLocalMethod(Method method) {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                Method interfaceMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    private Class<?> getRealReturnType(boolean asyncCall, Class<?> clazz, Method method, String methodName) {
        if (asyncCall) {
            try {
                Method m = clazz.getMethod(methodName, method.getParameterTypes());
                return m.getReturnType();
            } catch (Exception e) {
                log.warn("RefererInvocationHandler get real return type fail. err:" + e.getMessage());
                return method.getReturnType();
            }
        } else {
            return method.getReturnType();
        }
    }
}
