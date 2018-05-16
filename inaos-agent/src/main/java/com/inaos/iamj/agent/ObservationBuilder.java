package com.inaos.iamj.agent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.inaos.iamj.observation.Observation;
import com.inaos.iamj.observation.SerializedValue;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

class ObservationBuilder {

    private final String name;

    private final Map<String, SerializedValue> values;

    ObservationBuilder(String name) {
        this.name = name;
        values = new HashMap<String, SerializedValue>();
    }

    void serialize(Kryo kryo, String name, Class<?> type, Object argument) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("Key " + name + " is already registered");
        }
        values.put(name, make(kryo, type, argument));
    }

    void serialize(Kryo kryo, String name, Class<?>[] types, Object[] arguments) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("Key " + name + " is already registered");
        }
        values.put(name, make(kryo, types, arguments));
    }

    Observation toObservation() {
        return new Observation(name, values);
    }


    private SerializedValue make(Kryo kryo, Class<?> type, Object argument) {
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

    public SerializedValue make(Kryo kryo, Class<?>[] types, Object[] arguments) {
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
}
