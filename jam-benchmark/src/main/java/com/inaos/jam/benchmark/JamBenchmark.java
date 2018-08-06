package com.inaos.jam.benchmark;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JamBenchmark {

    private final Options options;

    private final List<String> names = new ArrayList<String>(), sources = new ArrayList<String>();

    public JamBenchmark() {
        this(new OptionsBuilder()
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build());
    }

    public JamBenchmark(Options options) {
        this.options = options;
    }

    public JamBenchmark withNames(String... names) {
        this.names.addAll(Arrays.asList(names));
        return this;
    }

    public JamBenchmark withSources(String... sources) {
        this.sources.addAll(Arrays.asList(sources));
        return this;
    }

    public void run(String libraryUri) throws IOException, RunnerException {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one input file");
        } else if (names.isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one method name");

        }

        InputStream in = JamBenchmark.class.getResourceAsStream("/jam-agent.jar");
        if (in == null) {
            throw new IllegalStateException("Agent jar was not created");
        }
        File agent;
        try {
            agent = File.createTempFile("jam-agent", ".jar");
            agent.deleteOnExit();
            OutputStream out = new FileOutputStream(agent);
            try {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }

        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder().parent(options)
                .param("source", sources.toArray(new String[0]))
                .param("name", names.toArray(new String[0]));
        Collection<RunResult> withoutAgent = new Runner(optionsBuilder.build()).run();
        Collection<RunResult> withAgent = new Runner(optionsBuilder.jvmArgsPrepend("-javaagent:" + agent.getAbsolutePath() + "=library=" + libraryUri).build()).run();

        System.out.println(withoutAgent);
        System.out.println(withAgent);
    }
}
