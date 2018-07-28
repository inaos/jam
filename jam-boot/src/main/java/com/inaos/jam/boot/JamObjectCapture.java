package com.inaos.jam.boot;

public abstract class JamObjectCapture {

    public static volatile JamObjectCapture dispatcher;

    public static void capture(Object key, String name, Object value) {
        JamObjectCapture dispatcher = JamObjectCapture.dispatcher;
        if (dispatcher != null) {
            dispatcher.doCapture(key, name, value);
        }
    }

    public static Object read(Object key, String name) {
        JamObjectCapture dispatcher = JamObjectCapture.dispatcher;
        if (dispatcher != null) {
            return dispatcher.doRead(key, name);
        } else {
            return null;
        }
    }

    protected abstract void doCapture(Object key, String name, Object value);

    protected abstract Object doRead(Object key, String name);
}
