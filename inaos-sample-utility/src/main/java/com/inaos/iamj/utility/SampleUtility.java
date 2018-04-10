package com.inaos.iamj.utility;

import com.inaos.iamj.observation.Observation;

import java.io.*;

public class SampleUtility {

    public static void main(String... args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Did not specify any arguments");
        }
        try {
            for (String file : args) {
                main(System.out, new File(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(PrintStream out, File target) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(target));
        out.println("Extracting all observations from " + target);
        while (!Thread.interrupted()) {
            try {
                Observation observation = (Observation) in.readObject();
                out.println(observation);
            } catch (EOFException ignored) {
                return;
            }
        }
    }
}
