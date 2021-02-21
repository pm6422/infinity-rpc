package org.infinity.rpc.core.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

@Data
public class ServiceConfig {
    /**
     * Group
     */
    @NotEmpty
    private String group              = GROUP_DEFAULT_VALUE;
    /**
     * Version
     */
    @NotEmpty
    private String version            = VERSION_DEFAULT_VALUE;
    /**
     * Check health factory
     */
    @NotEmpty
    private String checkHealthFactory = CHECK_HEALTH_FACTORY_DEFAULT_VALUE;
    /**
     * Timeout in milliseconds for handling request between client and server sides
     */
    private int    requestTimeout     = REQUEST_TIMEOUT_DEFAULT_VALUE;
    /**
     * Max retry count after calling failure
     */
    private int    maxRetries         = MAX_RETRIES_DEFAULT_VALUE;

}
