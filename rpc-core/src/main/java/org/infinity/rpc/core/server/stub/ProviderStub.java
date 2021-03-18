package org.infinity.rpc.core.server.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcBizException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.MethodParameterUtils;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * PRC provider stub
 * A stub in distributed computing is a piece of code that converts parameters passed between client and server
 * during a remote procedure call(RPC).
 * A provider stub take charge of handling remote method invocation and delegate to associate local method to execute.
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@Setter
@Getter
public class ProviderStub<T> {
    /**
     * Provider stub bean name
     */
    @NotNull(message = "The [beanName] property must NOT be null!")
    private           String              beanName;
    /**
     * The interface class of the provider
     */
    @NotNull(message = "The [interfaceClass] property of @Provider must NOT be null!")
    private           Class<T>            interfaceClass;
    /**
     * The provider interface fully-qualified name
     */
    @NotEmpty(message = "The [interfaceName] property of @Provider must NOT be empty!")
    private           String              interfaceName;
    /**
     * Protocol
     */
    private           String              protocol;
//    /**
//     * Registry
//     */
//    private           String              registry;
    /**
     * Group
     */
    private           String              group;
    /**
     * Version
     */
    private           String              version;
    /**
     *
     */
    private           String              checkHealthFactory;
    /**
     *
     */
    @Min(value = 0, message = "The [timeout] property of @Provider must NOT be a negative number!")
    private           Integer             requestTimeout;
    /**
     * The max retry times of RPC request
     */
    @Min(value = 0, message = "The [maxRetries] property of @Provider must NOT be a negative number!")
    private           Integer             maxRetries;
    /**
     * The max response message payload size in bytes
     */
    @Min(value = 0, message = "The [maxPayload] property of @Provider must NOT be a positive number!")
    private           Integer             maxPayload;
    /**
     * The provider instance
     * Disable serialize
     */
    @NotNull
    private transient T                   instance;
    /**
     * Method signature to method cache map for the provider class
     */
    private transient Map<String, Method> methodsCache = new HashMap<>();
    /**
     * The provider url
     */
    private           Url                 url;
    /**
     * Indicator used to identify whether the provider already been registered
     */
    private final     AtomicBoolean       exported     = new AtomicBoolean(false);

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     */
    @PostConstruct
    public void init() {
        // Automatically add {@link ProviderStub} instance to {@link ProviderStubHolder}
        ProviderStubHolder.getInstance().addStub(beanName, this);
        // Put methods to cache in order to accelerate the speed of executing.
        discoverMethods(interfaceClass);
    }

    /**
     * Discover all methods of provider interface and put them all to cache
     *
     * @param interfaceClass The interface class of the provider
     */
    private void discoverMethods(Class<T> interfaceClass) {
        // Get all methods of the class passed in or its super interfaces.
        Arrays.stream(interfaceClass.getMethods()).forEach(method -> {
            String methodSignature = MethodParameterUtils.getMethodSignature(method);
            methodsCache.putIfAbsent(methodSignature, method);
        });
    }

    /**
     * Find method associated with name and parameter list
     *
     * @param methodName       method name
     * @param methodParameters method parameter list. e.g, java.util.List,java.lang.Long
     * @return method
     */
    public Method findMethod(String methodName, String methodParameters) {
        return methodsCache.get(MethodParameterUtils.getMethodSignature(methodName, methodParameters));
    }

    /**
     * Register the RPC providers to registries
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registryConfig    registry configuration
     */
    public void register(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, RegistryConfig registryConfig) {
        // Export provider url
        Url providerUrl = createProviderUrl(applicationConfig, protocolConfig, registryConfig);

        if (exported.compareAndSet(false, true)) {
            // Export RPC provider service
            Protocol.getInstance(providerUrl.getProtocol()).export(this);
        }

        // Register provider URL to all the registries
        registryConfig.getRegistryImpl().register(providerUrl);
//            registry.registerApplicationProvider(applicationConfig.getName(), providerUrl);
        log.debug("Registered RPC provider [{}] to registry [{}]", interfaceName,
                registryConfig.getRegistryImpl().getRegistryUrl().getProtocol());
    }

    /**
     * Merge high priority properties to provider stub and generate provider url
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registryConfig    registry configuration
     * @return provider url
     */
    private Url createProviderUrl(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, RegistryConfig registryConfig) {
        url = Url.providerUrl(protocol, protocolConfig.getHost(), protocolConfig.getPort(), interfaceName, group, version);
        url.addOption(Url.PARAM_APP, applicationConfig.getName());
        url.addOption(CODEC, protocolConfig.getCodec());
        url.addOption(LOCAL_ADDRESS_FACTORY, protocolConfig.getLocalAddressFactory());
        url.addOption(ENDPOINT_FACTORY, protocolConfig.getEndpointFactory());
        url.addOption(MIN_CLIENT_CONN, String.valueOf(protocolConfig.getMinClientConn()));
        url.addOption(MAX_CLIENT_FAILED_CONN, String.valueOf(protocolConfig.getMaxClientFailedConn()));
        url.addOption(MAX_SERVER_CONN, String.valueOf(protocolConfig.getMaxServerConn()));
        url.addOption(MAX_CONTENT_LENGTH, String.valueOf(protocolConfig.getMaxContentLength()));
        url.addOption(MIN_THREAD, String.valueOf(protocolConfig.getMinThread()));
        url.addOption(MAX_THREAD, String.valueOf(protocolConfig.getMaxThread()));
        url.addOption(WORK_QUEUE_SIZE, String.valueOf(protocolConfig.getWorkQueueSize()));
        url.addOption(SHARED_CHANNEL, String.valueOf(protocolConfig.isSharedChannel()));
        url.addOption(ASYNC_INIT_CONN, String.valueOf(protocolConfig.isAsyncInitConn()));
        url.addOption(CHECK_HEALTH_FACTORY, checkHealthFactory);
        url.addOption(REQUEST_TIMEOUT, requestTimeout != null ? requestTimeout.toString() : null);
        url.addOption(MAX_RETRIES, maxRetries != null ? maxRetries.toString() : null);
        url.addOption(MAX_PAYLOAD, maxPayload != null ? maxPayload.toString() : null);

        return url;
    }

    /**
     * Unregister the RPC provider from registry
     */
    public void unregister(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.getRegisteredProviderUrls().forEach(registry::unregister);
            log.debug("Unregistered RPC provider [{}] from registry [{}]", interfaceName, registryUrl.getProtocol());
        }
    }

    /**
     * Invoke method locally and return the result
     *
     * @param request RPC request
     * @return RPC response
     */
    public Responseable localInvoke(Requestable request) {
        RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_BEFORE_BIZ);
        Responseable response = doLocalInvoke(request);
        RpcFrameworkUtils.logEvent(response, RpcConstants.TRACE_AFTER_BIZ);
        return response;
    }

    /**
     * Do the invocation of method
     *
     * @param request RPC request
     * @return RPC response
     */
    private Responseable doLocalInvoke(Requestable request) {
        RpcResponse response = new RpcResponse();
        Method method = findMethod(request.getMethodName(), request.getMethodParameters());
        if (method == null) {
            RpcServiceException exception =
                    new RpcServiceException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName()
                            + "(" + request.getMethodParameters() + ")", RpcErrorMsgConstant.SERVICE_NOT_FOUND);
            response.setException(exception);
            return response;
        }
        boolean defaultThrowExceptionStack = TRANS_EXCEPTION_STACK_VAL_DEFAULT;
        try {
            // Invoke method
            Object result = method.invoke(instance, request.getMethodArguments());
            response.setResultObject(result);
        } catch (Exception e) {
            if (e.getCause() != null) {
                response.setException(new RpcBizException("provider call process error", e.getCause()));
            } else {
                response.setException(new RpcBizException("provider call process error", e));
            }

            // not print stack in error log when exception declared in method
            boolean logException = true;
            for (Class<?> clazz : method.getExceptionTypes()) {
                if (clazz.isInstance(response.getException().getCause())) {
                    logException = false;
                    defaultThrowExceptionStack = false;
                    break;
                }
            }
            if (logException) {
                log.error("Exception caught when during method invocation. request:" + request.toString(), e);
            } else {
                log.info("Exception caught when during method invocation. request:" + request.toString() + ", exception:" + response.getException().getCause().toString());
            }
        } catch (Throwable t) {
            // 如果服务发生Error，将Error转化为Exception，防止拖垮调用方
            if (t.getCause() != null) {
                response.setException(new RpcServiceException("provider has encountered a fatal error!", t.getCause()));
            } else {
                response.setException(new RpcServiceException("provider has encountered a fatal error!", t));
            }
            //对于Throwable,也记录日志
            log.error("Exception caught when during method invocation. request:" + request.toString(), t);
        }
        if (response.getException() != null) {
            //是否传输业务异常栈
            if (!this.url.getBooleanOption(TRANS_EXCEPTION_STACK, defaultThrowExceptionStack)) {
                // 不传输业务异常栈
                ExceptionUtils.setMockStackTrace(response.getException().getCause());
            }
        }
        // Copy options
        response.setOptions(request.getOptions());
        return response;
    }
}
