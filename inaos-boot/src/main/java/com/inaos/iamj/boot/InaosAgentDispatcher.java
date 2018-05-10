package com.inaos.iamj.boot;

public abstract class InaosAgentDispatcher {

    private static final Object NOOP = new Object();

    public static volatile InaosAgentDispatcher dispatcher;

    public static Object observe(String name) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        Object observation = null;
        if (dispatcher != null) {
            observation = dispatcher.doObserve(name);
        }
        return observation == null ? NOOP : observation;
    }

    public static void attach(Object observation, String name, Class<?> type, Object argument) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher != null && observation != NOOP) {
            dispatcher.doAttach(observation, name, type, argument);
        }
    }

    public static void attach(Object observation, String name, Class<?>[] types, Object[] arguments) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher != null && observation != NOOP) {
            dispatcher.doAttach(observation, name, types, arguments);
        }
    }

    public static void commit(Object observation) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher != null && observation != NOOP) {
            dispatcher.doCommit(observation);
        }
    }

    protected abstract Object doObserve(String name);

    protected abstract void doAttach(Object observation, String name, Class<?> type, Object argument);

    protected abstract void doAttach(Object observation, String name, Class<?>[] types, Object[] arguments);

    protected abstract void doCommit(Object observation);
}
