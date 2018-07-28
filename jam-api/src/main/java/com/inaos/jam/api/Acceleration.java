/*
 * Copyright 2018 INAOS GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inaos.jam.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Acceleration {

    String ARGUMENTS = "arguments", TRANSFORMED_ARGUMENTS = "transformed_arguments", RETURN = "return";

    Class<?> type();

    String method();

    Class<?>[] parameters();

    Library[] libraries() default {};

    boolean simpleEntry() default false;

    Class<?>[] inline() default {};

    String[] expectedNames() default {};

    String checksum() default "";

    Application application() default Application.ALL;

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

    enum Application {
        PRODUCTION, DEVELOPMENT, ALL
    }
}
