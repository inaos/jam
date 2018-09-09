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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JamDestructor extends Thread {

    private static final Set<Method> DESTRUCTORS = Collections.newSetFromMap(new ConcurrentHashMap<Method, Boolean>());

    static {
        Runtime.getRuntime().addShutdownHook(new JamDestructor());
    }

    public static void register(Class<?> dispatcher, String name) throws NoSuchMethodException {
        DESTRUCTORS.add(dispatcher.getDeclaredMethod(name));
    }

    @Override
    public void run() {
        try {
            for (Method method : DESTRUCTORS) {
                method.setAccessible(true);
                method.invoke(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
