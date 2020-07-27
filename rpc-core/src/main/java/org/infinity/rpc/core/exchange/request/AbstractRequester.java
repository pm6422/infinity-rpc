package org.infinity.rpc.core.exchange.request;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractRequester<T> implements Requester<T> {
    protected          AtomicBoolean initialized        = new AtomicBoolean(false);
    protected volatile boolean       available          = false;
    protected          Class<T>      interfaceClass;
    protected          Url           url;
    protected          Url           providerUrl;
    protected          AtomicInteger activeRefererCount = new AtomicInteger(0);

    public AbstractRequester(Class<T> interfaceClass, Url url) {
        this.interfaceClass = interfaceClass;
        this.url = url;
    }

    public AbstractRequester(Class<T> interfaceClass, Url url, Url providerUrl) {
        this.interfaceClass = interfaceClass;
        this.url = url;
        this.providerUrl = providerUrl;
    }

    @Override
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Url getUrl() {
        return url;
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            // todo: refactor log
            log.warn("Requester {} already initialized!", this.getClass().getSimpleName());
            return;
        }

        boolean result = doInit();

        if (!result) {
            log.error("Failed to initialize the requester {}!", this.getClass().getSimpleName());
            throw new RpcFrameworkException("Failed to initialize the requester " + this.getClass().getSimpleName() + "!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } else {
            log.info("Initialized the requester {}", this.getClass().getSimpleName());
            available = true;
        }
    }

    protected abstract boolean doInit();

    @Override
    public Responseable call(Requestable request) {
        if (!isAvailable()) {
            // todo: refactor log
            throw new RpcFrameworkException("Requester is NOT available for now!");
        }



        return null;
    }
}
