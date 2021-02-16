package org.infinity.rpc.core.server.response;

public interface FutureResponse extends Responseable, Future {
    void onSuccess(Responseable response);

    void onFailure(Responseable response);

    long getCreateTime();

    void setReturnType(Class<?> clazz);
}
