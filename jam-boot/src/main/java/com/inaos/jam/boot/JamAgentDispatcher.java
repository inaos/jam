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

package com.inaos.jam.boot;

import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class JamAgentDispatcher {

    static {
        Iterator<JamAgentDispatcher> it = ServiceLoader.load(JamAgentDispatcher.class).iterator();
        if (it.hasNext()) {
            dispatcher = it.next();
        }
    }

    private static final Object NOOP = new Object();

    public static volatile JamAgentDispatcher dispatcher;

    public static Object observe(String name) {
        JamAgentDispatcher dispatcher = JamAgentDispatcher.dispatcher;
        Object observation = null;
        if (dispatcher != null) {
            observation = dispatcher.doObserve(name);
        }
        return observation == null ? NOOP : observation;
    }

    public static void attach(Object observation, String name, Class<?> type, Object argument) {
        JamAgentDispatcher dispatcher = JamAgentDispatcher.dispatcher;
        if (dispatcher != null && observation != NOOP) {
            dispatcher.doAttach(observation, name, type, argument);
        }
    }

    public static void attach(Object observation, String name, Class<?>[] types, Object[] arguments) {
        JamAgentDispatcher dispatcher = JamAgentDispatcher.dispatcher;
        if (dispatcher != null && observation != NOOP) {
            dispatcher.doAttach(observation, name, types, arguments);
        }
    }

    public static void commit(Object observation) {
        JamAgentDispatcher dispatcher = JamAgentDispatcher.dispatcher;
        if (dispatcher != null && observation != NOOP) {
            dispatcher.doCommit(observation);
        }
    }

    protected abstract Object doObserve(String name);

    protected abstract void doAttach(Object observation, String name, Class<?> type, Object argument);

    protected abstract void doAttach(Object observation, String name, Class<?>[] types, Object[] arguments);

    protected abstract void doCommit(Object observation);
}
