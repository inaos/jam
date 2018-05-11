package com.inaos.iamj.agent;

import com.esotericsoftware.kryo.io.Output;
import com.inaos.iamj.observation.Observation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class DispatcherToFile extends DispatcherBase {

    private final File target;

    private final long maxObservationCount, maxObservationBytes;

    private final ConcurrentMap<String, AtomicLong> observationCount = new ConcurrentHashMap<String, AtomicLong>(), observationBytes = new ConcurrentHashMap<String, AtomicLong>();

    public DispatcherToFile(File target, long maxObservationCount, long maxObservationBytes) {
        this.target = target;
        this.maxObservationCount = maxObservationCount;
        this.maxObservationBytes = maxObservationBytes;
    }

    @Override
    protected boolean suppressSample(String name) {
        if (!observationCount.containsKey(name)) {
            observationCount.putIfAbsent(name, new AtomicLong());
        }
        if (!observationBytes.containsKey(name)) {
            observationBytes.putIfAbsent(name, new AtomicLong());
        }
        AtomicLong sum = observationBytes.get(name);
        return observationCount.get(name).incrementAndGet() > maxObservationCount || sum.get() > maxObservationBytes;
    }

    @Override
    protected synchronized void doCommit(Observation observation) {
        AtomicLong sum = observationBytes.get(observation.getName());
        try {
            Output out = new Output(new ByteCountingStream(new FileOutputStream(target, true), sum));
            try {
                kryo.writeObject(out, observation);
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
