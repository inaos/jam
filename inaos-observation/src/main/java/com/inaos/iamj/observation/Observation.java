package com.inaos.iamj.observation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Observation implements Serializable {

    private final String name;

    private final Object returned;

    private final Object[] arguments;

    public Observation(String name, Object returned, Object... arguments) {
        this.name = name;
        this.returned = returned;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public Object getReturned() {
        return returned;
    }

    public List<Object> getArguments() {
        return Arrays.asList(arguments);
    }

    @Override
    public String toString() {
    	if (returned == null) {
    		return "Captured call of " + name + " returned void using " + Arrays.deepToString(arguments);
    	} else {
    		return "Captured call of " + name + " returned " + returned + " using " + Arrays.deepToString(arguments);
    	}
    }
}
