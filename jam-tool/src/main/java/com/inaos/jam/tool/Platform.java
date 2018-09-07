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

    public static final Platform CURRENT;

    public static final Platform
            WINDOWS_X64 = new Platform("dll", "", "win32-amd64"),
            WINDOWS_X86 = new Platform("dll", "", "win32-x86"),
            LINUX_X64 = new Platform("so", "lib", "linux-amd64"),
            LINUX_X86 = new Platform("so", "lib", "linux-x86");

    public final String extension;

    public final String prefix;

    public final String folder;

    public Platform(String extension, String prefix, String folder) {
        this.extension = extension;
        this.prefix = prefix;
        this.folder = folder;
    }

    static {
        String osArch = getSystemProperty("os.arch"), osName = getSystemProperty("os.name");
        String extension, prefix, folder;
        if ("amd64".equals(osArch)) {
            folder = "linux-amd64";
        } else if ("x86".equals(osArch)) {
            folder = "linux-i368";
        } else {
            throw new IllegalArgumentException("Operating System Architecture not supported: " + osArch);
        }
        if (isOsMatchesName(osName, "Linux") || isOsMatchesName(osName, "LINUX")) {
            extension = "so";
            prefix = "lib";
        } else if (isOsMatchesName(osName, "Windows")) {
            extension = "dll";
            prefix = "";
        } else {
            throw new IllegalArgumentException("Operating System not supported: " + osName);
        }
        CURRENT = new Platform(extension, prefix, folder);
    }

    public static Platform of(String name) {
        if (name.equalsIgnoreCase("WINDOWS_X64")) {
            return WINDOWS_X64;
        } else if (name.equalsIgnoreCase("WINDOWS_X64")) {
            return WINDOWS_X64;
        } else if (name.equalsIgnoreCase("WINDOWS_X86")) {
            return WINDOWS_X86;
        } else if (name.equalsIgnoreCase("LINUX_X64")) {
            return LINUX_X64;
        } else if (name.equalsIgnoreCase("LINUX_X86")) {
            return LINUX_X86;
        } else {
            throw new IllegalArgumentException("Unknown platform: " + name);
        }
    }

    private static boolean isOsMatchesName(String osName, String osNamePrefix) {
        return isOSNameMatch(osName, osNamePrefix);
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
        } catch (SecurityException e) { // We are not allowed to look at this property
            throw new IllegalStateException("Caught a SecurityException reading the system property '" + property + "'; the SystemUtils property value will default to null.");
        }
    }
}
