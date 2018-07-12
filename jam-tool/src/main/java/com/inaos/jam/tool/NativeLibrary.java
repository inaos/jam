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

package com.inaos.jam.tool;

import java.io.*;

public class NativeLibrary {

    public static void loadLibrary(final String libraryName, ClassLoader classLoader) {
        final String resource = Platform.NATIVE_SHARED_OBJ_FOLDER + "/" + Platform.NATIVE_SHARED_OBJ_PREFIX + libraryName;
        final String fullPath = resource + "." + Platform.NATIVE_SHARED_OBJ_EXT;
        InputStream in = classLoader.getResourceAsStream(fullPath);
        File file;
        if (in == null) {
            throw new IllegalArgumentException("Could not find shared-object in classpath: "+fullPath);
        }
        try {
            file = File.createTempFile(resource, "." + Platform.NATIVE_SHARED_OBJ_EXT);
            OutputStream out = new FileOutputStream(file);
            try {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            } finally {
                out.close();
            }
            in.close();
            System.load(file.getAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
