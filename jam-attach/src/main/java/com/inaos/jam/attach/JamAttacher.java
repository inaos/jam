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

package com.inaos.jam.attach;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.*;

public class JamAttacher {

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Must specify at least one argument in form [process info]");
        } else if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Must specify arguments in form [process info]");
        }

        InputStream agentJar = JamAttacher.class.getResourceAsStream("/jam-agent.jar");
        if (agentJar == null) {
            throw new IllegalStateException("Agent jar not found");
        }
        File materializedAgentJar;
        try {
            try {
                materializedAgentJar = File.createTempFile("jam-agent", ".jar");
                OutputStream out = new FileOutputStream(materializedAgentJar);
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = agentJar.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    out.close();
                }
            } finally {
                agentJar.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        for (int index = 0; index < args.length / 2; index += 2) {
            System.out.println("Attaching to process " + args[index] + " with arguments '" + args[index + 1] + "'");
            ByteBuddyAgent.attach(materializedAgentJar, args[index], args[index + 1]);
        }
    }
}
