package org.infinity.luix.core.server.buildin;

import org.infinity.luix.core.config.impl.ApplicationConfig;

public interface BuildInService {
    String METHOD_GET_APPLICATION_INFO = "getApplicationInfo";
    String METHOD_GET_SERVER_INFO      = "getServerInfo";

    /**
     * Get application information
     *
     * @return application information
     */
    ApplicationConfig getApplicationInfo();

    /**
     * Get server information
     *
     * @return server information
     */
    ServerInfo getServerInfo();
}
