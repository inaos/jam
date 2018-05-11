package com.inaos.iamj.agent;

import com.esotericsoftware.kryo.Kryo;
import com.inaos.iamj.observation.Observation;
import com.inaos.iamj.observation.SerializedValue;

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
        values.put(name, SerializedValue.make(kryo, type, argument));
    }

    void serialize(Kryo kryo, String name, Class<?>[] types, Object[] arguments) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("Key " + name + " is already registered");
        }
        values.put(name, SerializedValue.make(kryo, types, arguments));
    }

    Observation toObservation() {
        return new Observation(name, values);
    }
}
