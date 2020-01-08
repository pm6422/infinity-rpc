package org.infinity.rpc.client;


import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZookeeperRpcServerDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * 获取到接口对应的代理对象，被代理对象的所有的方法调用时都会执行invoke方法
 */
public class RpcClientProxy {
    private ZookeeperRpcServerDiscovery zookeeperRpcServerDiscovery;

    public RpcClientProxy(ZookeeperRpcServerDiscovery zookeeperRpcServerDiscovery) {
        this.zookeeperRpcServerDiscovery = zookeeperRpcServerDiscovery;
    }

    @SuppressWarnings("all")
    public <T> T getInstance(Class<T> interfaceClass) {
        T instance = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, MethodInvocationHandler.getInstance(zookeeperRpcServerDiscovery));
        //返回一个代理对象
        return instance;
    }

    static class MethodInvocationHandler implements InvocationHandler {

        private static final Logger                      LOGGER = LoggerFactory.getLogger(MethodInvocationHandler.class);
        private static       ZookeeperRpcServerDiscovery zookeeperRpcServerDiscovery;

        private MethodInvocationHandler() {
        }

        public static MethodInvocationHandler getInstance(ZookeeperRpcServerDiscovery discovery) {
            zookeeperRpcServerDiscovery = discovery;
            return SingletonHolder.INSTANCE;
        }

        private static class SingletonHolder {
            private static final MethodInvocationHandler INSTANCE = new MethodInvocationHandler();// 静态变量在类加载时就会初始化创建对象
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().getName().equals(Object.class.getName()) && method.getName().equals("toString")) {
                // TODO: 调查调用的原因
                LOGGER.debug("Invoked Object.toString()");
                return null;
            }
            // 创建请求对象，包含类名，方法名，参数类型和实际参数值
            RpcRequest rpcRequest = new RpcRequest(UUID.randomUUID().toString(),
                    method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), args);
            LOGGER.debug("RPC request: {}", rpcRequest);
            // 创建client对象，并且发送消息到服务端
            RpcClient rpcClient = new RpcClient(rpcRequest, zookeeperRpcServerDiscovery);
            RpcResponse rpcResponse = rpcClient.send();
            // 返回调用结果
            return rpcResponse.getResult();
        }
    }
}



