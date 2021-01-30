package org.infinity.rpc.core.url;

import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.utilities.network.NetworkUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * Url used to represent a provider or client or registry
 */
@Data
public final class Url implements Serializable {
    private static final long    serialVersionUID   = 2970867582138131181L;
    /**
     * URL Pattern: {protocol}://{host}:{port}/{path}?{parameters}
     */
    private static final String  URL_PATTERN        = "{0}://{1}:{2}/{3}?{4}";
    private static final String  PROTOCOL_SEPARATOR = "://";
    public static final  String  PATH_SEPARATOR     = "/";
    private static final int     CLIENT_URL_PORT    = 0;
    /**
     * RPC protocol
     */
    private              String  protocol;
    /**
     * RPC server or client host name
     */
    private              String  host;
    /**
     * RPC server or client port
     */
    private              Integer port;
    /**
     * RPC interface fully-qualified name
     */
    private              String  path;
    /**
     * Service provider group
     */
    private              String  group;
    /**
     * RPC protocol version
     */
    private              String  version;

    // ◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘
    // Constants definitions
    public static final String  PARAM_GROUP_APPLICATION                   = "application";
    public static final String  PARAM_GROUP_APPLICATION_PROVIDER          = "application-provider";
    /**
     *
     */
    public static final String  PARAM_CODEC                               = "codec";
    public static final String  PARAM_CODEC_DEFAULT_VALUE                 = "default";
    /**
     *
     */
    public static final String  PARAM_TYPE                                = "type";
    public static final String  PARAM_TYPE_PROVIDER                       = "provider";
    public static final String  PARAM_TYPE_CONSUMER                       = "consumer";
    public static final String  PARAM_TYPE_REGISTRY                       = "registry";
    /**
     *
     */
    public static final String  PARAM_CLUSTER                             = "cluster";
    public static final String  PARAM_CLUSTER_DEFAULT_VALUE               = "default";
    /**
     *
     */
    public static final String  PARAM_LOAD_BALANCER                       = "loadBalancer";
    public static final String  PARAM_LOAD_BALANCER_DEFAULT_VALUE         = "random";
    /**
     *
     */
    public static final String  PARAM_HA                                  = "ha";
    public static final String  PARAM_HA_DEFAULT_VALUE                    = "failover";
    /**
     *
     */
    public static final String  PARAM_SERIALIZER                          = "serializer";
    public static final String  PARAM_SERIALIZER_DEFAULT_VALUE            = "hessian2";
    /**
     *
     */
    public static final String  PARAM_MAX_RETRIES                         = "maxRetries";
    /**
     *
     */
    public static final String  PARAM_REQUEST_TIMEOUT                     = "requestTimeout";
    /**
     *
     */
    public static final String  PARAM_CONNECT_TIMEOUT                     = "connectTimeout";
    public static final int     PARAM_CONNECT_TIMEOUT_DEFAULT_VALUE       = 1000;
    /**
     * pool max conn number
     */
    public static final String  PARAM_MAX_CONTENT_LENGTH                  = "maxContentLength";
    public static final int     PARAM_MAX_CONTENT_LENGTH_DEFAULT_VALUE    = 10 * 1024 * 1024;
    /**
     * multi consumers share the same channel
     */
    public static final String  PARAM_SHARE_CHANNEL                       = "shareChannel";
    public static final boolean PARAM_SHARE_CHANNEL_DEFAULT_VALUE         = true;
    /**
     * thread pool minimum connection number
     */
    public static final String  PARAM_MIN_CLIENT_CONNECTION               = "minClientConnection";
    public static final int     PARAM_MIN_CLIENT_CONNECTION_DEFAULT_VALUE = 2;
    /**
     * thread pool maximum connection number
     */
    public static final String  PARAM_MAX_CLIENT_CONNECTION               = "maxClientConnection";
    public static final int     PARAM_MAX_CLIENT_CONNECTION_DEFAULT_VALUE = 10;
    /**
     * max server conn (all clients conn)
     */
    public static final String  PARAM_MAX_SERVER_CONNECTION               = "maxServerConnection";
    public static final int     PARAM_MAX_SERVER_CONNECTION_DEFAULT_VALUE = 100000;
    /**
     *
     */
    public static final String  PARAM_WORKER_QUEUE_SIZE                   = "workerQueueSize";
    public static final int     PARAM_WORKER_QUEUE_SIZE_DEFAULT_VALUE     = 0;
    /**
     * service min worker threads
     */
    public static final String  PARAM_MIN_WORKER_THREAD                   = "minWorkerThread";
    public static final int     PARAM_MIN_WORKER_THREAD_DEFAULT_VALUE     = 20;
    /**
     * service max worker threads
     */
    public static final String  PARAM_MAX_WORKER_THREAD                   = "maxWorkerThread";
    public static final int     PARAM_MAX_WORKER_THREAD_DEFAULT_VALUE     = 200;
    /**
     *
     */
    public static final String  PARAM_HOST                                = "host";
    public static final String  PARAM_HOST_DEFAULT_VALUE                  = "";
    /**
     *
     */
    public static final String  PARAM_ENDPOINT_FACTORY                    = "endpointFactory";
    public static final String  PARAM_ENDPOINT_FACTORY_DEFAULT_VALUE      = "netty";
    /**
     *
     */
    public static final String  PARAM_TRANS_EXCEPTION_STACK               = "transExceptionStack";
    public static final boolean PARAM_TRANS_EXCEPTION_STACK_DEFAULT_VALUE = true;
    /**
     *
     */
    public static final String  PARAM_ASYNC_INIT_CONNECTION               = "asyncInitConnection";
    public static final boolean PARAM_ASYNC_INIT_CONNECTION_DEFAULT_VALUE = false;
    /**
     *
     */
    public static final String  PARAM_THROW_EXCEPTION                     = "throwException";
    public static final boolean PARAM_THROW_EXCEPTION_DEFAULT_VALUE       = true;
    /**
     *
     */
    public static final String  PARAM_WEIGHT                              = "weights";
    public static final String  PARAM_WEIGHT_DEFAULT_VALUE                = "";

    public static final String PARAM_ADDRESS         = "address";
    public static final String PARAM_SESSION_TIMEOUT = "sessionTimeout";
    public static final String PARAM_RETRY_INTERVAL  = "retryInterval";
    public static final String PARAM_APP             = "app";
    public static final String PARAM_APP_UNKNOWN     = "unknown";
    public static final String PARAM_ACTIVATED_TIME  = "activatedTime";

    /**
     * Extended parameters
     */
    private           Map<String, String> parameters;
    /**
     * non-transient fields can be used to generate equals() and hashcode() by lombok
     */
    private transient Map<String, Number> numbers;

    /**
     * Prevent instantiation of it outside the class
     */
    private Url() {
    }

    public static Url of(String protocol, String host, Integer port, String path, String group, String version,
                         Map<String, String> parameters) {
        Url url = new Url();
        url.setProtocol(protocol);
        url.setGroup(group);
        url.setVersion(version);
        url.setHost(host);
        url.setPort(port);
        url.setPath(path);

        // initialize fields with init values
        url.initialize();
        url.addParameters(parameters);
        url.checkIntegrity();
        url.checkValidity();
        return url;
    }

    public static Url of(String protocol, String host, Integer port, String path, Map<String, String> parameters) {
        return of(protocol, host, port, path, GROUP_DEFAULT_VALUE, VERSION_DEFAULT_VALUE, parameters);
    }

    public static Url of(String protocol, String host, Integer port, String path) {
        return of(protocol, host, port, path, GROUP_DEFAULT_VALUE, VERSION_DEFAULT_VALUE, new HashMap<>());
    }

    public static Url providerUrl(String protocol, String host, Integer port, String path, String group, String version) {
        Url url = of(protocol, host, port, path, group, version, new HashMap<>());
        url.addParameter(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
        return url;
    }

    /**
     * The consumer url used to export to registry only for consumers discovery management,
     * but it have nothing to do with the service calling.
     *
     * @param protocol
     * @param port
     * @param path
     * @param group
     * @param version
     * @return
     */
    public static Url consumerUrl(String protocol, Integer port, String path, String group, String version) {
        // todo: check internet or intranet ip
        Url url = of(protocol, NetworkUtils.INTRANET_IP, port, path, group, version, new HashMap<>());
        url.addParameter(Url.PARAM_TYPE, Url.PARAM_TYPE_CONSUMER);
        return url;
    }

    public static Url clientUrl(String protocol, String path) {
        return of(protocol, NetworkUtils.INTRANET_IP, CLIENT_URL_PORT, path, GROUP_DEFAULT_VALUE, VERSION_DEFAULT_VALUE, new HashMap<>());
    }

    /**
     * Create a register url
     *
     * @param protocol registry name
     * @param host     registry host
     * @param port     registry port
     * @return registry url
     */
    public static Url registryUrl(String protocol, String host, Integer port) {
        Url url = of(protocol, host, port, Registry.class.getName(), GROUP_DEFAULT_VALUE, VERSION_DEFAULT_VALUE, new HashMap<>());
        url.addParameter(Url.PARAM_TYPE, Url.PARAM_TYPE_REGISTRY);
        return url;
    }


    /**
     * Composition of host + port
     *
     * @return address
     */
    public String getAddress() {
        return host + ":" + port;
    }

    private void initialize() {
        parameters = new HashMap<>();
    }

    private void checkIntegrity() {
        Validate.notEmpty(protocol, "Protocol must NOT be empty!");
        Validate.notEmpty(host, "Host must NOT be empty!");
        Validate.notNull(port, "Port must NOT be null!");
        Validate.notEmpty(group, "Group must NOT be empty!");
        Validate.notEmpty(version, "Version must NOT be empty!");
    }

    private void checkValidity() {
    }

    public Url copy() {
        Map<String, String> params = new HashMap<>();
        if (this.parameters != null) {
            params.putAll(this.parameters);
        }
        return of(protocol, host, port, path, params);
    }

    public String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Integer getIntParameter(String name, int defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(name, i);
        return i;
    }

    public Integer getIntParameter(String name) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = parameters.get(name);
        int i = Integer.parseInt(value);
        getNumbers().put(name, i);
        return i;
    }

    private Map<String, Number> getNumbers() {
        if (numbers == null) {
            // 允许并发重复创建
            numbers = new ConcurrentHashMap<>();
        }
        return numbers;
    }

    public Boolean getBooleanParameter(String name, boolean defaultValue) {
        Boolean value = getBooleanParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public Boolean getBooleanParameter(String name) {
        String value = parameters.get(name);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private void addParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    /**
     * 返回identity string,如果两个url的identity相同，则表示相同的一个service或者consumer
     *
     * @return identity
     */
    public String getIdentity() {
        if (PARAM_TYPE_REGISTRY.equals(getParameter(PARAM_TYPE))) {
            return protocol + PROTOCOL_SEPARATOR + host + ":" + port;
        }
        return protocol + PROTOCOL_SEPARATOR + host + ":" + port
                + "/" + getParameter(GROUP, GROUP_DEFAULT_VALUE)
                + "/" + getPath()
                + "/" + getParameter(VERSION, VERSION_DEFAULT_VALUE)
                + "/" + getParameter(PARAM_TYPE, PARAM_TYPE_PROVIDER);
    }

    public String getServerPortStr() {
        return buildHostPortStr(host, port);
    }

    private static String buildHostPortStr(String host, int defaultPort) {
        if (defaultPort <= 0) {
            return host;
        }

        int idx = host.indexOf(":");
        if (idx < 0) {
            return host + ":" + defaultPort;
        }

        int port = Integer.parseInt(host.substring(idx + 1));
        if (port <= 0) {
            return host.substring(0, idx + 1) + defaultPort;
        }
        return host;
    }

    public static Url valueOf(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url is null");
        }
        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = new HashMap<>();
        // separator between body and parameters
        int i = url.indexOf("?");
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("&");

            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new RpcConfigurationException("url missing protocol: \"" + url + "\"");
            }
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new RpcConfigurationException("url missing protocol: \"" + url + "\"");
                }
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }

        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) {
            host = url;
        }
        return of(protocol, host, port, path, parameters);
    }

    public String toFullStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(getUri());
        if (MapUtils.isNotEmpty(parameters)) {
            builder.append("?");
        }
        Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String name = entry.getKey();
            String value = entry.getValue();
            builder.append(name).append("=").append(value);
            if (iterator.hasNext()) {
                builder.append("&");
            }
        }
        return builder.toString();
    }

    public String getUri() {
        return protocol + PROTOCOL_SEPARATOR + host + ":" + port + PATH_SEPARATOR + path;
    }

    /**
     * Including protocol, host, port, group
     *
     * @return combination string
     */
    public String toSimpleString() {
        return getUri() + "?group=" + getGroup();
    }

    @Override
    public String toString() {
        return toSimpleString();
    }

    public void addParameter(String name, String value) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
            return;
        }
        parameters.put(name, value);
    }

    /**
     * Get method level parameter value
     *
     * @param methodName       method name
     * @param methodParameters method parameter class name list string. e.g, java.util.List,java.lang.Long
     * @param name             parameter name
     * @param defaultValue     parameter default value
     * @return value
     */
    public Integer getMethodParameter(String methodName, String methodParameters, String name, int defaultValue) {
        String key = methodName + "(" + methodParameters + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(methodName, methodParameters, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    public String getMethodParameter(String methodName, String methodParameters, String name) {
        String value = getParameter(RpcConstants.METHOD_CONFIG_PREFIX + methodName + "(" + methodParameters + ")." + name);
        if (value == null || value.length() == 0) {
            return getParameter(name);
        }
        return value;
    }
}
