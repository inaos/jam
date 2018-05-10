package com.inaos.iamj.observation;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class SerializedValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] types;

    private final byte[] arguments;

    public SerializedValue(String[] types, byte[] arguments) {
        this.types = types;
        this.arguments = arguments;
    }

    public static SerializedValue make(Kryo kryo, Class<?> type, Object argument) {
        if (type == void.class) {
            return null;
        }
        String[] serializedTypes = {type.getName()};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Output out = new Output(bytes);
        kryo.writeObject(out, 1);
        kryo.writeClassAndObject(out, argument);
        out.close();
        return new SerializedValue(serializedTypes, bytes.toByteArray());

    }

    public static SerializedValue make(Kryo kryo, Class<?>[] types, Object[] arguments) {
        String[] serializedTypes = new String[types.length];
        int index = 0;
        for (Class<?> type : types) {
            serializedTypes[index++] = type.getName();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Output out = new Output(bytes);
        kryo.writeObject(out, arguments.length);
        for (Object argument : arguments) {
            kryo.writeClassAndObject(out, argument);
        }
        out.close();
        return new SerializedValue(serializedTypes, bytes.toByteArray());
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

    public Object[] resolveArguments(Kryo kryo) {
        Input in = new Input(arguments);
        Object[] arguments = new Object[kryo.readObject(in, Integer.class)];
        for (int index = 0; index < arguments.length; index++) {
            arguments[index] = kryo.readClassAndObject(in);
        }
        return arguments;
    }
}
