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
