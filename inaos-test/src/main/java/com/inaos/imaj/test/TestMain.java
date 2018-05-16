package com.inaos.imaj.test;

import java.lang.reflect.InvocationTargetException;

public class TestMain {

    public static void main(String[] args) {
        try {
            Class.forName(args[0]).getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t.getCause().printStackTrace();
            } else {
                t.printStackTrace();
            }
            System.exit(1);
        }
    }
}
