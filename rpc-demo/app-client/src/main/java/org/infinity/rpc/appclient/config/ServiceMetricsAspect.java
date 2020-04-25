package org.infinity.rpc.appclient.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

import java.util.Arrays;

/**
 * Aspect for logging execution of service Spring components.
 */
@Aspect
@ConditionalOnProperty(prefix = "application.service-metrics", value = "enable", havingValue = "true")
@Configuration
public class ServiceMetricsAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMetricsAspect.class);

    @Around("within(" + ApplicationConstants.BASE_PACKAGE + ".service.impl.*)")
    public Object serviceAround(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            long threshold = 10L;
            if (elapsed > threshold) {
                LOGGER.warn("@@@@@@@@@@@@@@@@@@@@@ Exit: {}.{}() with {} ms @@@@@@@@@@@@@@@@@@@@@",
                        joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName(), elapsed);
            }
            return result;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Illegal argument: {} in {}.{}()", Arrays.toString(joinPoint.getArgs()),
                    joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());

            throw e;
        }
    }
}
