package org.infinity.rpc.core.constant;

public interface ProtocolConstants {

    String  PROTOCOL                           = "protocol";
    String  PROTOCOL_VAL_INFINITY              = "infinity";
    String  PROTOCOL_VAL_JVM                   = "jvm";
    String  CODEC                              = "codec";
    String  CODEC_VAL_DEFAULT                  = "default";
    String  SERIALIZER                         = "serializer";
    String  SERIALIZER_VAL_HESSIAN2            = "hessian2";
    String  SERIALIZER_VAL_KRYO                = "kryo";
    String  SERIALIZER_VAL_DEFAULT             = SERIALIZER_VAL_HESSIAN2;
    int     SERIALIZER_HESSIAN2_TYPE_NUM       = 1;
    int     SERIALIZER_KRYO_TYPE_NUM           = 2;
    String  LOCAL_ADDRESS_FACTORY              = "localAddressFactory";
    String  LOCAL_ADDRESS_FACTORY_VAL_DEFAULT  = "default";
    String  ENDPOINT_FACTORY                   = "endpointFactory";
    String  ENDPOINT_FACTORY_VAL_NETTY         = "netty";
    String  MIN_CLIENT_CONN                    = "minClientConn";
    int     MIN_CLIENT_CONN_VAL_DEFAULT        = 2;
    String  MAX_CLIENT_FAILED_CONN             = "maxClientFailedConn";
    int     MAX_CLIENT_FAILED_CONN_VAL_DEFAULT = 10;
    String  MAX_SERVER_CONN                    = "maxServerConn";
    int     MAX_SERVER_CONN_VAL_DEFAULT        = 100000;
    String  MAX_CONTENT_LENGTH                 = "maxContentLength";
    int     MAX_CONTENT_LENGTH_VAL_DEFAULT     = 10 * 1024 * 1024; // 10M
    String  MIN_THREAD                         = "minThread";
    int     MIN_THREAD_VAL_DEFAULT             = 20;
    int     MIN_THREAD_SHARED_CHANNEL          = 40;
    String  MAX_THREAD                         = "maxThread";
    int     MAX_THREAD_VAL_DEFAULT             = 200;
    int     MAX_THREAD_SHARED_CHANNEL          = 800;
    String  WORK_QUEUE_SIZE                    = "workQueueSize";
    int     WORK_QUEUE_SIZE_VAL_DEFAULT        = 0;
    String  SHARED_CHANNEL                     = "sharedChannel";
    boolean SHARED_CHANNEL_VAL_DEFAULT         = true;
    String  ASYNC_INIT_CONN                    = "asyncInitConn";
    boolean ASYNC_INIT_CONN_VAL_DEFAULT        = false;
    String  THROW_EXCEPTION                    = "throwException";
    boolean THROW_EXCEPTION_VAL_DEFAULT        = true;
    String  TRANS_EXCEPTION_STACK              = "transExceptionStack";
    boolean TRANS_EXCEPTION_STACK_VAL_DEFAULT  = true;
}
