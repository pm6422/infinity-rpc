package org.infinity.rpc.core.client.annotation;

import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC consumer annotation
 * Please refer {@link ServiceConstants} for the property definition
 * <p>
 * This class can annotate non-static field, non-static public method with prefix 'set' name and one parameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Consumer {
    /**
     * @return interface class of RPC consumer class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * The interface class fully-qualified name of RPC consumer class
     * For the generic consumer instance must specify interfaceName attribute
     *
     * @return fully-qualified class name
     */
    String interfaceName() default "";

    /**
     * Available values: [infinity, zookeeper, local, direct, injvm]
     *
     * @return protocol
     */
    String protocol() default "";

    /**
     * Available values: [zookeeper]
     * @return registry
     */
//    String registry() default "";

    /**
     * @return provider invoke cluster
     */
    String cluster() default "";

    /**
     * @return fault tolerance
     */
    String faultTolerance() default "";

    /**
     * @return load balancer
     */
    String loadBalancer() default "";

    /**
     * One service interface may have multiple implementations(forms),
     * It used to distinguish between different implementations of service provider interface
     *
     * @return group
     */
    String form() default "";

    /**
     * When the service changes, such as adding or deleting methods, and interface parameters change,
     * the provider and consumer application instances need to be upgraded.
     * In order to deploy in a production environment without affecting user use,
     * a gradual migration scheme is generally adopted.
     * First upgrade some provider application instances,
     * and then use the same version number to upgrade some consumer instances.
     * The old version of the consumer instance calls the old version of the provider instance.
     * Observe that there is no problem and repeat this process to complete the upgrade.
     *
     * @return version
     */
    String version() default "";

    /**
     * @return consumer proxy factory used to create proxyInstance which is the implementation of consumer interface class
     */
    String proxyFactory() default "";

    /**
     * @return health checker
     */
    String healthChecker() default "";

    /**
     * @return Timeout value for service invocation
     */
    int requestTimeout() default Integer.MAX_VALUE;

    /**
     * @return The max retry times of RPC request
     */
    int maxRetries() default Integer.MAX_VALUE;

    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     *
     * @return direct urls, e.g. 127.0.0.1:26010,192.168.120.111:26010
     */
    String directAddresses() default "";
}