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

package com.inaos.jam.tool.memory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class NativeMemoryManager {

    private final NativeMemoryCallback callback;

    private final Map<Long, NativeMemoryReference> nativeMemoryBackedInstances =
            new HashMap<Long, NativeMemoryReference>();

    public NativeMemoryManager(NativeMemoryCallback callback) {
        this.callback = callback;
    }

    public long allocate(long size) {
        return callback.allocate(size);
    }

    public void register(NativeMemory memory) {
        NativeMemoryReference ref = new NativeMemoryReference(this, memory);
        nativeMemoryBackedInstances.put(memory.getPointer(), ref);
    }

    public void free(long pointer) {
        callback.free(pointer);
        nativeMemoryBackedInstances.remove(pointer);
    }

    public boolean allMemoryFreed() {
        return nativeMemoryBackedInstances.keySet().size() == 0;
    }

    public void report(PrintStream stream) {
        if (nativeMemoryBackedInstances.keySet().size() == 0) {
            stream.println("No native memory alive.");
            return;
        }
        stream.println("Following native memory is still allocated: ");
        for (Long ptr : nativeMemoryBackedInstances.keySet()) {
            stream.println("Address: "+Long.toHexString(ptr));
        }
    }
}

