package com.inaos.iamj.utility;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.inaos.iamj.observation.Observation;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.File;
import java.io.FileInputStream;

public class Main {

    @Parameter(names = {"--source", "-s"}, description = "Input file", required = true)
    private File source;

    @Parameter(names = {"--target", "-t"}, description = "Output folder")
    private File target = new File(".");

    @Parameter(names = {"--dispatcher", "-d"}, description = "Generate dispatcher classes")
    private boolean dispatcher;

    public static void main(String... args) throws Exception {
        Main command = new Main();
        JCommander.newBuilder().addObject(command).build().parse(args);

        if (command.source == null || !command.source.isFile()) {
            throw new IllegalArgumentException("No source file given or not a file: " + command.source);
        } else if (command.target == null || !command.target.isDirectory()) {
            throw new IllegalArgumentException("No target folder given or not a file: " + command.target);
        }

        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        DispatcherGenerator dispatcherGenerator = new DispatcherGenerator();

        Input in = new Input(new FileInputStream(command.source));
        try {
            while (!Thread.interrupted() && !in.eof()) {
                Observation observation = kryo.readObject(in, Observation.class);
                if (command.dispatcher) {
                    dispatcherGenerator.generateDispatcher(observation, command.target);
                }
            }
        } finally {
            in.close();
        }
    }
}
