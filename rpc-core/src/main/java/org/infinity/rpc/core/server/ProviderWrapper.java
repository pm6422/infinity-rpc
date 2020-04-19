package org.infinity.rpc.core.server;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j
public class ProviderWrapper<T> {

    /**
     * The provider interface full name
     */
    private String providerInterface;

    /**
     * The provider instance name
     */
    private String providerInstanceName;

    /**
     * The provider instance
     */
    private T providerInstance;

    /**
     * The method is invoked by Java EE container automatically
     */
    @PostConstruct
    public void init() {
        ProviderWrapperHolder.getInstance().addProvider(providerInterface, this);
    }

    public String getProviderInterface() {
        return providerInterface;
    }

    public void setProviderInterface(String providerInterface) {
        this.providerInterface = providerInterface;
    }

    public String getProviderInstanceName() {
        return providerInstanceName;
    }

    public void setProviderInstanceName(String providerInstanceName) {
        this.providerInstanceName = providerInstanceName;
    }

    public T getProviderInstance() {
        return providerInstance;
    }

    public void setProviderInstance(T providerInstance) {
        this.providerInstance = providerInstance;
    }

    public void register() {
        log.debug("Published RPC provider [{}] to registry", providerInterface);
    }
}
