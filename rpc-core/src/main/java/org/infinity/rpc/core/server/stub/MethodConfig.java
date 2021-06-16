package org.infinity.rpc.core.server.stub;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class MethodConfig implements Serializable {

    private static final long serialVersionUID = -2099139897676465243L;

    /**
     * RPC invocation timeout in milliseconds
     * Format: integer
     */
    private String requestTimeout;

    /**
     * The max retry times of RPC invocation
     * Format: integer
     */
    private String maxRetries;
}
