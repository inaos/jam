package com.inaos.iamj.observation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Observation implements Serializable {

    private final String name;

    private final Serializable returned;

    private final Serializable[] arguments;

    public Observation(String name, Serializable returned, Serializable[] arguments) {
        this.name = name;
        this.returned = returned;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public Serializable getReturned() {
        return returned;
    }

    public List<Serializable> getArguments() {
        return Arrays.asList(arguments);
    }

    @Override
    public String toString() {
        return "Captured call of " + name + " returned " + returned + " using " + Arrays.deepToString(arguments);
    }
}
