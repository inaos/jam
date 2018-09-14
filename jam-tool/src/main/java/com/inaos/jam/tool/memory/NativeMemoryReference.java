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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class NativeMemoryReference extends WeakReference {

    private final static ReferenceQueue<NativeMemory>
            referenceQueue = new ReferenceQueue<NativeMemory>();

    static {
        new CleanupThread().start();
    }

    private final NativeMemoryManager manager;

    private final long memoryAddress;

    public NativeMemoryReference(NativeMemoryManager manager, NativeMemory memory) {
        super(memory, referenceQueue);
        memoryAddress = memory.getPointer();
        this.manager = manager;
    }

    public void cleanUp() {
        manager.free(memoryAddress);
    }

    static class CleanupThread extends Thread {
        public CleanupThread() {
            setPriority(Thread.MAX_PRIORITY);
            setName("JamNativeGarbageCollection");
            setDaemon(true);
        }
        public void run() {
            while (true) {
                try {
                    NativeMemoryReference ref = (NativeMemoryReference)referenceQueue.remove();
                    ref.cleanUp();
                }
                catch (InterruptedException e) {
                    // swallow
                }
            }
        }
    }
}
