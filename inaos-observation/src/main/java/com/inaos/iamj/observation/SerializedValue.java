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

package com.inaos.iamj.observation;

import java.io.Serializable;

public class SerializedValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] types;

    private final byte[] arguments;

    public SerializedValue(String[] types, byte[] arguments) {
        this.types = types;
        this.arguments = arguments;
    }

    public String[] getTypes() {
        return types;
    }

    public byte[] getArguments() {
        return arguments;
    }

    public Class<?>[] resolveTypes(ClassLoader classLoader) {
        Class<?>[] types = new Class<?>[this.types.length];
        int index = 0;
        try {
            for (String type : this.types) {
                types[index++] = forName(type, classLoader);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return types;
    }

    private static Class<?> forName(String type, ClassLoader classLoader) throws ClassNotFoundException {
        if (void.class.toString().equals(type)) return void.class;
        if (boolean.class.toString().equals(type)) return boolean.class;
        if (byte.class.toString().equals(type)) return byte.class;
        if (short.class.toString().equals(type)) return short.class;
        if (char.class.toString().equals(type)) return char.class;
        if (int.class.toString().equals(type)) return int.class;
        if (long.class.toString().equals(type)) return long.class;
        if (float.class.toString().equals(type)) return float.class;
        if (double.class.toString().equals(type)) return double.class;
        return Class.forName(type, false, classLoader);
    }
}
