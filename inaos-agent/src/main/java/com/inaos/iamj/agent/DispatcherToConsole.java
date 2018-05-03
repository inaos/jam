package com.inaos.iamj.agent;

import com.esotericsoftware.kryo.Kryo;
import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.SerializedValue;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.Arrays;

public class DispatcherToConsole extends InaosAgentDispatcher {

    private final Kryo kryo = new Kryo();

    {
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    @Override
    protected Object accept(Class<?>[] types, Object[] arguments) {
        return SerializedValue.make(kryo, types, arguments);
    }

    @Override
    protected void accept(String name, Object entry,
                          String dispatcherName, String methodName,
                          Class<?> returnType, Class<?>[] argumentTypes,
                          Object returnValue, Object[] argumentValues) {
        StringBuilder sb = new StringBuilder().append("Invoking ").append(name);
        sb.append(" for dispatching to ").append(dispatcherName).append(" ").append(methodName);
        if (entry != null) {
            SerializedValue serializedValue = (SerializedValue) entry;
            sb.append(" - Altered arguments, on method entry: ").append(Arrays.toString(serializedValue.resolveArguments(kryo)));
        }
        if (returnType != void.class) {
            sb.append(" - Yields return value: ").append(returnValue);
        }
        sb.append(" - With arguments: ").append(Arrays.toString(argumentValues));
        System.out.println(sb.toString());
    }
}
