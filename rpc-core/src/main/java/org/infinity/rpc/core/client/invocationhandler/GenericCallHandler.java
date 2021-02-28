package org.infinity.rpc.core.client.invocationhandler;

import java.util.Map;

public interface GenericCallHandler {
    /**
     * Generic call
     *
     * @param methodName       method name
     * @param methodParamTypes method parameter types string
     * @param args             method arguments
     * @param options          call options
     * @return return object
     */
    Object call(String methodName, String[] methodParamTypes, Object[] args, Map<String, String> options);
}