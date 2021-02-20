package org.infinity.rpc.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.constant.ServiceConstants;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.infinity.rpc.core.url.Url;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.CHECK_HEALTH_FACTORY;

public class RpcFrameworkUtils {

    public static String getGroupFromRequest(Requestable request) {
        return getValueFromRequest(request, ServiceConstants.GROUP, ServiceConstants.GROUP_DEFAULT_VALUE);
    }

    public static String getVersionFromRequest(Requestable request) {
        return getValueFromRequest(request, ServiceConstants.VERSION, ServiceConstants.VERSION_DEFAULT_VALUE);
    }

    public static String getValueFromRequest(Requestable request, String key, String defaultValue) {
        String value = defaultValue;
        if (request.getOptions() != null && request.getOptions().containsKey(key)) {
            value = request.getOption(key);
        }
        return value;
    }


    /**
     * 目前根据 group/interface/version 来唯一标示一个服务
     *
     * @param request
     * @return
     */

    public static String getServiceKey(Requestable request) {
        String version = getVersionFromRequest(request);
        String group = getGroupFromRequest(request);
        return getServiceKey(group, request.getInterfaceName(), version);
    }

    /**
     * 目前根据 group/interface/version 来唯一标示一个服务
     *
     * @param url
     * @return
     */
    public static String getServiceKey(Url url) {
        return getServiceKey(url.getGroup(), url.getPath(), url.getVersion());
    }

    /**
     * protocol key: protocol://host:port/group/interface/version
     *
     * @param url
     * @return
     */
    public static String getProtocolKey(Url url) {
        return url.getProtocol() + RpcConstants.PROTOCOL_SEPARATOR + url.getAddress() + RpcConstants.PATH_SEPARATOR
                + url.getGroup() + RpcConstants.PATH_SEPARATOR + url.getPath() + RpcConstants.PATH_SEPARATOR + url.getVersion();
    }

    /**
     * 根据Request得到 interface.method(paramDesc) 的 desc
     * <p>
     * <pre>
     * 		比如：
     * 			package com.weibo.api.motan;
     *
     * 		 	interface A { public hello(int age); }
     *
     * 			那么return "com.weibo.api.motan.A.hell(int)"
     * </pre>
     *
     * @param request
     * @return
     */
    public static String getFullMethodString(Requestable request) {
        return request.getInterfaceName() + "." + request.getMethodName() + "("
                + request.getMethodParameters() + ")";
    }

    public static String getGroupMethodString(Requestable request) {
        return getGroupFromRequest(request) + "_" + getFullMethodString(request);
    }


    /**
     * 判断url:source和url:target是否可以使用共享的service channel(port) 对外提供服务
     * <p>
     * <pre>
     * 		1） protocol
     * 		2） codec
     * 		3） serialize
     * 		4） maxContentLength
     * 		5） maxServerConnection
     * 		6） maxWorkerThread
     * 		7） workerQueueSize
     * 		8） heartbeatFactory
     * </pre>
     *
     * @param source
     * @param target
     * @return
     */
    public static boolean checkIfCanShareServiceChannel(Url source, Url target) {
        if (!StringUtils.equals(source.getProtocol(), target.getProtocol())) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(CODEC), target.getOption(CODEC))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(SERIALIZER), target.getOption(SERIALIZER_DEFAULT_VALUE))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(Url.PARAM_MAX_CONTENT_LENGTH), target.getOption(Url.PARAM_MAX_CONTENT_LENGTH))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(MAX_SERVER_CONN), target.getOption(MAX_SERVER_CONN))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(Url.PARAM_MAX_WORKER_THREAD), target.getOption(Url.PARAM_MAX_WORKER_THREAD))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(Url.PARAM_WORKER_QUEUE_SIZE), target.getOption(Url.PARAM_WORKER_QUEUE_SIZE))) {
            return false;
        }

        return StringUtils.equals(source.getOption(CHECK_HEALTH_FACTORY), target.getOption(CHECK_HEALTH_FACTORY));
    }

    /**
     * 判断url:source和url:target是否可以使用共享的client channel(port) 对外提供服务
     * <p>
     * <pre>
     * 		1） protocol
     * 		2） codec
     * 		3） serialize
     * 		4） maxContentLength
     * 		5） maxClientConnection
     * 		6） heartbeatFactory
     * </pre>
     *
     * @param source
     * @param target
     * @return
     */
    public static boolean checkIfCanShallClientChannel(Url source, Url target) {
        if (!StringUtils.equals(source.getProtocol(), target.getProtocol())) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(CODEC), target.getOption(CODEC))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(SERIALIZER), target.getOption(SERIALIZER))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(Url.PARAM_MAX_CONTENT_LENGTH), target.getOption(Url.PARAM_MAX_CONTENT_LENGTH))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(MAX_CLIENT_CONN), target.getOption(MAX_CLIENT_CONN))) {
            return false;
        }

        return StringUtils.equals(source.getOption(CHECK_HEALTH_FACTORY), target.getOption(CHECK_HEALTH_FACTORY));

    }

    /**
     * serviceKey: group/interface/version
     *
     * @param group
     * @param interfaceName
     * @param version
     * @return
     */
    private static String getServiceKey(String group, String interfaceName, String version) {
        return group + RpcConstants.PATH_SEPARATOR + interfaceName + RpcConstants.PATH_SEPARATOR + version;
    }

    /**
     * 获取默认motan协议配置
     *
     * @return motan协议配置
     */
//    public static ProtocolConfig getDefaultProtocolConfig() {
//        ProtocolConfig pc = new ProtocolConfig();
//        pc.setId("motan");
//        pc.setName("motan");
//        return pc;
//    }

    /**
     * 默认本地注册中心
     *
     * @return local registry
     */
//    public static RegistryConfig getDefaultRegistryConfig() {
//        RegistryConfig local = new RegistryConfig();
//        local.setRegProtocol("local");
//        return local;
//    }
    public static String removeAsyncSuffix(String path) {
        if (path != null && path.endsWith(RpcConstants.ASYNC_SUFFIX)) {
            return path.substring(0, path.length() - RpcConstants.ASYNC_SUFFIX.length());
        }
        return path;
    }

    public static RpcResponse buildErrorResponse(Requestable request, Exception e) {
        return buildErrorResponse(request.getRequestId(), request.getProtocolVersion(), e);
    }

    public static RpcResponse buildErrorResponse(long requestId, byte version, Exception e) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setProtocolVersion(version);
        response.setException(e);
        return response;
    }

    public static void logEvent(Requestable request, String event) {
        if (SwitcherService.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            logEvent(request, event, System.currentTimeMillis());
        }
    }

    public static void logEvent(Requestable request, String event, long time) {
        if (request == null) {
            return;
        }
        if (RpcConstants.TRACE_CSEND.equals(event)) {
            request.setSendingTime(time);
            return;
        }
        if (RpcConstants.TRACE_SRECEIVE.equals(event)) {
            request.setReceivedTime(time);
            return;
        }
        if (SwitcherService.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            request.addTrace(event, String.valueOf(time));
        }
    }

    public static void logEvent(Responseable response, String event) {
        if (SwitcherService.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            logEvent(response, event, System.currentTimeMillis());
        }
    }

    public static void logEvent(Responseable response, String event, long time) {
        if (response == null) {
            return;
        }
        if (RpcConstants.TRACE_SSEND.equals(event)) {
            response.setSendingTime(time);
            return;
        }
        if (RpcConstants.TRACE_CRECEIVE.equals(event)) {
            response.setReceivedTime(time);
            return;
        }
        if (SwitcherService.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            response.addTrace(event, String.valueOf(time));
        }
    }
}
