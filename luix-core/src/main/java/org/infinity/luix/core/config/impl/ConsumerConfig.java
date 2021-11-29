package org.infinity.luix.core.config.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.ratelimit.RateLimiter;

import javax.validation.constraints.NotEmpty;

import static org.infinity.luix.core.constant.ConsumerConstants.*;

@Data
@Slf4j
public class ConsumerConfig extends ServiceConfig {
    public static final String  PREFIX           = "consumer";
    /**
     * Service provider invoker
     */
    @NotEmpty
    private             String  invoker          = INVOKER_VAL_DEFAULT;
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private             String  faultTolerance   = FAULT_TOLERANCE_VAL_DEFAULT;
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private             String  loadBalancer     = LOAD_BALANCER_VAL_DEFAULT;
    /**
     * Consumer proxy factory
     */
    @NotEmpty
    private             String  proxyFactory     = PROXY_VAL_DEFAULT;
    /**
     * Indicates whether rate limit enabled or not
     */
    private             boolean limitRate;
    /**
     * Permits per second of rate limit
     */
    private             long    permitsPerSecond = 100L;

    public void init() {
        checkIntegrity();
        checkValidity();
        initRateLimiter();
        log.info("Luix consumer configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }

    private void initRateLimiter() {
        RateLimiter.getInstance(RATE_LIMITER_GUAVA).create(permitsPerSecond);
    }
}
