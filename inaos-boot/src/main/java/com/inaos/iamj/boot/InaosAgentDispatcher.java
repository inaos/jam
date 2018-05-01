package com.inaos.iamj.boot;

public abstract class InaosAgentDispatcher {

    public static volatile InaosAgentDispatcher dispatcher;

    public static void serialize(String name,
                                 Class<?>[] argumentTypes,
                                 Object[] argumentValues) {
        serialize(name, void.class, argumentTypes, null, argumentValues);
    }

    public static void serialize(String name,
                                 Class<?> returnType, Class<?>[] argumentTypes,
                                 Object returnValue, Object[] argumentValues) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher == null) {
            return;
        }
        dispatcher.accept(name, returnType, argumentTypes, returnValue, argumentValues);
    }

    protected abstract void accept(String name,
                                   Class<?> returnType, Class<?>[] argumentTypes,
                                   Object returnValue, Object[] argumentValues);
}
