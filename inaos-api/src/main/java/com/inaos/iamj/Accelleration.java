package com.inaos.iamj;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Accelleration {

    Class<?> type();

    String method();

    Class<?>[] parameters();

    Library[] libraries() default {};

    @interface Library {

        Class<?> dispatcher();

        String binary();
    }
}
