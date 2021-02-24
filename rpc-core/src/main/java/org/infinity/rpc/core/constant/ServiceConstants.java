package org.infinity.rpc.core.constant;

/**
 * All the attribute names of
 * {@link org.infinity.rpc.core.client.annotation.Consumer}
 * {@link org.infinity.rpc.core.server.annotation.Provider}
 */
public interface ServiceConstants {
    String INTERFACE_NAME  = "interfaceName";
    String INTERFACE_CLASS = "interfaceClass";
    String GENERIC         = "generic";

    String GROUP                            = "group";
    String GROUP_VAL_DEFAULT                = "default";
    String VERSION                          = "version";
    String VERSION_VAL_DEFAULT              = "1.0.0";
    String CHECK_HEALTH_FACTORY             = "checkHealthFactory";
    String CHECK_HEALTH_FACTORY_VAL_DEFAULT = "default";
    String MAX_RETRIES                      = "maxRetries";
    int    MAX_RETRIES_VAL_DEFAULT          = 0;
    String REQUEST_TIMEOUT                  = "requestTimeout";
    int    REQUEST_TIMEOUT_VAL_DEFAULT      = 500;
}
