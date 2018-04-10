package com.inaos.iamj.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Acceleration {

    Class<?> type();

    String method();

    Class<?>[] parameters();

    Library[] libraries() default {};

    @interface Library {

        Class<?> dispatcher();

        String binary();
    }
}
