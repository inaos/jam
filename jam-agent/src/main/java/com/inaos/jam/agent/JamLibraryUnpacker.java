/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.agent;

import java.io.*;

public class JamLibraryUnpacker {

    public static String unpack(Class<?> type, String resource, String extension) {
        InputStream in = type.getClassLoader().getResourceAsStream(resource);
        File file;
        try {
            file = File.createTempFile(resource, "." + extension);
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
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return file.getAbsolutePath();
    }
}
