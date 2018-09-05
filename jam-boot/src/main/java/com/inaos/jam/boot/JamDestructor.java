package com.inaos.jam.boot;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JamDestructor extends Thread {

    private static final Set<Method> DESTRUCTORS = Collections.newSetFromMap(new ConcurrentHashMap<Method, Boolean>());

    static {
        Runtime.getRuntime().addShutdownHook(new JamDestructor());
    }

    public void register(Class<?> dispatcher, String name) throws NoSuchMethodException {
        DESTRUCTORS.add(dispatcher.getDeclaredMethod(name));
    }

    @Override
    public void run() {
        try {
            for (Method method : DESTRUCTORS) {
                method.setAccessible(true);
                method.invoke(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
