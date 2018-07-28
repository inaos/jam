package com.inaos.jam.agent;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import com.inaos.jam.boot.JamObjectCapture;

public class WeakHashMapCapture extends JamObjectCapture {

    private final WeakConcurrentMap<Object, Capture> map = new WeakConcurrentMap.WithInlinedExpunction<Object, Capture>();

    @Override
    protected void doCapture(Object key, String name, Object value) {
        map.put(key, new Capture(name, value));
    }

    @Override
    protected Object doRead(Object key, String name) {
        Capture capture = map.get(key);
        return capture == null || !capture.name.equals(name) ? null : capture.value;
    }

    static class Capture {

        final String name;

        final Object value;

        Capture(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}
