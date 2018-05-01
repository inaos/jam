package com.inaos.iamj.boot;

public abstract class InaosAgentDispatcher {

    public static volatile InaosAgentDispatcher dispatcher;

    public static void serialize(String name, Object returned, Object... args) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher == null) {
            return;
        }
        dispatcher.accept(name, returned, args);
    }

    protected abstract void accept(String name, Object returned, Object[] args);
}
