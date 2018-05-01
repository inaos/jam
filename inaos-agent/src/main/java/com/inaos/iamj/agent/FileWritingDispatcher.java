package com.inaos.iamj.agent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class FileWritingDispatcher extends InaosAgentDispatcher {

    private final Kryo kryo = new Kryo();
    private final File target;

    private final long maxObservationCount, maxObservationBytes;
    private final ConcurrentMap<String, AtomicLong> observationCount = new ConcurrentHashMap<String, AtomicLong>(), observationBytes = new ConcurrentHashMap<String, AtomicLong>();

    public FileWritingDispatcher(File target, long maxObservationCount, long maxObservationBytes) {
        this.target = target;
        this.maxObservationCount = maxObservationCount;
        this.maxObservationBytes = maxObservationBytes;
    }

    @Override
    protected synchronized void accept(String name, Object returned, Object[] args) {
        observationCount.putIfAbsent(name, new AtomicLong());
        if (observationCount.get(name).incrementAndGet() > maxObservationCount) {
            return;
        }
        observationBytes.putIfAbsent(name, new AtomicLong());
        AtomicLong sum = observationBytes.get(name);
        if (sum.get() > maxObservationBytes) {
            return;
        }
        try {
            Observation o = new Observation(name, returned, args);
            Output out = new Output(new ByteCountingStream(new FileOutputStream(target, true), sum));
            try {
                kryo.writeClassAndObject(out, o);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ByteCountingStream extends OutputStream {

        private final OutputStream out;
        private final AtomicLong sum;

        private long count;

        ByteCountingStream(OutputStream out, AtomicLong sum) {
            this.out = out;
            this.sum = sum;
        }

        @Override
        public void write(int b) throws IOException {
            count++;
            out.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            count += b.length;
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            count += len;
            out.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            sum.addAndGet(count);
            out.close();
        }
    }
}
