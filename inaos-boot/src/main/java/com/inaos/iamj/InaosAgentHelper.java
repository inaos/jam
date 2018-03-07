package com.inaos.iamj;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InaosAgentHelper {

    static void serialize(String name, Serializable... args) {
        List<String> nativeSignature = new ArrayList<String>();
        for (Serializable s : args) {
            nativeSignature.add(s.getClass().getName());
        }
        System.out.println(name + " - " + nativeSignature + ": " + Arrays.toString(args));
    }

}
