package com.inaos.iamj.observation;

import java.io.Serializable;

public class SerializedValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] types;

    private final byte[] arguments;

    public SerializedValue(String[] types, byte[] arguments) {
        this.types = types;
        this.arguments = arguments;
    }

    public String[] getTypes() {
        return types;
    }

    public byte[] getArguments() {
        return arguments;
    }

    public Class<?>[] resolveTypes(ClassLoader classLoader) {
        Class<?>[] types = new Class<?>[this.types.length];
        int index = 0;
        try {
            for (String type : this.types) {
                types[index++] = forName(type, classLoader);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return types;
    }

    private static Class<?> forName(String type, ClassLoader classLoader) throws ClassNotFoundException {
        if (void.class.toString().equals(type)) return void.class;
        if (boolean.class.toString().equals(type)) return boolean.class;
        if (byte.class.toString().equals(type)) return byte.class;
        if (short.class.toString().equals(type)) return short.class;
        if (char.class.toString().equals(type)) return char.class;
        if (int.class.toString().equals(type)) return int.class;
        if (long.class.toString().equals(type)) return long.class;
        if (float.class.toString().equals(type)) return float.class;
        if (double.class.toString().equals(type)) return double.class;
        return Class.forName(type, false, classLoader);
    }
}
