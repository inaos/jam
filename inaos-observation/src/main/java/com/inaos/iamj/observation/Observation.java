package com.inaos.iamj.observation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Observation implements Serializable {

    private final String name;

    private final Serializable[] arguments;

    public Observation(String name, Serializable[] arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public List<Serializable> getArguments() {
        return Arrays.asList(arguments);
    }

    @Override
    public String toString() {
        return "Captured call of " + name + " using " + Arrays.deepToString(arguments);
    }
}
