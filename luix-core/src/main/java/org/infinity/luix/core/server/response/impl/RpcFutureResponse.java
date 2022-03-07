package org.infinity.luix.core.server.response.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.TraceableContext;
import org.infinity.luix.core.exchange.constants.FutureState;
import org.infinity.luix.core.protocol.constants.ProtocolVersion;
import org.infinity.luix.core.server.response.FutureListener;
import org.infinity.luix.core.server.response.FutureResponse;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.serializer.DeserializableResult;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@ToString
public class RpcFutureResponse implements FutureResponse, Serializable {
    private static final long                 serialVersionUID = -8089955194208179445L;
    protected final      Object               lock             = new Object();
    protected volatile   FutureState          state            = FutureState.DOING;
    protected            Object               resultObject;
    protected            Exception            exception;
    protected            long                 createdTime      = System.currentTimeMillis();
    protected            String               protocol;
    protected            byte                 protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    protected            String               group;
    protected            String               version;
    protected            int                  timeout;
    protected            long                 processTime      = 0;
    protected            Requestable          request;
    protected            List<FutureListener> listeners;
    protected            Url                  serverUrl;
    protected            Class<?>             returnType;
    protected            long                 sendingTime;
    protected            long                 receivedTime;
    protected            long                 elapsedTime;
    protected            Map<String, String>  traces           = new ConcurrentHashMap<>();
    /**
     * RPC request options, all the optional RPC request parameters will be put in it.
     */
    protected            Map<String, String>  options          = new ConcurrentHashMap<>();
    protected            TraceableContext     traceableContext = new TraceableContext();
    /**
     * default serialization is hession2
     */
    protected            int                  serializerId     = 0;

    public RpcFutureResponse(Requestable requestObj, int timeout, Url serverUrl) {
        this.request = requestObj;
        this.timeout = timeout;
        this.serverUrl = serverUrl;
    }

    @Override
    public void onSuccess(Responseable response) {
        this.resultObject = response.getResult();
        this.processTime = response.getElapsedTime();
        this.options = response.getOptions();
        traceableContext.setReceiveTime(response.getReceivedTime());
        response.getTraces().forEach((key, value) -> traceableContext.addTraceInfo(key, value));
        done();
    }

    @Override
    public void onFailure(Responseable response) {
        this.exception = response.getException();
        this.processTime = response.getElapsedTime();
        done();
    }

    @Override
    public boolean cancel() {
        Exception exception = new RpcFrameworkException("Processed request in " + (System.currentTimeMillis() - createdTime)
                + "ms with address " + serverUrl.getAddress() + " and " + request);
        log.error(exception.getMessage());
        return cancel(exception);
    }

    protected boolean cancel(Exception e) {
        synchronized (lock) {
            if (!isDoing()) {
                return false;
            }

            state = FutureState.CANCELLED;
            exception = e;
            lock.notifyAll();
        }

        notifyListeners();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return state.isCancelledState();
    }

    @Override
    public boolean isDone() {
        return state.isDoneState();
    }

    @Override
    public boolean isSuccess() {
        return isDone() && (exception == null);
    }

    @Override
    public void addListener(FutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("FutureListener is null");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (!isDoing()) {
                notifyNow = true;
            } else {
                if (listeners == null) {
                    listeners = new ArrayList<>(1);
                }

                listeners.add(listener);
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    @Override
    public void setReturnType(Class<?> clazz) {
        this.returnType = clazz;
    }

    public FutureState getState() {
        return state;
    }

    private void timeoutToCancel() {
        this.processTime = System.currentTimeMillis() - createdTime;

        synchronized (lock) {
            if (!isDoing()) {
                return;
            }

            state = FutureState.CANCELLED;
            exception = new RpcFrameworkException("Failed to request server " + serverUrl.getAddress()
                    + " with timeout of " + processTime + "ms for " + request);
            lock.notifyAll();
        }

        notifyListeners();
    }

    private void notifyListeners() {
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(this::notifyListener);
    }

    private void notifyListener(FutureListener listener) {
        try {
            listener.operationComplete(this);
        } catch (Throwable t) {
            log.error(this.getClass().getName() + " notifyListener Error: " + listener.getClass().getSimpleName(), t);
        }
    }

    private boolean isDoing() {
        return state.isDoingState();
    }

    protected boolean done() {
        synchronized (lock) {
            if (!isDoing()) {
                return false;
            }

            state = FutureState.DONE;
            lock.notifyAll();
        }

        notifyListeners();
        return true;
    }

    @Override
    public long getRequestId() {
        return this.request.getRequestId();
    }

    @Override
    public Object getResult() {
        synchronized (lock) {
            if (!isDoing()) {
                return getResultOrThrowable();
            }

            if (timeout <= 0) {
                try {
                    lock.wait();
                } catch (Exception e) {
                    cancel(new RpcFrameworkException(this.getClass().getName() + " getValue InterruptedException : "
                            + request.toString() + " cost=" + (System.currentTimeMillis() - createdTime), e));
                }
                return getResultOrThrowable();
            } else {
                long waitTime = timeout - (System.currentTimeMillis() - createdTime);
                if (waitTime > 0) {
                    for (; ; ) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException e) {
                            // Leave blank intentionally
                        }

                        if (!isDoing()) {
                            break;
                        } else {
                            waitTime = timeout - (System.currentTimeMillis() - createdTime);
                            if (waitTime <= 0) {
                                break;
                            }
                        }
                    }
                }

                if (isDoing()) {
                    timeoutToCancel();
                }
            }
            return getResultOrThrowable();
        }
    }

    private Object getResultOrThrowable() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RpcFrameworkException(
                    exception.getMessage(), exception);
        }
        if (resultObject != null && returnType != null && resultObject instanceof DeserializableResult) {
            try {
                resultObject = ((DeserializableResult) resultObject).deserialize();
            } catch (IOException e) {
                log.error("deserialize response value fail! return type:" + returnType, e);
                throw new RpcFrameworkException("deserialize return value fail! deserialize type:" + returnType, e);
            }
        }
        return resultObject;
    }

    @Override
    public void addTrace(String key, String value) {
        traces.putIfAbsent(key, value);
    }

    @Override
    public String getTrace(String key) {
        return traces.get(key);
    }

    @Override
    public void addOption(String key, String value) {
        if (this.options == null) {
            this.options = new HashMap<>(10);
        }
        this.options.put(key, value);
    }

    @Override
    public String getOption(String key) {
        return options.get(key);
    }

    @Override
    public String getOption(String key, String defaultValue) {
        String value = getOption(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public int getIntOption(String key) {
        return Integer.parseInt(options.get(key));
    }

    @Override
    public int getIntOption(String key, int defaultValue) {
        String value = getOption(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
