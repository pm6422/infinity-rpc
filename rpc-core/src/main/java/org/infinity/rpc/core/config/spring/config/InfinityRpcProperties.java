package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.infinity.rpc.core.registry.Protocol;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "spring.infinity-rpc")
@Data
public class InfinityRpcProperties implements InitializingBean {
    public static final Pattern  COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    private             Registry registry;
    private             Protocol protocol;

    @Override
    public void afterPropertiesSet() throws Exception {
        checkIntegrity();
        checkValidity();
        assignValues();
    }

    private void checkIntegrity() {
        // todo
    }

    private void checkValidity() {
        // todo
    }

    private void assignValues() {
        if (!StringUtils.isEmpty(registry.getAddress()) && StringUtils.isEmpty(registry.getHost()) && registry.getPort() == null) {
            String[] splitParts = COLON_SPLIT_PATTERN.split(registry.getAddress());
            registry.setHost(splitParts[0]);
            registry.setPort(Integer.parseInt(splitParts[1]));
        }
    }

    @Data
    public static class Registry {
        // Protocol for register center
        private Protocol protocol;
        // Registry center server address
        private String   address;
        // Registry center host name
        private String   host;
        // Registry center port number
        private Integer  port;
    }

//    @Data
//    public static class Protocol {
//        // RPC server port
//        private int port;
//    }
}
