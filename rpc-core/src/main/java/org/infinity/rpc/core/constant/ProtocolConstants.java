package org.infinity.rpc.core.constant;

public interface ProtocolConstants {

    String PROTOCOL                             = "protocol";
    String PROTOCOL_DEFAULT_VALUE               = "infinity";
    String CODEC                                = "codec";
    String CODEC_DEFAULT_VALUE                  = "default";
    String SERIALIZER                           = "serializer";
    String SERIALIZER_DEFAULT_VALUE             = "hessian2";
    String LOCAL_ADDRESS_FACTORY                = "localAddressFactory";
    String LOCAL_ADDRESS_FACTORY_DEFAULT_VALUE  = "default";
    String MIN_CLIENT_CONN                      = "minClientConn";
    int    MIN_CLIENT_CONN_DEFAULT_VALUE        = 2;
    String MAX_CLIENT_FAILED_CONN               = "maxClientFailedConn";
    int    MAX_CLIENT_FAILED_CONN_DEFAULT_VALUE = 10;
    String MAX_SERVER_CONN                      = "maxServerConn";
    int    MAX_SERVER_CONN_DEFAULT_VALUE        = 100000;
    String MAX_CONTENT_LENGTH                   = "maxContentLength";
    int    MAX_CONTENT_LENGTH_DEFAULT_VALUE     = 10 * 1024 * 1024; // 10M
}
