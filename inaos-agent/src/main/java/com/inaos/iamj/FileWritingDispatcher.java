package com.inaos.iamj;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

import java.io.*;

class FileWritingDispatcher extends InaosAgentDispatcher {

    private final File target;

    FileWritingDispatcher(File target) {
        this.target = target;
    }

    @Override
    protected synchronized void accept(String name, Serializable[] args) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target));
            try {
                out.writeObject(new Observation(name, args));
            } finally {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
