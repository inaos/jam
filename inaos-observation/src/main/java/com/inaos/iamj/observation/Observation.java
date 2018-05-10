package com.inaos.iamj.observation;

import java.io.Serializable;
import java.util.Map;

public class Observation implements Serializable {

    private final String name;

    private final Map<String, SerializedValue> values;

    public Observation(String name, Map<String, SerializedValue> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public Map<String, SerializedValue> getValues() {
        return values;
    }

    public SerializedValue value(String name) {
        return values.get(name);
    }
}
