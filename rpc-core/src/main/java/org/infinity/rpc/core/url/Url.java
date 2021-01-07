package org.infinity.rpc.core.url;

import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.utilities.network.NetworkUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Url used to represent a provider or client or registry
 */
@EqualsAndHashCode
public final class Url implements Serializable {
    private static final long    serialVersionUID   = 2970867582138131181L;
    /**
     * URL Pattern: {protocol}://{host}:{port}/{path}?{parameters}
     */
    private static final String  URL_PATTERN        = "{0}://{1}:{2}/{3}?{4}";
    private static final String  PROTOCOL_SEPARATOR = "://";
    public static final  String  PATH_SEPARATOR     = "/";
    /**
     * Url type
     * TODO: add
     */
    private              UrlType urlType;
    /**
     * RPC protocol
     */
    private              String  protocol;
    /**
     * RPC protocol version
     */
    private              String  version;
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

    // ◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘
    // Constants definitions
    private static final int    CLIENT_URL_PORT                        = 0;
    /**
     *
     */
    public static final  String PARAM_GROUP                            = "group";
    public static final  String PARAM_GROUP_PROVIDER                   = "provider";
    public static final  String PARAM_GROUP_APPLICATION                = "application";
    public static final  String PARAM_GROUP_APPLICATION_PROVIDER       = "application-provider";
    /**
     *
     */
    public static final  String PARAM_CHECK_HEALTH                     = "checkHealth";
    public static final  String PARAM_CHECK_HEALTH_DEFAULT_VALUE       = "true";
    /**
     *
     */
    public static final  String PARAM_CODEC                            = "codec";
    public static final  String PARAM_CODEC_DEFAULT_VALUE              = "infinity";
    /**
     *
     */
    public static final  String PARAM_TYPE                             = "type";
    public static final  String PARAM_TYPE_DEFAULT_VALUE               = "provider";
    /**
     *
     */
    public static final  String PARAM_CLUSTER                          = "cluster";
    public static final  String PARAM_CLUSTER_DEFAULT_VALUE            = "default";
    /**
     *
     */
    public static final  String PARAM_LOAD_BALANCER                    = "loadBalancer";
    public static final  String PARAM_LOAD_BALANCER_DEFAULT_VALUE      = "random";
    /**
     *
     */
    public static final  String PARAM_HA                               = "ha";
    public static final  String PARAM_HA_DEFAULT_VALUE                 = "failover";
    /**
     *
     */
    public static final  String PARAM_SERIALIZER                       = "serializer";
    public static final  String PARAM_SERIALIZER_DEFAULT_VALUE         = "hessian2";
    /**
     *
     */
    public static final  String PARAM_REQUEST_TIMEOUT                  = "requestTimeout";
    public static final  String PARAM_REQUEST_TIMEOUT_DEFAULT_VALUE    = "200";
    /**
     *
     */
    public static final  String PARAM_CONNECT_TIMEOUT                  = "connectTimeout";
    public static final  int    PARAM_CONNECT_TIMEOUT_DEFAULT_VALUE    = 1000;
    /**
     * pool max conn number
     */
    public static final  String PARAM_MAX_CONTENT_LENGTH               = "maxContentLength";
    public static final  int    PARAM_MAX_CONTENT_LENGTH_DEFAULT_VALUE = 10 * 1024 * 1024;


    public static final  String PARAM_ADDRESS                          = "address";
    public static final  String PARAM_SESSION_TIMEOUT                  = "sessionTimeout";
    public static final  String PARAM_MAX_RETRIES                      = "maxRetries";
    public static final  String PARAM_RETRY_INTERVAL                   = "retryInterval";
    public static final  String PARAM_APP                              = "app";
    public static final  String PARAM_ACTIVATED_TIME                   = "activatedTime";

    public static final String PARAM_MAX_SERVER_CONNECTION = "maxServerConnection";
    public static final String PARAM_MAX_CLIENT_CONNECTION = "maxClientConnection";
    public static final String PARAM_MAX_WORKER_THREAD     = "maxWorkerThread";
    public static final String PARAM_WORKER_QUEUE_SIZE     = "workerQueueSize";
    public static final String PARAM_HEART_BEAT_FACTORY    = "heartbeatFactory";

    /**
     * Extended parameters
     */
    private           Map<String, String> parameters;
    /**
     * non-transient fields can be used to generate equals() and hashcode() by lombok
     */
    private transient Map<String, Number> numbers;

    /**
     * Prohibit instantiate an instance outside the class
     */
    private Url() {
    }

    public static Url of(String protocol, String host, Integer port, String path, Map<String, String> parameters) {
        Url url = new Url();
        url.setProtocol(protocol);
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

    public static Url of(String protocol, String host, Integer port, String path) {
        return of(protocol, host, port, path, new HashMap<>());
    }

    public static Url of(String protocol, String host, Integer port) {
        return of(protocol, host, port, "", new HashMap<>());
    }

    public static Url providerUrl(String protocol, Integer port, String path) {
        return of(protocol, NetworkUtils.INTRANET_IP, port, path, new HashMap<>());
    }

    public static Url clientUrl(String protocol, String path) {
        return of(protocol, NetworkUtils.INTRANET_IP, CLIENT_URL_PORT, path, new HashMap<>());
    }

    public static Url registryUrl(String protocol, String host, Integer port) {
        return of(protocol, host, port, Registrable.class.getName(), new HashMap<>());
    }

    public String getProtocol() {
        return protocol;
    }

    // private access modifier
    private void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public String getHost() {
        return host;
    }

    // private access modifier
    private void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    // private access modifier
    private void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Composition of host + port
     *
     * @return
     */
    public String getAddress() {
        return host + ":" + port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    private void initialize() {
        parameters = new HashMap<>();
        parameters.put(PARAM_GROUP, PARAM_GROUP_PROVIDER);
    }

    private void checkIntegrity() {
        Validate.notNull(protocol, "Protocol must NOT be null!");
        Validate.notEmpty(host, "Host must NOT be empty!");
        Validate.notNull(port, "Port must NOT be null!");
    }

    private void checkValidity() {
    }

    public Url copy() {
        Map<String, String> params = new HashMap<String, String>();
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
        if (numbers == null) { // 允许并发重复创建
            numbers = new ConcurrentHashMap<String, Number>();
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
     * 返回一个service or referer的identity,如果两个url的identity相同，则表示相同的一个service或者referer
     *
     * @return
     */
    public String getIdentity() {
        return protocol + PROTOCOL_SEPARATOR + host + ":" + port +
                "/" + getParameter(Url.PARAM_TYPE, Url.PARAM_TYPE_DEFAULT_VALUE);
    }

    public String getGroup() {
        return getParameter(Url.PARAM_GROUP);
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
        int i = url.indexOf("?"); // separator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");

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
            if (i == 0) throw new RpcConfigurationException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) throw new RpcConfigurationException("url missing protocol: \"" + url + "\"");
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
        if (url.length() > 0) host = url;
        return of(protocol, host, port, path, parameters);
    }

    public String toFullStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(getUri()).append("?");

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

    public Integer getMethodParameter(String methodName, String paramDesc, String name, int defaultValue) {
        String key = methodName + "(" + paramDesc + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    public String getMethodParameter(String methodName, String paramDesc, String name) {
        String value = getParameter(RpcConstants.METHOD_CONFIG_PREFIX + methodName + "(" + paramDesc + ")." + name);
        if (value == null || value.length() == 0) {
            return getParameter(name);
        }
        return value;
    }
}
