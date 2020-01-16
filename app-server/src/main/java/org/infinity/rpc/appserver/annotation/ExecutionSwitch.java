package org.infinity.rpc.appserver.annotation;

import org.infinity.rpc.appserver.config.ApplicationConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecutionSwitch {

    public static final String AROUND = "@annotation(" + ApplicationConstants.BASE_PACKAGE
            + ".annotation.ExecutionSwitch)";

    /**
     * @return
     */
    String on();
}