package com.inaos.iamj.observation;

import java.io.Serializable;
import java.util.Arrays;

public class Observation implements Serializable {

    private final String name;

    private final String dispatcherName;

    private final String methodName;

    private final Class<?> returnType;

    private final Class<?>[] argumentTypes;

    private final Object returnValue;

    private final Object[] argumentValues;

    public Observation(String name,
                       String dispatcherName, String methodName,
                       Class<?> returnType, Class<?>[] argumentTypes,
                       Object returnValue, Object[] argumentValues) {
        this.name = name;
        this.dispatcherName = dispatcherName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
        this.returnValue = returnValue;
        this.argumentValues = argumentValues;
    }

    public String getName() {
        return name;
    }

    public String getDispatcherName() {
        return dispatcherName;
    }

    public String getMethodName() {
        return methodName;
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
        return "Observation for " + name +
                " with native dispatch to " + dispatcherName + " on " + methodName +
                " returning " + returnType + " with value " + returnValue +
                " using arguments " + Arrays.toString(argumentValues) + " with values " + Arrays.deepToString(argumentValues);
    }
}
