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

package com.inaos.jam.tool;

public class Platform {
    public static final String OS_NAME = getSystemProperty("os.name");

    public static final String OS_ARCH = getSystemProperty("os.arch");

    public static final String OS_NAME_WINDOWS_PREFIX = "Windows";

    public static final boolean IS_OS_LINUX = isOsMatchesName("Linux") || isOsMatchesName("LINUX");

    public static final boolean IS_OS_WINDOWS = isOsMatchesName(OS_NAME_WINDOWS_PREFIX);

    public static final boolean IS_OS_ARCH_64 = "amd64".equals(OS_ARCH);

    public static final boolean IS_OS_ARCH_32 = "x86".equals(OS_ARCH);

    public static final String NATIVE_SHARED_OBJ_EXT;

    public static final String NATIVE_SHARED_OBJ_PREFIX;

    public static final String NATIVE_SHARED_OBJ_FOLDER;

    static {
        if (IS_OS_LINUX) {
            NATIVE_SHARED_OBJ_EXT = "so";
            NATIVE_SHARED_OBJ_PREFIX = "lib";
            if (IS_OS_ARCH_64) {
                NATIVE_SHARED_OBJ_FOLDER = "linux-amd64";
            } else if (IS_OS_ARCH_32) {
                NATIVE_SHARED_OBJ_FOLDER = "linux-i368";
            } else {
                NATIVE_SHARED_OBJ_FOLDER = null;
                throw new IllegalArgumentException("Operating System Architecture not supported: " + OS_ARCH);
            }
        } else if (IS_OS_WINDOWS) {
            NATIVE_SHARED_OBJ_EXT = "dll";
            NATIVE_SHARED_OBJ_PREFIX = "";
            if (IS_OS_ARCH_64) {
                NATIVE_SHARED_OBJ_FOLDER = "win32-amd64";
            } else if (IS_OS_ARCH_32) {
                NATIVE_SHARED_OBJ_FOLDER = "win32-x86";
            } else {
                NATIVE_SHARED_OBJ_FOLDER = null;
                throw new IllegalArgumentException("Operating System Architecture not supported: " + OS_ARCH);
            }
        } else {
            NATIVE_SHARED_OBJ_EXT = null;
            NATIVE_SHARED_OBJ_PREFIX = null;
            NATIVE_SHARED_OBJ_FOLDER = null;
            throw new IllegalArgumentException("Operating System not supported: " + OS_NAME);
        }
    }

    private static boolean isOsMatchesName(String osNamePrefix) {
        return isOSNameMatch(OS_NAME, osNamePrefix);
    }

    private static boolean isOSNameMatch(String osName, String osNamePrefix) {
        if (osName == null) {
            return false;
        }
        return osName.startsWith(osNamePrefix);
    }

    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            throw new IllegalStateException("Caught a SecurityException reading the system property '" + property + "'; the SystemUtils property value will default to null.");
        }
    }
}
