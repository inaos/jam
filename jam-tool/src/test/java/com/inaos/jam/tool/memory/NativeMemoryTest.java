/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.tool.memory;

import com.inaos.jam.tool.tensor.NativeDoubleTensor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Constructor;

public class NativeMemoryTest {

    private TestNativeMemory memory;

    @Before
    public void setup() throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();
        memory = new TestNativeMemory(unsafe);
    }

    private final static class TestNativeMemory implements NativeMemoryCallback {

        private final Unsafe unsafe;

        TestNativeMemory(Unsafe unsafe) {
            this.unsafe = unsafe;
        }

        @Override
        public long allocate(long size) {
            return unsafe.allocateMemory(size);
        }

        @Override
        public void free(long pointer) {
            unsafe.freeMemory(pointer);
        }
    }

    @Test
    public void testNativeMemoryViaTensor() throws Exception {

        NativeMemoryManager manager = new NativeMemoryManager(memory);

        // those tree steps are required to make sure native memory is clean-up after the java object gets GC'ed
        //
        // 1. allocate the native memory
        // 2. pass the native pointer to the Java wrapper
        // 3. register the java wrapper with the manager
        //
        long pointer = manager.allocate((Double.SIZE / Byte.SIZE) * 2000);

        NativeDoubleTensor tensor = new NativeDoubleTensor(pointer, 200*10);

        manager.register(tensor);

        tensor = null;

        System.gc();
        Thread.sleep(200); // give GC and clean-up thread some time to do their work

        if (!manager.allMemoryFreed()) {
            manager.report(System.out);
        }

        Assert.assertTrue(manager.allMemoryFreed());
    }
}
