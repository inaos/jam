/*
 * Copyright 2018 INAOS GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inaos.jam.test;

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
        InputStream in = TestRunner.class.getResourceAsStream("/jam-agent.jar");
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

        List<String> command = new ArrayList<String>();

        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");

        command.add(System.getProperty("java.home") + (windows ? "\\bin\\java.exe": "/bin/java"));
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
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
