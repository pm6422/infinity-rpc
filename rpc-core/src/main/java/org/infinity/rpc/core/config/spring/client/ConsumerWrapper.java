package org.infinity.rpc.core.config.spring.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 */
@Slf4j
@Data
@Builder
public class ConsumerWrapper<T> implements DisposableBean {
    /**
     * The consumer interface class fully-qualified name
     */
//    private String   interfaceName;
    /**
     * The interface class of the consumer
     */
    private Class<?> interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private String   instanceName;
    /**
     * The consumer proxy instance, refer the return type of {@link org.infinity.rpc.core.client.proxy.RpcConsumerProxy#getProxy(Class)}
     */
    private T        proxyInstance;

    @Override
    public void destroy() throws Exception {
        // Leave blank intentionally for now
    }

    public T getProxyInstance() {
        if (proxyInstance == null) {
            initProxyInstance();
        }
        return proxyInstance;
    }

    private void initProxyInstance() {
        //todo
    }
}
