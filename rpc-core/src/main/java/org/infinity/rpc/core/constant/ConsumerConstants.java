package org.infinity.rpc.core.constant;

/**
 * All the attribute names of {@link org.infinity.rpc.core.client.annotation.Consumer}
 */
public interface ConsumerConstants extends ServiceConstants {
    String CLUSTER                      = "cluster";
    String CLUSTER_VAL_P2P              = "p2p";
    String CLUSTER_VAL_BROADCAST        = "broadcast";
    String FAULT_TOLERANCE              = "faultTolerance";
    String FAULT_TOLERANCE_VAL_FAILOVER = "failover";
    String FAULT_TOLERANCE_VAL_FAILFAST = "failfast";
    String LOAD_BALANCER                = "loadBalancer";
    String LOAD_BALANCER_VAL_RANDOM     = "random";
    String PROVIDER_ADDRESSES           = "providerAddresses";
    String LIMIT_RATE                   = "limitRate";
    String RATE_LIMITER_GUAVA           = "guava";
    String PROXY                        = "proxy";
    String PROXY_VAL_JDK                = "jdk";
    String PROXY_VAL_JAVASSIST          = "javassist";
}
