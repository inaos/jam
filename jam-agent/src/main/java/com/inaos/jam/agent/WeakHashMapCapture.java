/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.agent;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import com.inaos.jam.boot.JamObjectCapture;

public class WeakHashMapCapture extends JamObjectCapture {

    private final WeakConcurrentMap<Object, Capture> map = new WeakConcurrentMap.WithInlinedExpunction<Object, Capture>();

    @Override
    protected void doCapture(Object key, String name, Object value) {
        map.put(key, new Capture(name, value));
    }

    @Override
    protected Object doRead(Object key, String name) {
        Capture capture = map.get(key);
        return capture == null || !capture.name.equals(name) ? null : capture.value;
    }

    static class Capture {

        final String name;

        final Object value;

        Capture(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}
