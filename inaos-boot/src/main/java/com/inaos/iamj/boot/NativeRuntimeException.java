package com.inaos.iamj.boot;

import java.nio.file.Paths;

public class NativeRuntimeException extends RuntimeException {
	public NativeRuntimeException(String message) {
		super(message);
	}
	
	public void setNativeError(String functionName, String file, int line) {
		addStackTraceElement(this, functionName, file, line);
	}
	
	private static void addStackTraceElement(Throwable t, String functionName, String file, int line) {
		StackTraceElement[] currentStack = t.getStackTrace();
		StackTraceElement[] newStack = new StackTraceElement[currentStack.length+1];
		
		System.arraycopy(currentStack, 0, newStack, 1, currentStack.length);
		
		// we do this because of windows path names (backslashes and other fun)
		file = Paths.get(file).normalize().toString();
		
		newStack[0] = new StackTraceElement("<native>", functionName, file, line);
		
		t.setStackTrace(newStack);
	}
}
