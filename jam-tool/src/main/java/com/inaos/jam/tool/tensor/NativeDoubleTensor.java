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

package com.inaos.jam.tool.tensor;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

public class NativeDoubleTensor extends NativeTensor {

    protected DoubleBuffer data;

    public NativeDoubleTensor(long pointer, int len) {
        super(pointer);
        initData(len);
    }

    private void initData(int len) {
        int dataSize = len * Double.SIZE / Byte.SIZE;
        try {
            Field address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            Field capacity = Buffer.class.getDeclaredField("capacity");
            capacity.setAccessible(true);
            Field limit = Buffer.class.getDeclaredField("limit");
            limit.setAccessible(true);

            data = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asDoubleBuffer();
            address.setLong(data, pointer);
            capacity.setInt(data, dataSize);
            limit.setInt(data, dataSize);
        }
        catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
