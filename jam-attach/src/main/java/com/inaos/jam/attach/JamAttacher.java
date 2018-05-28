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

import java.io.File;

public class JamAttacher {

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected an agent jar as a first argument and at least one process id");
        }

        File agent = new File(args[0]);
        for (int index = 1; index < args.length; index++) {
            ByteBuddyAgent.attach(agent, args[index]);
        }
    }
}
