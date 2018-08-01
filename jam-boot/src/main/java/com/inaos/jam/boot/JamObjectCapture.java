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

public abstract class JamObjectCapture {

    public static volatile JamObjectCapture dispatcher;

    public static void capture(Object key, String name, Object value) {
        JamObjectCapture dispatcher = JamObjectCapture.dispatcher;
        if (dispatcher != null) {
            dispatcher.doCapture(key, name, value);
        }
    }

    public static Object read(Object key, String name) {
        JamObjectCapture dispatcher = JamObjectCapture.dispatcher;
        if (dispatcher != null) {
            return dispatcher.doRead(key, name);
        } else {
            return null;
        }
    }

    protected abstract void doCapture(Object key, String name, Object value);

    protected abstract Object doRead(Object key, String name);
}
