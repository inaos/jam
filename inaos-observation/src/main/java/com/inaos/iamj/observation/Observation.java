package com.inaos.iamj.observation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Observation implements Serializable {

    private final String name;

    private final Class<?> returnType;
    private final Class<?>[] argumentTypes;

    private final Object returnValue;
    private final Object[] argumentValues;

    public Observation(String name, Class<?> returnType, Class<?>[] argumentTypes, Object returnValue, Object[] argumentValues) {
        this.name = name;
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
        this.returnValue = returnValue;
        this.argumentValues = argumentValues;
    }

    public String getName() {
        return name;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?>[] getArgumentTypes() {
        return argumentTypes;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Object[] getArgumentValues() {
        return argumentValues;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Observation for ").append(name)
                .append(" with native dispatch")
                .append(" returning ").append(returnType).append(" with value ").append(returnValue)
                .append(" for arguments ").append(Arrays.toString(argumentValues)).append(" with values ").append(Arrays.deepToString(argumentValues))
                .toString();
    }
}
