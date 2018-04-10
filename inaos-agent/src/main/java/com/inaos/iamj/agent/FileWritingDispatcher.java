package com.inaos.iamj.agent;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

import java.io.*;

public class FileWritingDispatcher extends InaosAgentDispatcher {

    private final File target;

    public FileWritingDispatcher(File target) {
        this.target = target;
    }

    @Override
    protected synchronized void accept(String name, Serializable returned, Serializable[] args) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target, true));
            try {
                out.writeObject(new Observation(name, returned, args));
            } finally {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
