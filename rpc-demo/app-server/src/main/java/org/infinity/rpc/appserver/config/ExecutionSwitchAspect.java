package org.infinity.rpc.appserver.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.infinity.rpc.appserver.annotation.ExecutionSwitch;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Pointcut configuration
 */
@Aspect
@Configuration
public class ExecutionSwitchAspect {

    private final Environment env;

    public ExecutionSwitchAspect(Environment env) {
        this.env = env;
    }

    @Around("@annotation(executionSwitch)")
    public Object switchAround(ProceedingJoinPoint joinPoint, ExecutionSwitch executionSwitch) throws Throwable {
        if ("true".equals(env.getProperty(executionSwitch.on()))) {
            // Proceed to execute method
            return joinPoint.proceed();
        }
        return null;
    }
}
