package com.inaos.iamj;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Accelleration {

    Class<?> type();

    String method();

    Class<?>[] parameters();

    String[] libraries() default {};

    Class<?>[] dispatchers() default {};
}
