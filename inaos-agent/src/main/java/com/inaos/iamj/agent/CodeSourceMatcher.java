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

package com.inaos.iamj.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.List;

class CodeSourceMatcher implements AgentBuilder.RawMatcher {

    private final AgentBuilder.RawMatcher previous;

    private final List<String> expected;

    CodeSourceMatcher(AgentBuilder.RawMatcher previous, List<String> expected) {
        this.previous = previous;
        this.expected = expected;
    }

    @Override
    public boolean matches(TypeDescription typeDescription,
                           ClassLoader classLoader,
                           JavaModule module,
                           Class<?> classBeingRedefined,
                           ProtectionDomain protectionDomain) {
        if (!previous.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
            return false;
        }
        URL location = null;
        if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
            location = protectionDomain.getCodeSource().getLocation();
        }
        if (location != null) {
            File file;
            try {
                file = new File(location.toURI());
            } catch (URISyntaxException e) {
                file = new File(location.getPath());
            }
            String name = file.getName();
            for (String candidate : expected) {
                if (name.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
