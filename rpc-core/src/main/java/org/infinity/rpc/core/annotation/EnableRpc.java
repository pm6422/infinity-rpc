package org.infinity.rpc.core.annotation;

import org.infinity.rpc.core.registrar.RpcConsumerProviderRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcConsumerProviderRegistrar.class)
public @interface EnableRpc {

    String[] scanBasePackages() default {};
}
