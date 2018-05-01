package com.inaos.iamj.agent;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

public class ConsoleDispatcher extends InaosAgentDispatcher {

    @Override
    protected void accept(String name,
                          String dispatcherName, String methodName,
                          Class<?> returnType, Class<?>[] argumentTypes,
                          Object returnValue, Object[] argumentValues) {
        System.out.println(new Observation(name, dispatcherName, methodName, returnType, argumentTypes, returnValue, argumentValues));
    }
}
