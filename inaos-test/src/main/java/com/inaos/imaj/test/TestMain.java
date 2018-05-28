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

package com.inaos.imaj.test;

import java.lang.reflect.InvocationTargetException;

public class TestMain {

    public static void main(String[] args) {
        try {
            Class.forName(args[0]).getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t.getCause().printStackTrace();
            } else {
                t.printStackTrace();
            }
            System.exit(1);
        }
    }
}
