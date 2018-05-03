package com.inaos.iamj.observation;

import java.io.Serializable;

public class Observation implements Serializable {

    private final String name, dispatcherName, methodName;

    private final SerializedValue serializedArguments, serializedArgumentsOnEntry, serializedReturn;

    public Observation(String name, String dispatcherName, String methodName,
                       SerializedValue serializedArguments, SerializedValue serializedArgumentsOnEntry, SerializedValue serializedReturn) {
        this.name = name;
        this.dispatcherName = dispatcherName;
        this.methodName = methodName;
        this.serializedArguments = serializedArguments;
        this.serializedArgumentsOnEntry = serializedArgumentsOnEntry;
        this.serializedReturn = serializedReturn;
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

    public SerializedValue getSerializedArguments() {
        return serializedArguments;
    }

    public SerializedValue getSerializedArgumentsOnEntry() {
        return serializedArgumentsOnEntry;
    }

    public SerializedValue getSerializedReturn() {
        return serializedReturn;
    }
}
