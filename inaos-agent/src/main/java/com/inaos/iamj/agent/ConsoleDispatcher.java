package com.inaos.iamj.agent;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

public class ConsoleDispatcher extends InaosAgentDispatcher {

    @Override
    protected void accept(String name, Class<?> returnType, Class<?>[] argumentTypes, Object returnValue, Object[] argumentValues) {
        System.out.println(new Observation(name, returnType, argumentTypes, returnValue, argumentValues));
    }
}
