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

package com.inaos.jam.observation.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.inaos.jam.observation.SerializedValue;

import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;

public class KryoSerializer {

    private final Kryo kryo;

    public KryoSerializer() {
        kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public KryoSerializer(Kryo kryo) {
        this.kryo = kryo;
    }

    public SerializedValue make(Class<?> type, Object argument) {
        if (type == void.class) {
            return null;
        }
        String[] serializedTypes = {type.getName()};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Output out = new Output(bytes);
        kryo.writeObject(out, 1);
        kryo.writeClassAndObject(out, argument);
        out.close();
        return new SerializedValue(serializedTypes, bytes.toByteArray());
    }

    public SerializedValue make(Class<?>[] types, Object[] arguments) {
        String[] serializedTypes = new String[types.length];
        int index = 0;
        for (Class<?> type : types) {
            serializedTypes[index++] = type.getName();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Output out = new Output(bytes);
        kryo.writeObject(out, arguments.length);
        for (Object argument : arguments) {
            kryo.writeClassAndObject(out, argument);
        }
        out.close();
        return new SerializedValue(serializedTypes, bytes.toByteArray());
    }

    public Object[] resolveArguments(SerializedValue value) {
        Input in = new Input(value.getArguments());
        Object[] arguments = new Object[kryo.readObject(in, Integer.class)];
        for (int index = 0; index < arguments.length; index++) {
            arguments[index] = kryo.readClassAndObject(in);
        }
        return arguments;
    }
}
