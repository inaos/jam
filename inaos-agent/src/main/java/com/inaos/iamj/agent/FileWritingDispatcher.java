package com.inaos.iamj.agent;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileWritingDispatcher extends InaosAgentDispatcher {

    private final File target;
    private final int maxObservationCount;
    private final long maxObservationBytes;
    private final Map<String, Integer> observationCount = new HashMap<String, Integer>();
    private final Map<String, Long> observationBytes = new HashMap<String, Long>();

    public FileWritingDispatcher(File target, int maxObservationCount, long maxObservationBytes) {
        this.target = target;
        this.maxObservationCount = maxObservationCount;
        this.maxObservationBytes = maxObservationBytes;
    }

    @Override
    protected synchronized void accept(String name, Serializable returned, Serializable[] args) {
        try {
        	Observation o = new Observation(name, returned, args);
        	if (canWriteToFile(name, o)) {
	            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target, true));
	            try {
	                out.writeObject(o);
	            } finally {
	                out.close();
	            }
        	}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private boolean canWriteToFile(String name, Observation observation) throws IOException {
		Integer count = observationCount.get(name);
        if (count == null) {
        	count = new Integer(0);
        	observationCount.put(name, count);
        }
        count++;
        Long bytes = observationBytes.get(name);
		if (bytes == null) {
			bytes = new Long(0);
		}
		long bytesToWrite = CheckSerializedSize.getSerializedSize(observation);
		bytes += bytesToWrite;
		if (count < this.maxObservationCount && bytes < this.maxObservationBytes) {
			return true;
		}
		return false;
	}
	
	private final static class CheckSerializedSize extends OutputStream {
		
		private long nBytes = 0;
		
	    public static long getSerializedSize(Serializable obj) throws IOException {
	        CheckSerializedSize counter = new CheckSerializedSize();
	        ObjectOutputStream objectOutputStream = new ObjectOutputStream(counter);
	        objectOutputStream.writeObject(obj);
	        objectOutputStream.close();
	        return counter.getNBytes();
	    }

	    private CheckSerializedSize() {}

	    @Override
	    public void write(int b) throws IOException {
	        ++nBytes;
	    }

	    @Override
	    public void write(byte[] b, int off, int len) throws IOException {
	        nBytes += len;
	    }

	    public long getNBytes() {
	        return nBytes;
	    }
	}
}
