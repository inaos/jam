package com.inaos.iamj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Acceleration {

    String ARGUMENTS = "arguments", ORIGINAL_ARGUMENTS = "original_arguments", RETURN = "return";

    Class<?> type();

    String method();

    Class<?>[] parameters();

    Library[] libraries() default {};

    boolean simpleEntry() default false;

    @Target(value = {})
    @interface Library {

        Class<?> dispatcher();

        String binary();

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @interface Init {

        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @interface Destroy {

        }
    }
}
