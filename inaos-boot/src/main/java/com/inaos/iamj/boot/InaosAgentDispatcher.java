package com.inaos.iamj.boot;

import java.io.Serializable;

public abstract class InaosAgentDispatcher {

    public static volatile InaosAgentDispatcher dispatcher;

    public static void serialize(String name, Serializable returned, Serializable... args) {
        InaosAgentDispatcher dispatcher = InaosAgentDispatcher.dispatcher;
        if (dispatcher == null) {
            return;
        }
        dispatcher.accept(name, returned, args);
    }

    protected abstract void accept(String name, Serializable returned, Serializable[] args);
}
