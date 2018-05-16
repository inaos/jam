package com.inaos.imaj.test;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestRunner {

    private final String libraryUri;

    private boolean devMode;

    private File sample;

    public TestRunner(String libraryUri) {
        this.libraryUri = libraryUri;
    }

    public TestRunner devMode() {
        devMode = true;
        return this;
    }

    public TestRunner devMode(File sample) {
        devMode = true;
        this.sample = sample;
        return this;
    }

    public void run(Class<?> main) throws IOException {
        InputStream in = TestRunner.class.getResourceAsStream("/inaos-agent.jar");
        if (in == null) {
            throw new IllegalStateException("Agent jar was not created");
        }
        File agent;
        try {
            agent = File.createTempFile("instana-agent", ".jar");
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

        List<String> command = new ArrayList<String>();
        command.add(System.getProperty("java.home") + "/bin/java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path")
                .replace("/home/rafael/workspace/inaos/iamj/inaos-example-lib/target/classes:", "")
                .replace("/home/rafael/workspace/inaos/iamj/inaos-boot/target/classes:", "")
                .replace("/home/rafael/workspace/inaos/iamj/inaos-api/target/classes:", ""));
        command.add("-javaagent:" + agent.getAbsolutePath()
                + "="
                + "library=" + libraryUri
                + ",devMode=" + devMode
                + (sample != null ? ",sample=" + sample.getAbsolutePath() : ""));
        command.add(TestMain.class.getName());
        command.add(main.getName());

        System.out.println("Running test with arguments: " + command);

        ProcessResult processResult;
        try {
            processResult = new ProcessExecutor()
                    .timeout(5, TimeUnit.MINUTES)
                    .command(command)
                    .readOutput(true)
                    .redirectOutput(System.out)
                    .redirectError(System.out)
                    .execute();
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        if (processResult.getExitValue() != 0) {
            throw new AssertionError("Process terminated with an exceptional exit code");
        }
    }
}
