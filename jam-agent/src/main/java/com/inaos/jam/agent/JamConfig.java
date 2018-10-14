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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JamConfig {

    static final long MAX_OBSERVATION_COUNT_FOR_FILES = 10000;

    static final long MAX_OBSERVATION_BYTES_FOR_FILES = 1024 * 1024; // 1 MB

    final URL url;

    final boolean devMode, expectedName, debugMode, ignoreChecksum;

    final File sample;

    final Set<String> filtered;

    public JamConfig(URL url, boolean devMode, boolean expectedName, boolean debugMode, boolean ignoreChecksum, File sample, Set<String> filtered) {
        this.url = url;
        this.devMode = devMode;
        this.expectedName = expectedName;
        this.debugMode = debugMode;
        this.ignoreChecksum = ignoreChecksum;
        this.sample = sample;
        this.filtered = filtered;
    }

    JamConfig(List<String> arguments) throws MalformedURLException {
        boolean devMode = false, expectedName = false, debugMode = false, ignoreChecksum = false;
        URL url = null;
        File sample = null;
        filtered = new HashSet<String>();

        for (String config : arguments) {
            String[] pair = config.split("=");
            if (pair.length != 2) {
                throw new IllegalArgumentException("Expected argument to be of format key=value but was " + config);
            } else if (pair[0].equals("devMode")) {
                devMode = Boolean.parseBoolean(pair[1]);
            } else if (pair[0].equals("expectedName")) {
                expectedName = Boolean.parseBoolean(pair[1]);
            } else if (pair[0].equals("library")) {
                url = new URL(pair[1]);
            } else if (pair[0].equals("sample")) {
                sample = new File(pair[1]);
            } else if (pair[0].equals("debugMode")) {
                debugMode = Boolean.parseBoolean(pair[1]);
            } else if (pair[0].equals("filter")) {
                filtered.add(pair[1]);
            } else if (pair[0].equals("ignoreChecksum")) {
                ignoreChecksum = Boolean.parseBoolean(pair[1]);
            } else {
                throw new IllegalArgumentException("Unknown configuration: " + pair[0]);
            }
        }

        if (url == null) {
            throw new IllegalArgumentException("Agent library is not set");
        }

        this.devMode = devMode;
        this.expectedName = expectedName;
        this.debugMode = debugMode;
        this.ignoreChecksum = ignoreChecksum;
        this.url = url;
        this.sample = sample;
    }
}
