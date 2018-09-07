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
