package com.inaos.iamj.boot;

public abstract class InaosAgentDispatcher {

    public static volatile InaosAgentDispatcher dispatcher;

    public static Object serialize(Class<?>[] types, Object[] values) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher == null) {
            // Do not return null to avoid skip in case of incorrect setup.
            return new Object();
        }
        return dispatcher.accept(types, values);
    }

    public static void serialize(String name, Object entry,
                                 String dispatcherName, String methodName,
                                 Class<?>[] argumentTypes, Object[] argumentValues) {
        serialize(name, entry, dispatcherName, methodName, void.class, argumentTypes, null, argumentValues);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(String name, Object entry,
                                 String dispatcherName, String methodName,
                                 Class<?> returnType, Class<?>[] argumentTypes,
                                 Object returnValue, Object[] argumentValues) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher == null) {
            return;
        }
        dispatcher.accept(name, entry, dispatcherName, methodName, returnType, argumentTypes, returnValue, argumentValues);
    }

    protected abstract Object accept(Class<?>[] types, Object[] arguments);

    protected abstract void accept(String name, Object entry,
                                   String dispatcherName, String methodName,
                                   Class<?> returnType, Class<?>[] argumentTypes,
                                   Object returnValue, Object[] argumentValues);
}
