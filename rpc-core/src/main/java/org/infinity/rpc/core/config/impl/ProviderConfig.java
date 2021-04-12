package org.infinity.rpc.core.config.impl;

import lombok.Data;

@Data
public class ProviderConfig extends ServiceConfig {
    public static final String  PREFIX     = "provider";
    /**
     * Indicates whether all the providers were exposed to registry automatically
     */
    private             boolean autoExpose = true;

    public void init() {
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }
}
