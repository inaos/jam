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

import com.esotericsoftware.kryo.io.Output;
import com.inaos.jam.observation.Observation;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class DispatcherToFile extends DispatcherBase {

    private final File target;

    private final long maxObservationCount, maxObservationBytes;

    private final ConcurrentMap<String, AtomicLong> observationCount = new ConcurrentHashMap<String, AtomicLong>(), observationBytes = new ConcurrentHashMap<String, AtomicLong>();

    public DispatcherToFile() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("META-INF/jam/sample.location"), "utf-8"));
        try {
            target = new File(in.readLine());
            maxObservationCount = Long.parseLong(in.readLine());
            maxObservationBytes = Long.parseLong(in.readLine());
        } finally {
            in.close();
        }
    }

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
