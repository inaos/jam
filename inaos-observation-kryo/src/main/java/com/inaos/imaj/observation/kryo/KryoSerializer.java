package com.inaos.imaj.observation.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.inaos.iamj.observation.SerializedValue;
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
        kryo.writeClassAndObject(out, 1);
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
